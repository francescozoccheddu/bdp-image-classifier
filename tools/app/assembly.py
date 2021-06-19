from subprocess import CalledProcessError
from ..utils import cli, exceptions
from ..utils.launcher import main


class CompilationFailedError(exceptions.LoggableError):
    def __init__(self):
        super().__init__('Build failed')


def assembly(project_dir, output_file):
    from os import path
    import shutil
    interm_file = path.abspath(path.join(project_dir, '.intermediate.jar'))
    try:
        cli.run('sbt', ['--error', f'set assembly / assemblyOutputPath := file("{interm_file}")', 'assembly'], cwd=project_dir)
    except CalledProcessError:
        raise CompilationFailedError()
    shutil.move(interm_file, output_file)


@main
def _main():
    import argparse
    from os import path
    from ..utils import cliargs
    default_project_dir = path.relpath(path.join(path.dirname(__file__), '../../app/image-classifier'))
    parser = argparse.ArgumentParser(description='Assembly app JAR', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('-p', '--project-dir', type=cliargs.input_dir, default=default_project_dir, help='the SBT project directory')
    parser.add_argument('-o', '--output-file', type=cliargs.output_file, default='assembly.jar', help='the output JAR file')
    args = parser.parse_args()
    assembly(args.project_dir, args.output_file)
