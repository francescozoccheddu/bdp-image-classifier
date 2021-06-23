from ..utils.launcher import main
from . import env_utils, install
from ..utils import files, cli


def run(install_dir, assembly_file, config_file, driver_ram_mb=2048, executor_ram_mb=2048):
    env_utils.ensure_supported_platform()
    start_script = files.join(install_dir, env_utils.hadoop_dir(), 'sbin', 'start-dfs.sh')
    run_script = files.join(install_dir, env_utils.spark_dir(), 'bin', 'spark-submit')
    stop_script = files.join(install_dir, env_utils.hadoop_dir(), 'sbin', 'stop-dfs.sh')
    try:
        cli.log('Starting HDFS daemons...')
        if cli.run(start_script, [], enable_out=False, enable_err=False) != 0:
            raise RuntimeError('Failed to start HDFS daemons')
        cli.log('Running...')
        cli.run(run_script, ['--master', 'local[*]', '--driver-memory', f'{driver_ram_mb}G', '--executor-memory', f'{executor_ram_mb}G', assembly_file, config_file], enable_out=True, enable_err=True)
    finally:
        cli.log('Stopping HDFS daemons...')
        if cli.run(stop_script, [], enable_out=False, enable_err=False) != 0:
            raise RuntimeError('Failed to stop HDFS daemons')


@main
def _main():
    import argparse
    from ..utils import cli
    parser = argparse.ArgumentParser(description='Run the app on a debug environment', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    env_utils.add_argparse_install_dir(parser)
    cli.add_argparse_quiet(parser)
    parser.add_argument('assembly_file', metavar='ASSEMBLY_FILE', type=cli.input_file_arg, help='the app JAR file')
    parser.add_argument('config_file', metavar='CONFIG_FILE', type=cli.make_input_file_or_parent_arg('config.json'), help='the JSON configuration file')
    parser.add_argument('--driver-ram', type=cli.make_int_arg(1, 128), default=1, help='the driver RAM amount in GB')
    parser.add_argument('--executor-ram', type=cli.make_int_arg(1, 128), default=1, help='the executor RAM amount in GB')
    args = parser.parse_args()
    cli.set_exception_hook()
    cli.set_logging(not args.quiet)
    run(args.install_dir, args.assembly_file, args.config_file, args.driver_ram, args.executor_ram)
