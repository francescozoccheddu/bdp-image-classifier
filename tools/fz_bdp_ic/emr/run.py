from . import emr_utils
from ..utils.launcher import main


def run(script_file, output_dir, aws_ak_id, aws_ak_secret, mode=emr_utils.Mode.SSH, suppress_ssh_out=False):
    pass


@main
def _main():
    import argparse
    parser = argparse.ArgumentParser(description=f'Change output paths in a JSON configuration file', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('script_file', metavar='SCRIPT_FILE', help='the script file that outputs the configuration JSON string when executed on the cluster')
    emr_utils.add_argparse_args(parser)
    args = emr_utils.get_args(parser)
    run(args.script_file, args.output_dir, args.mode, args.suppress_ssh_output)
