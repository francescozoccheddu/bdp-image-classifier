
from .. import utils

from enum import Enum


class Architecture(Enum):
    x64 = 'x64'

    def __str__(self) -> str:
        return self.value


def download(architecture: Architecture, output_file: str):
    utils.download(f'https://github.com/francescozoccheddu/big-data-project/releases/download/v0.1-alpha/assembly-{architecture}.jar', output_file)


def _main():
    utils.hook_exceptions()
    import argparse
    parser = argparse.ArgumentParser(description='Download prebuilt app JARs', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('architecture', type=Architecture, choices=list(Architecture), help='the target architecture')
    parser.add_argument('-o', '--output-file', type=utils.output_file_arg, default='assembly.jar', help='the output JAR file')
    args = parser.parse_args()
    download(args.architecture, args.output_file)


if __name__ == '__main__':
    _main()
