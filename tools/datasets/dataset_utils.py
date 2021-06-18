from ..utils import exceptions, cli
from ..utils.launcher import dont_run
dont_run()


class KaggleAuthenticationError(exceptions.AuthenticationError):

    def __init__(self, cause):
        super().__init__('Kaggle authentication failed', cause)


def download_kaggle(dataset, output_dir):
    try:
        with cli.no_stdout():
            from kaggle.api.kaggle_api_extended import KaggleApi
            api = KaggleApi()
            api.authenticate()
    except ValueError as exc:
        raise KaggleAuthenticationError(exc)
    try:
        api.dataset_download_files(dataset, output_dir, True, False, True)
    except Exception as exc:
        raise exceptions.NetworkError('Dataset download failed', exc)


_config_template_name = '.{}-config.json.template'
_config_output_name = 'config.json'


def images_dir():
    return 'images'


def downloader(temp_files):

    temp_files += [_config_output_name]

    def decorator(func):
        from ..utils import launcher
        import os
        import inspect
        module = inspect.getmodule(func)
        script_file = module.__file__
        script_dir = os.path.dirname(script_file)
        dataset = os.path.basename(script_file).rstrip('.py')
        config_file = os.path.join(script_dir, _config_template_name.format(dataset))

        def download(dir):
            from ..utils import files
            import shutil
            with files.output_dir(dir, temp_files + [images_dir(), _config_output_name], True):
                func()
                shutil.copyfile(config_file, _config_output_name)
                for temp_file in temp_files:
                    files.delete(temp_file)

        if launcher.is_main_module(module):
            cli.set_exception_hook()
            from ..utils import cliargs
            import argparse
            parser = argparse.ArgumentParser(description=f'Download "{dataset}" dataset', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
            parser.add_argument('-o', '--output-dir', type=cliargs.output_dir, default='dataset', help='the output directory')
            args = parser.parse_args()
            download(args.output_dir)

        return download

    return decorator
