
from . import emr_utils
from ..utils.launcher import main
from ..datasets.download import Dataset


def run(
        dataset=Dataset.test,
        output_dir='result',
        aws_ak_id=None,
        aws_ak_secret=None,
        mode=emr_utils.Mode.SSH,
        instance_type=emr_utils.InstanceType.m4_large,
        instance_count=1,
        suppress_ssh_out=False):
    pass


@main
def _main():
    import argparse
    parser = argparse.ArgumentParser(description=f'Run the classifier on an AWS EMR cluster with a preconfigured dataset', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('dataset', metavar='DATASET', default=Dataset.test, choices=Dataset.choices(), help='the dataset to use')
    emr_utils.add_argparse_args(parser)
    args = emr_utils.get_args(parser)
    run(args.dataset, args.output_dir, args.aws_ak_id, args.aws_ak_secret, args.mode, args.instance_type, args.instance_count, args.suppress_ssh_output)
