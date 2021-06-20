

from ..utils.launcher import main
from ..utils import files
from ..utils.cli import Choice


class Architecture(Choice):
    x64 = 'x64'


def download(architecture: Architecture, output_file: str):
    files.download(f'https://github.com/francescozoccheddu/big-data-project/releases/download/latest/assembly-{architecture}.jar', output_file)


@main
def _main():
    from ..utils import cli
    import argparse
    parser = argparse.ArgumentParser(description='Download prebuilt app JARs', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('architecture', metavar='ARCHITECTURE', type=Architecture, choices=Architecture.choices(), help='the target architecture')
    parser.add_argument('-o', '--output-file', type=cli.output_file_arg, default='assembly.jar', help='the output JAR file')
    cli.add_argparse_quiet(parser)
    args = parser.parse_args()
    cli.set_exception_hook()
    cli.set_logging(not args.quiet)
    download(args.architecture, args.output_file)
