
from . import emr_utils
from ..utils.launcher import main
from enum import Enum


class Dataset(Enum):
    test = 'test'
    supermarket = 'supermarket'
    land = 'land'
    indoor = 'indoor'

    def __str__(self):
        return self.value


def run(dataset=Dataset.test, output_dir='result', aws_ak_id=None, aws_ak_secret=None, mode=emr_utils.Mode.SSH, suppress_ssh_out=False):
    pass


@main
def _main():
    import argparse
    parser = argparse.ArgumentParser(description=f'Change output paths in a JSON configuration file', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('dataset', metavar='DATASET', default=Dataset.test, choices=list(Dataset), help='the dataset to use')
    emr_utils.add_argparse_args(parser)
    args = emr_utils.get_args(parser)
    run(args.dataset, args.output_dir, args.mode, args.suppress_ssh_output)
