from contextlib import contextmanager
from botocore.exceptions import ClientError
from . import emr_utils
from ..utils.launcher import main
from ..utils.cli import log
from ..utils import files


def run_file(cg_script_file, output_dir, aws_region='us-east-1', aws_ak_id=None, aws_ak_secret=None, mode=emr_utils.Mode.SSH, suppress_ssh_out=False):
    run(files.read(cg_script_file), output_dir, aws_region, aws_ak_id, aws_ak_secret, mode, suppress_ssh_out)


def run(script, output_dir, aws_region='us-east-1', aws_ak_id=None, aws_ak_secret=None, mode=emr_utils.Mode.SSH, suppress_ssh_out=False):
    import boto3
    import os
    log(f'All AWS resources will be allocated in region "{aws_region}"')
    aws_ak_id = aws_ak_id or os.environ.get('AWS_ACCESS_KEY_ID')
    aws_ak_secret = aws_ak_secret or os.environ.get('AWS_SECRET_ACCESS_KEY')
    session = boto3.Session(aws_ak_id, aws_ak_secret, region_name=aws_region)
    if mode == emr_utils.Mode.SSH:
        _run_with_ssh(session, script, output_dir, suppress_ssh_out)
    elif mode == emr_utils.Mode.S3:
        _run_with_s3(session, script, output_dir)


@contextmanager
def _bucket(session, name):
    bucket = session.resource('s3').Bucket(name)
    try:
        log(f'Creating "{name}" S3 bucket')
        bucket.create(
            CreateBucketConfiguration={
                'LocationConstraint': session.region_name
            })
        yield bucket
    except BaseException:
        try:
            log(f'Deleting "{name}" S3 bucket')
            bucket.objects.all.delete()
            bucket.delete()
        except ClientError as exc:
            if exc['error']['code'] != 'NoSuchBucket':
                raise
        raise


def _run_with_s3(session, cg_script, output_dir):
    uid = session.client('sts').get_caller_identity().get('Account')
    bucket_name = f'bdp-image-classifier-{uid}'
    cg_script_key='cg-script'
    job_script_key='job-script'
    job_script = files.template('job.sh')
    results_emr_file = 'results.tgz'
    with _bucket(bucket_name) as bucket:
        bucket.put_object(Body=cg_script.encode(), Key=cg_script_key)
        bucket.put_object(Body=job_script.encode(), Key=job_script_key)


def _run_with_ssh(session, script_file, output_dir, suppress_output=False):
    pass


@main
def _main():
    import argparse
    parser = argparse.ArgumentParser(description=f'Change output paths in a JSON configuration file', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('script_file', metavar='SCRIPT_FILE', help='the script file that outputs the configuration JSON string when executed on the cluster')
    emr_utils.add_argparse_args(parser)
    args = emr_utils.get_args(parser)
    run(args.script_file, args.output_dir, args.mode, args.suppress_ssh_output)
