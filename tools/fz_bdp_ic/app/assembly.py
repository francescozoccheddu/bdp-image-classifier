from os import path
from ..utils.launcher import main
from ..utils import files


def assembly(project_dir, output_file):
    from ..utils.cli import run
    interm_file = files.abs(files.join(project_dir, '.intermediate.jar'))
    if run('sbt', ['--error', f'set assembly / assemblyOutputPath := file("{interm_file}")', 'assembly'], cwd=project_dir) != 0:
        raise RuntimeError('Compilation failed')
    files.move(interm_file, output_file)


@main
def _main():
    import argparse
    from ..utils import cli
    inferred_project_dir = files.rel(files.join(files.parent(__file__), '../../app/image-classifier'))
    project_dir_nargs=None
    project_dir_default=None
    if files.is_dir(inferred_project_dir) and files.is_file(files.join(inferred_project_dir, 'build.sbt')):
        project_dir_default = inferred_project_dir
        project_dir_nargs = '?'
    parser = argparse.ArgumentParser(description='Assembly app JAR', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('project_dir', metavar='PROJECT_DIR', type=cli.input_dir_arg, default=project_dir_default, nargs=project_dir_nargs, help='the SBT project directory')
    parser.add_argument('-o', '--output-file', type=cli.output_file_arg, default='assembly.jar', help='the output JAR file')
    cli.add_argparse_quiet(parser)
    args = parser.parse_args()
    cli.set_exception_hook()
    cli.set_logging(not args.quiet)
    assembly(args.project_dir, args.output_file)
