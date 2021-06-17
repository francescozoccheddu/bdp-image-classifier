from .. import utils


class NoSBTError(utils.LoggableError):
    def __init__(self):
        super().__init__('SBT is not installed. See "https://www.scala-sbt.org/download.html" for help.')


class CompilationFailedError(utils.LoggableError):
    def __init__(self):
        super().__init__('Build failed.')


def assembly(project_dir, output_file):
    import shutil
    sbt_file = shutil.which('sbt')
    if not sbt_file:
        raise NoSBTError()
    import subprocess
    from os import path
    interm_file = path.realpath(path.join(project_dir, '.intermediate.jar'))
    if subprocess.call([sbt_file, '--error', f'set assembly / assemblyOutputPath := file("{interm_file}")', 'assembly'], cwd=project_dir) != 0:
        raise CompilationFailedError()
    shutil.move(interm_file, output_file)


def _main():
    utils.hook_exceptions()
    import argparse
    from os import path
    default_project_dir = path.relpath(path.join(path.dirname(__file__), '../../app/image-classifier'))
    parser = argparse.ArgumentParser(description='Assembly app JAR', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('-p', '--project-dir', type=utils.input_dir_arg, default=default_project_dir, help='the SBT project directory')
    parser.add_argument('-o', '--output-file', type=utils.output_file_arg, default='assembly.jar', help='the output JAR file')
    args = parser.parse_args()
    assembly(args.project_dir, args.output_file)


if __name__ == '__main__':
    _main()
