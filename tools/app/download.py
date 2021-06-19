

from ..utils.launcher import main
from ..utils import files
from enum import Enum


class Architecture(Enum):
    x64 = 'x64'

    def __str__(self) -> str:
        return self.value


def download(architecture: Architecture, output_file: str):
    files.download(f'https://github.com/francescozoccheddu/big-data-project/releases/download/v0.1-alpha/assembly-{architecture}.jar', output_file)


@main
def _main():
    from ..utils import cli
    import argparse
    parser = argparse.ArgumentParser(description='Download prebuilt app JARs', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('architecture', metavar='ARCHITECTURE', type=Architecture, choices=list(Architecture), help='the target architecture')
    parser.add_argument('-o', '--output-file', type=cli.output_file_arg, default='assembly.jar', help='the output JAR file')
    cli.add_argparse_quiet(parser)
    args = parser.parse_args()
    cli.set_exception_hook()
    cli.set_logging(not args.quiet)
    download(args.architecture, args.output_file)
