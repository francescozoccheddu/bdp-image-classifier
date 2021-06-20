from ..utils import cli, files
from ..utils.launcher import dont_run
import inspect
dont_run()


def download_kaggle(dataset, output_dir):
    with cli.no_stdout():
        from kaggle.api.kaggle_api_extended import KaggleApi
        api = KaggleApi()
        api.authenticate()
    api.dataset_download_files(dataset, output_dir, True, not cli.is_logging(), True)


_config_template_name = '{}-config.json'
_config_output_name = 'config.json'


def images_dir():
    return 'images'


def _get_name(module):
    return files.name(module.__file__, False)


def _get_config_file(module):
    return files.resource(module.__name__, _config_template_name.format(_get_name(module)))


def downloader(temp_files):

    def decorator(func):
        module = inspect.getmodule(func)
        config_file = _get_config_file(module)

        def download(dir):
            with files.output_dir(dir, temp_files + [images_dir(), _config_output_name], True):
                func()
                files.copy(config_file, _config_output_name)
                for temp_file in temp_files:
                    files.delete(temp_file)

        return download

    return decorator


def main(func):
    module = inspect.getmodule(func)
    dataset = _get_name(module)
    from ..utils import launcher

    def run():
        import argparse
        parser = argparse.ArgumentParser(description=f'Download "{dataset}" dataset', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
        parser.add_argument('-o', '--output-dir', type=cli.output_dir_arg, default='dataset', help='the output directory')
        cli.add_argparse_quiet(parser)
        args = parser.parse_args()
        cli.set_exception_hook()
        cli.set_logging(not args.quiet)
        func(args.output_dir)

    if launcher.is_main_module(module):
        run()

    return run
