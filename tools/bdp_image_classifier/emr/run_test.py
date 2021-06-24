
from . import emr_utils
from ..utils.launcher import main
from ..datasets import download
from ..utils import files


def run(
        dataset,
        output_dir='result',
        aws_region='us-east-1',
        aws_ak_id=None,
        aws_ak_secret=None,
        mode=emr_utils.Mode.SSH,
        instance_type=emr_utils.InstanceType.m4_large,
        instance_count=1,
        suppress_ssh_out=False,
        wait_for_s3_error_logs=False):
    cg_script = files.template('test-config-gen.sh', vars={
        "%DATASET%": dataset
    })
    from .run import run
    run(cg_script, output_dir, aws_region, aws_ak_id, aws_ak_secret, mode, instance_type, instance_count, suppress_ssh_out, wait_for_s3_error_logs)


@main
def _main():
    import argparse
    parser = argparse.ArgumentParser(description=f'Run the classifier on an AWS EMR cluster with a preconfigured dataset', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('dataset', metavar='DATASET', choices=download.dataset_names(), help='the dataset to use')
    emr_utils.add_argparse_args(parser)
    args = emr_utils.get_args(parser)
    run(args.dataset, args.output_dir, args.aws_region, args.aws_ak_id, args.aws_ak_secret, args.mode, args.instance_type, args.instance_count, args.suppress_ssh_output, args.wait_s3_error_logs)
