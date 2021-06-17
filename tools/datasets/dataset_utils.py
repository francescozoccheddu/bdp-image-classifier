
from genericpath import exists
from .. import utils
utils.dont_run()


class KaggleAuthenticationError(utils.LoggableError):

    def __init__(self, error):
        super().__init__('Kaggle authentication failed', error)


def download(url, output_dir, format=None):
    file = utils.download(url)
    utils.extract(file, output_dir, format)


def download_kaggle(dataset, output_dir):
    from kaggle.api.kaggle_api_extended import KaggleApi
    api = KaggleApi()
    try:
        with utils.suppress_stdout():
            api.authenticate()
    except ValueError as e:
        raise KaggleAuthenticationError(e)
    api.dataset_download_files(dataset, output_dir, True, False, True)


_config_template_name = '.{}-config.json.template'
_config_output_name = 'config.json'


def _on_dir(dir, config_file, func):
    import os
    import shutil
    existed = os.path.exists(dir)
    if not existed:
        os.mkdir(dir)
    try:
        func(dir)
        shutil.copyfile(config_file, os.path.join(dir, _config_output_name))
    except BaseException:
        try:
            if existed:
                os.rmdir(dir)
            else:
                shutil.rmtree(dir)
        except BaseException:
            pass
        raise


def on_dir(dir, func):
    from os import path
    caller_file = utils.get_caller_module().__file__
    caller_dir = path.dirname(caller_file)
    name = path.basename(caller_file).rstrip('.py')
    config_file = path.join(caller_dir, _config_template_name.format(name))
    _on_dir(dir, config_file, func)


def catch_main(func):
    if utils.is_caller_main():
        utils.hook_exceptions()
        import argparse
        from os import path
        caller_file = utils.get_caller_module().__file__
        caller_dir = path.dirname(caller_file)
        name = path.basename(caller_file).rstrip('.py')
        config_file = path.join(caller_dir, _config_template_name.format(name))
        parser = argparse.ArgumentParser(description=f'Download "{name}" dataset', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
        parser.add_argument('-o', '--output-dir', type=utils.output_dir_arg, default='dataset', help='the output directory')
        args = parser.parse_args()
        _on_dir(args.output_dir, config_file, func)
