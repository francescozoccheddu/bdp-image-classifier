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
    from ..utils import cliargs
    default_project_dir = files.rel(files.join(files.parent(__file__), '../../app/image-classifier'))
    parser = argparse.ArgumentParser(description='Assembly app JAR', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('-p', '--project-dir', type=cliargs.input_dir, default=default_project_dir, help='the SBT project directory')
    parser.add_argument('-o', '--output-file', type=cliargs.output_file, default='assembly.jar', help='the output JAR file')
    args = parser.parse_args()
    assembly(args.project_dir, args.output_file)
