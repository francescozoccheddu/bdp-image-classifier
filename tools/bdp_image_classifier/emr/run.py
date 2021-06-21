from contextlib import contextmanager
from tools.bdp_image_classifier.debug_env.env_utils import ssh_key_dir
from typing import final
import boto3
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
        suppress_ssh_out=False):
    run(files.read(cg_script_file), output_dir, aws_region, aws_ak_id, aws_ak_secret, mode, instance_type, instance_count, suppress_ssh_out)


def run(
        cg_script,
        output_dir,
        aws_region='us-east-1',
        aws_ak_id=None,
        aws_ak_secret=None,
        mode=emr_utils.Mode.SSH,
        instance_type=emr_utils.InstanceType.m4_large,
        instance_count=1,
        suppress_ssh_out=False):
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
        _run_with_s3(session, cg_script, instance_type, instance_count, output_dir)


_cluster_name = f'{_prefix}-cluster'


def ensure_no_cluster(session):
    emr = session.client('emr')
    response = emr.list_clusters(ClusterStates=['STARTING', 'BOOTSTRAPPING', 'RUNNING', 'WAITING'])
    if any([c['Name'] == _cluster_name for c in response['Clusters']]):
        raise RuntimeError(f'A cluster with name "{_cluster_name}" is already running')


def _delete_bucket(bucket):
    try:
        log(f'Deleting "{bucket.name}" S3 bucket...')
        bucket.objects.all.delete()
        bucket.delete()
    except ClientError as exc:
        if exc['error']['code'] != 'NoSuchBucket':
            raise


@contextmanager
def _bucket(session):
    uid = session.client('sts').get_caller_identity().get('Account')
    name = f'{_prefix}-{uid}-bucket'
    s3 = session.resource('s3')
    bucket = s3.Bucket(name)
    _delete_bucket(bucket)
    try:
        log(f'Creating "{name}" S3 bucket...')
        bucket.create(
            CreateBucketConfiguration={
                'LocationConstraint': session.region_name
            }).wait_until_exists()
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


@ contextmanager
def _key_pair(session, name):
    ec2 = session.client('ec2')
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


@ contextmanager
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


@ contextmanager
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


def _delete_security_group(group, max_attempts=5, delay=10):
    log(f'Deleting "{group.group_name}" EC2 security group...')
    try:
        if group.ip_permissions:
            group.revoke_ingress(IpPermissions=group.ip_permissions)
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
    except Exception as exc:
        if exc.response['Error']['Code'] != 'NoSuchEntity':
            raise


@ contextmanager
def _security_group(session, vpc, name, enable_ssh):
    try:
        group = list(vpc.security_groups.filter(GroupNames=[name]))[0]
        _delete_security_group(group)
    except ClientError as exc:
        if exc.response['Error']['Code'] != 'InvalidGroup.NotFound':
            raise
    ec2_client = session.client('ec2')
    group = None
    try:
        log(f'Creating "{name}" EC2 security group...')
        group = vpc.create_security_group(GroupName=name, Description='Security group for the bdp-image-classifier EMR cluster')
        ec2_client.get_waiter('security_group_exists').wait(GroupIds=[group.id])
        if enable_ssh:
            import requests
            my_ip = requests.get('http://checkip.amazonaws.com').text.rstrip()
            log(f'Authorizing SSH traffic from {my_ip} in EC2 security group "{name}"...')
            group.authorize_ingress(FromPort=22, IpProtocol='tcp', ToPort=22, IpRanges=[{'CidrIp': f'{my_ip}/32'}])
        yield group
    finally:
        if group is not None:
            _delete_security_group(group)


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


@ contextmanager
def _emr_security_groups(session, enable_ssh):
    vpc = _default_vpc(session)
    with _security_group(session, vpc, f'{_prefix}-master-security-group', enable_ssh) as master_sg:
        with _security_group(session, vpc, f'{_prefix}-slave-security-group', False) as slave_sg:
            yield master_sg, slave_sg


@ contextmanager
def _cluster(session, instance_type, instance_count, steps=[], key=None):
    with _emr_roles(session) as (job_flow_role, service_role):
        with _emr_security_groups(session, key is not None) as (master_sg, slave_sg):
            delay = 5
            if delay > 0:
                import time
                log(f'Waiting {delay} seconds for the changes to propagate...')
                time.sleep(delay)
            log(f'Creating "{_cluster_name}" EMR cluster...')
            import json
            client = session.client('emr')
            id = None
            try:
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
                    VisibleToAllUsers=True
                )['JobFlowId']
                log('Waiting for the EMR cluster to be running. Please wait or type CTRL+C to abort (it usually takes about 10 minutes)...')
                client.get_waiter('cluster_running').wait(ClusterId=id)
                log('The EMR cluster is running.')
                yield id
            finally:
                log(f'Terminating "{_cluster_name}" EMR cluster...')
                if id is not None:
                    client.terminate_job_flows(JobFlowIds=[id])


def _wait_cluster_terminated(session, cluster_id):
    log('Waiting for the EMR cluster to terminate. Please wait or type CTRL+C to abort...')
    session.client('emr').get_waiter('cluster_terminated').wait(ClusterId=cluster_id)
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


def _run_with_s3(session, cg_script, output_dir, instance_type, instance_count):
    cg_script_s3_key = 'cg-script'
    job_script_s3_key = 'job-script'
    results_emr_file = '~/results.tgz'
    cg_script_emr_file = '~/cg-script'
    job_script = files.template('job.sh', vars={
        '%CG_SCRIPT_FILE%': cg_script_emr_file,
        '%RESULTS_FILE%': results_emr_file
    })
    results_s3_key = 'results.tgz'
    results_file = None
    try:
        with _bucket() as bucket:
            steps = [
                _step(session, 'Download config generator script from S3', f'aws s3 cp "s3://{bucket.name}/{cg_script_s3_key}" "{cg_script_emr_file}"', True),
                _step(session, 'Run job', f'"s3://{bucket.name}/{job_script_s3_key}"', True),
                _step(session, 'Upload results to S3', f'aws s3 cp "{results_emr_file}" "s3://{bucket.name}/{results_s3_key}"', False)
            ]
            log('Uploading scripts to S3 bucket...')
            bucket.put_object(Body=cg_script.encode(), Key=cg_script_s3_key)
            bucket.put_object(Body=job_script.encode(), Key=job_script_s3_key)
            with _cluster(session, instance_type, instance_count, steps) as cluster_id:
                _wait_cluster_terminated(session, cluster_id)
            results_file = files.temp_path()
            log('Downloading results from S3 bucket...')
            bucket.download_file(results_s3_key, results_file)
            files.extract(results_file, output_dir, 'gztar', True)
    finally:
        if results_file is not None:
            files.delete(results_file)


def _run_with_ssh(session, script_file, output_dir, instance_type, instance_count, suppress_output=False):
    pass


@main
def _main():
    import argparse
    parser = argparse.ArgumentParser(description=f'Change output paths in a JSON configuration file', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('script_file', metavar='SCRIPT_FILE', help='the script file that outputs the configuration JSON string when executed on the cluster')
    emr_utils.add_argparse_args(parser)
    args = emr_utils.get_args(parser)
    run(args.script_file, args.output_dir, args.mode, args.suppress_ssh_output)
