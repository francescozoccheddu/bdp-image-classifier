from contextlib import contextmanager
from botocore.exceptions import ClientError
from . import emr_utils
from ..utils.launcher import main
from ..utils.cli import log
from ..utils import files

_prefix = 'bdp-image-classifier'


def run_file(
        cg_script_file,
        output_dir,
        aws_region='us-east-1',
        aws_ak_id=None,
        aws_ak_secret=None,
        mode=emr_utils.Mode.SSH,
        instance_type=emr_utils.InstanceType.m4_large,
        instance_count=1,
        suppress_ssh_out=False,
        wait_for_s3_error_logs=False):
    run(files.read(cg_script_file), output_dir, aws_region, aws_ak_id, aws_ak_secret, mode, instance_type, instance_count, suppress_ssh_out, wait_for_s3_error_logs)


def run(
        cg_script,
        output_dir,
        aws_region='us-east-1',
        aws_ak_id=None,
        aws_ak_secret=None,
        mode=emr_utils.Mode.SSH,
        instance_type=emr_utils.InstanceType.m4_large,
        instance_count=1,
        suppress_ssh_out=False,
        wait_for_s3_error_logs=False):
    import boto3
    import os
    log(f'All AWS resources will be allocated in region "{aws_region}".')
    aws_ak_id = aws_ak_id or os.environ['AWS_ACCESS_KEY_ID']
    aws_ak_secret = aws_ak_secret or os.environ['AWS_SECRET_ACCESS_KEY']
    session = boto3.Session(aws_ak_id, aws_ak_secret, region_name=aws_region)
    ensure_no_cluster(session)
    if mode == emr_utils.Mode.SSH:
        _run_with_ssh(session, cg_script, output_dir, instance_type, instance_count, suppress_ssh_out)
    elif mode == emr_utils.Mode.S3:
        _run_with_s3(session, cg_script, output_dir, instance_type, instance_count, wait_for_s3_error_logs)


_cluster_name = f'{_prefix}-cluster'


def ensure_no_cluster(session):
    emr = session.client('emr')
    response = emr.list_clusters(ClusterStates=['STARTING', 'BOOTSTRAPPING', 'RUNNING', 'WAITING'])
    if any([c['Name'] == _cluster_name for c in response['Clusters']]):
        raise RuntimeError(f'A cluster with name "{_cluster_name}" is already running')


def _delete_bucket(bucket):
    try:
        log(f'Deleting "{bucket.name}" S3 bucket...')
        bucket.objects.all().delete()
        bucket.delete()
    except ClientError as exc:
        if exc.response['Error']['Code'] != 'NoSuchBucket':
            raise


@contextmanager
def _bucket(session):
    uid = session.client('sts').get_caller_identity().get('Account')
    name = f'{_prefix}-{uid}-{session.region_name}-s3b'
    s3 = session.resource('s3')
    bucket = s3.Bucket(name)
    _delete_bucket(bucket)
    try:
        log(f'Creating "{name}" S3 bucket...')
        if session.region_name != 'us-east-1':
            bucket.create(CreateBucketConfiguration={'LocationConstraint': session.region_name})
        else:
            bucket.create()
        bucket.wait_until_exists()
        s3.meta.client.put_public_access_block(
            Bucket=name,
            PublicAccessBlockConfiguration={
                'BlockPublicAcls': True,
                'IgnorePublicAcls': True,
                'BlockPublicPolicy': True,
                'RestrictPublicBuckets': True
            })
        yield bucket
    finally:
        _delete_bucket(bucket)


def _delete_key_pair(key):
    log(f'Deleting "{key.name}" EC2 key pair...')
    try:
        key.delete()
    except Exception as exc:
        if exc.response['Error']['Code'] != 'NoSuchEntity':
            raise


@contextmanager
def _key_pair(session):
    ec2 = session.resource('ec2')
    name = f'{_prefix}-key-pair'
    _delete_key_pair(ec2.KeyPair(name))
    key = None
    try:
        key = ec2.create_key_pair(KeyName=name)
        yield key
    finally:
        if key is not None:
            _delete_key_pair(key)


def _delete_role(role):
    log(f'Deleting "{role.name}" IAM role...')
    try:
        for policy in role.attached_policies.all():
            role.detach_policy(PolicyArn=policy.arn)
        for inst_profile in role.instance_profiles.all():
            inst_profile.remove_role(RoleName=role.name)
            inst_profile.delete()
        role.delete()
    except Exception as exc:
        if exc.response['Error']['Code'] != 'NoSuchEntity':
            raise


@contextmanager
def _role(session, name, policy_doc, policy_arn, create_instance_profile):
    iam = session.resource('iam')
    role = None
    _delete_role(iam.Role(name))
    try:
        log(f'Creating "{name}" IAM role...')
        role = iam.create_role(
            RoleName=name,
            AssumeRolePolicyDocument=policy_doc
        )
        iam.meta.client.get_waiter('role_exists').wait(RoleName=name)
        role.attach_policy(PolicyArn=policy_arn)
        if create_instance_profile:
            instance_profile = iam.create_instance_profile(InstanceProfileName=name)
            instance_profile.add_role(RoleName=name)
            iam.meta.client.get_waiter('instance_profile_exists').wait(InstanceProfileName=name)
        yield role
    finally:
        if role is not None:
            _delete_role(role)


@contextmanager
def _emr_roles(session):
    job_flow_role_name = f'{_prefix}-ec2-role'
    job_flow_policy_arn = 'arn:aws:iam::aws:policy/service-role/AmazonElasticMapReduceforEC2Role'
    job_flow_policy_doc = files.template('emr-ec2-policy.json')
    service_role_name = f'{_prefix}-service-role'
    service_policy_arn = 'arn:aws:iam::aws:policy/service-role/AmazonElasticMapReduceRole'
    service_policy_doc = files.template('emr-service-policy.json')
    with _role(session, job_flow_role_name, job_flow_policy_doc, job_flow_policy_arn, True) as job_flow_role:
        with _role(session, service_role_name, service_policy_doc, service_policy_arn, False) as service_role:
            yield job_flow_role, service_role


def _delete_security_groups(groups, max_attempts=5, delay=10):
    for i, group in enumerate(groups):
        try:
            if group.ip_permissions:
                group.revoke_ingress(IpPermissions=group.ip_permissions)
        except Exception as exc:
            if exc.response['Error']['Code'] != 'NoSuchEntity':
                raise
            else:
                groups[i] = None
    groups = [g for g in groups if g is not None]
    for group in groups:
        log(f'Deleting "{group.group_name}" EC2 security group...')
        while True:
            try:
                group.delete()
                break
            except ClientError as exc:
                if exc.response['Error']['Code'] == 'DependencyViolation' and max_attempts > 0:
                    log(f'EC2 security group "{group.group_name}" is in use. Retrying in {delay} seconds {max_attempts} more times. Please wait...')
                    import time
                    time.sleep(delay)
                    max_attempts -= 1
                else:
                    raise


def _get_security_group_by_name(vpc, name):
    try:
        return list(vpc.security_groups.filter(GroupNames=[name]))[0]
    except ClientError as exc:
        if exc.response['Error']['Code'] != 'InvalidGroup.NotFound':
            raise
        else:
            return None


def _create_security_group(session, vpc, name, enable_ssh):
    ec2_client = session.client('ec2')
    log(f'Creating "{name}" EC2 security group...')
    group = vpc.create_security_group(GroupName=name, Description='Security group for the bdp-image-classifier EMR cluster')
    ec2_client.get_waiter('security_group_exists').wait(GroupIds=[group.id])
    if enable_ssh:
        import requests
        my_ip = requests.get('http://checkip.amazonaws.com').text.rstrip()
        log(f'Authorizing SSH traffic from {my_ip} in EC2 security group "{name}"...')
        group.authorize_ingress(FromPort=22, IpProtocol='tcp', ToPort=22, CidrIp=f'{my_ip}/32')
    return group


def _default_vpc(session):
    log('Retrieving default EC2 VPC...')
    ec2 = session.resource('ec2')
    try:
        ec2.meta.client.create_default_vpc()
    except ClientError as exc:
        if exc.response['Error']['Code'] != 'DefaultVpcAlreadyExists':
            raise
    default_filter = [{'Name': 'isDefault', 'Values': ['true']}]
    ec2.meta.client.get_waiter('vpc_available').wait(Filters=default_filter)
    vpc = list(ec2.vpcs.filter(Filters=default_filter))[0]
    return vpc


@contextmanager
def _emr_security_groups(session, enable_ssh):
    vpc = _default_vpc(session)
    default_sg = _get_security_group_by_name(vpc, 'default')
    master_name = f'{_prefix}-master-security-group'
    slave_name = f'{_prefix}-slave-security-group'
    groups = [g for g in [_get_security_group_by_name(vpc, n) for n in [master_name, slave_name]] if g is not None]
    _delete_security_groups(groups, 3)
    groups = []
    try:
        groups += [_create_security_group(session, vpc, master_name, enable_ssh)]
        groups += [_create_security_group(session, vpc, slave_name, False)]
        yield (*groups, default_sg)
    finally:
        _delete_security_groups(groups, 12)


def _list_instances(session, cluster_id):
    emr = session.client('emr')
    instances = emr.list_instances(ClusterId=cluster_id, InstanceGroupTypes=['MASTER'])['Instances']
    instances += emr.list_instances(ClusterId=cluster_id, InstanceGroupTypes=['CORE', 'TASK'])['Instances']
    return instances


@contextmanager
def _cluster(session, instance_type, instance_count, steps=[], key=None, log_uri=None):
    with _emr_roles(session) as (job_flow_role, service_role):
        with _emr_security_groups(session, key is not None) as (master_sg, slave_sg, default_sg):
            log(f'Creating "{_cluster_name}" EMR cluster...')
            import json
            import time
            client = session.client('emr')
            id = None
            instances = {
                'MasterInstanceType': instance_type.value,
                'SlaveInstanceType': instance_type.value,
                'InstanceCount': instance_count + 1,
                'KeepJobFlowAliveWhenNoSteps': not steps,
                'EmrManagedMasterSecurityGroup': master_sg.id,
                'EmrManagedSlaveSecurityGroup': slave_sg.id,
            }
            if key:
                instances['Ec2KeyName'] = key
            addit_args = {}
            if log_uri:
                addit_args['LogUri'] = log_uri
            max_attempts = 6
            delay = 10
            log(f'Waiting {delay} seconds for the EC2 profile instance to propagate...')
            time.sleep(delay)
            try:
                while id is None:
                    try:
                        id = client.run_job_flow(
                            Name=_cluster_name,
                            ReleaseLabel='emr-6.3.0',
                            Instances=instances,
                            Steps=steps,
                            Applications=[{
                                'Name': 'Spark'
                            }],
                            Configurations=json.loads(files.template('emr-configuration.json')),
                            JobFlowRole=job_flow_role.name,
                            ServiceRole=service_role.name,
                            EbsRootVolumeSize=10,
                            VisibleToAllUsers=True,
                            **addit_args
                        )['JobFlowId']
                        break
                    except ClientError as exc:
                        err = exc.response['Error']
                        if err['Code'] == 'ValidationException' and 'invalid instanceprofile' in err['Message'].lower() and max_attempts > 0:
                            log(f'The EC2 instance profile is not ready yet. Retrying in {delay} seconds {max_attempts} more times. Please wait...')
                            time.sleep(delay)
                            max_attempts -= 1
                        else:
                            raise
                log(f'EMR cluster successfully created with ID "{id}".')
                log('Waiting for the EMR cluster to be running. Please wait or type CTRL+C to abort (it usually takes about 10 minutes)...')
                client.get_waiter('cluster_running').wait(ClusterId=id)
                log('The EMR cluster is running.')
                yield id
            finally:
                if id is not None:
                    log(f'Terminating "{_cluster_name}" EMR cluster...')
                    try:
                        ec2 = session.resource('ec2')
                        instances = _list_instances(session, id)
                        for instance in instances:
                            instance = ec2.Instance(instance['Ec2InstanceId'])
                            instance.modify_attribute(Groups=[default_sg.id])
                    except Exception as exc:
                        pass
                    client.terminate_job_flows(JobFlowIds=[id])


def _wait_cluster_terminated(session, cluster_id):
    log('Waiting for the EMR cluster to terminate. Please wait or type CTRL+C to abort...')
    session.client('emr').get_waiter('cluster_terminated').wait(ClusterId=cluster_id, WaiterConfig={'Delay': 30, 'MaxAttempts': 480})
    log('The EMR cluster has terminated.')


def _step(session, name, command, local=False):
    import shlex
    jar = 'command-runner.jar' if local else f's3://{session.region_name}.elasticmapreduce/libs/script-runner/script-runner.jar'
    return {
        'Name': name,
        'ActionOnFailure': 'TERMINATE_CLUSTER',
        'HadoopJarStep': {
            'Jar': jar,
            'Args': shlex.split(command)
        }
    }


_emr_user = 'hadoop'


def _run_with_s3(session, cg_script, output_dir, instance_type, instance_count, wait_for_logs):
    cg_script_s3_key = 'cg-script'
    job_script_s3_key = 'job-script'
    results_emr_file = f'/home/{_emr_user}/results.tgz'
    cg_script_emr_file = f'/home/{_emr_user}/cg-script'
    job_script = files.template('job.sh', vars={
        '%CG_SCRIPT_FILE%': cg_script_emr_file,
        '%RESULTS_FILE%': results_emr_file
    })
    results_s3_key = 'results.tgz'
    logs_s3_key = 'logs'
    results_file = None
    try:
        with _bucket(session) as bucket:
            steps = [
                _step(session, 'Download config generator script from S3', f'aws s3 cp "s3://{bucket.name}/{cg_script_s3_key}" "{cg_script_emr_file}"', True),
                _step(session, 'Run job', f'"s3://{bucket.name}/{job_script_s3_key}"', False),
                _step(session, 'Upload results to S3', f'aws s3 cp "{results_emr_file}" "s3://{bucket.name}/{results_s3_key}"', True)
            ]
            log('Uploading scripts to S3 bucket...')
            bucket.put_object(Body=cg_script.encode(), Key=cg_script_s3_key)
            bucket.put_object(Body=job_script.encode(), Key=job_script_s3_key)
            try:
                log_uri = f's3://{bucket.name}/{logs_s3_key}'
                with _cluster(session, instance_type, instance_count, steps, log_uri=log_uri) as cluster_id:
                    _wait_cluster_terminated(session, cluster_id)
            except Exception:
                if wait_for_logs:
                    from ..utils import cli
                    cli.err(f'Job failed. You may want to check the logs at "{log_uri}" before continuing, as the bucket will be deleted.')
                    cli.pause(False)
                raise
            results_file = files.temp_path()
            log('Downloading results from S3 bucket...')
            bucket.download_file(results_s3_key, results_file)
            files.extract(results_file, output_dir, 'gztar', True)
    finally:
        if results_file is not None:
            files.delete(results_file)


def _ssh_command(client, command, suppress_output):
    _, stdout, stderr = client.exec_command(command)
    while True:
        line = stdout.readline()
        if not line:
            break
        if not suppress_output:
            print(f'[EMR] {line}', end="")
    if stdout.channel.recv_exit_status() != 0:
        raise RuntimeError('Command failed', stderr.read().decode())


def _run_with_ssh(session, cg_script, output_dir, instance_type, instance_count, suppress_output=False):
    import paramiko
    from scp import SCPClient
    import io
    results_emr_file = f'/home/{_emr_user}/results.tgz'
    cg_script_emr_file = f'/home/{_emr_user}/cg-script'
    job_script_emr_file = f'/home/{_emr_user}/job-script'
    job_script = files.template('job.sh', vars={
        '%CG_SCRIPT_FILE%': cg_script_emr_file,
        '%RESULTS_FILE%': results_emr_file
    })
    results_file = None
    try:
        with _key_pair(session) as key_pair:
            with _cluster(session, instance_type, instance_count, key=key_pair.name) as cluster_id:
                master_ip = _list_instances(session, cluster_id)[0]['PublicDnsName']
                log('Establishing SSH connection to EMR cluster...')
                with paramiko.SSHClient() as ssh:
                    ssh_key = paramiko.RSAKey.from_private_key(io.StringIO(key_pair.key_material))
                    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
                    ssh.connect(hostname=master_ip, username=_emr_user, pkey=ssh_key)
                    with SCPClient(ssh.get_transport()) as scp:
                        log('Uploading scripts to EMR cluster...')
                        scp.putfo(io.StringIO(job_script), job_script_emr_file, mode='0777')
                        scp.putfo(io.StringIO(cg_script), cg_script_emr_file, mode='0777')
                        log('Running job on EMR cluster...')
                        _ssh_command(ssh, job_script_emr_file, suppress_output)
                        log('Collecting results from EMR cluster...')
                        results_file = files.temp_path()
                        scp.get(results_emr_file, results_file)
                files.extract(results_file, output_dir, 'gztar', True)
    finally:
        if results_file is not None:
            files.delete(results_file)


@main
def _main():
    import argparse
    parser = argparse.ArgumentParser(description=f'Change output paths in a JSON configuration file', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('script_file', metavar='SCRIPT_FILE', help='the script file that outputs the configuration JSON string when executed on the cluster')
    emr_utils.add_argparse_args(parser)
    args = emr_utils.get_args(parser)
    run(args.script_file, args.output_dir, args.mode, args.suppress_ssh_output, args.wait_s3_error_logs)
