
from genericpath import exists
import tempfile
from .. import utils
utils.dont_run()


class KaggleAuthenticationError(utils.LoggableError):

    def __init__(self, error):
        super().__init__('Kaggle authentication failed', error)


def download(url, output_dir, format=None):
    file = utils.download(url)
    utils.extract(file, output_dir, format)
    utils.delete(file)


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


def images_dir():
    return 'images'


def _on_dir(dir, config_file, func, temp_files):
    import os
    import shutil
    existed = os.path.exists(dir)
    config_output_file = os.path.join(dir, _config_output_name)
    this_images_dir = os.path.join(dir, images_dir())
    if not existed:
        os.mkdir(dir)

    def delete_results():
        utils.delete(config_output_file)
        utils.delete(this_images_dir)

    def delete_temp():
        for temp_file in temp_files:
            utils.delete(os.path.join(dir, temp_file))

    delete_temp()
    delete_results()

    try:
        with utils.cwd(dir):
            func()
        shutil.copyfile(config_file, config_output_file)
    except Exception:
        delete_temp()
        delete_results()
        if existed:
            utils.delete_if_empty_dir(dir)
        else:
            utils.delete(dir)
        raise
    finally:
        delete_temp()


def on_dir(dir, func, temp_files):
    from os import path
    caller_file = path.realpath(utils.get_caller_module().__file__)
    caller_dir = path.dirname(caller_file)
    name = path.basename(caller_file).rstrip('.py')
    config_file = path.join(caller_dir, _config_template_name.format(name))
    _on_dir(dir, config_file, func, temp_files)


def catch_main(func, temp_files):
    if utils.is_caller_main():
        utils.hook_exceptions()
        import argparse
        from os import path
        caller_file = path.realpath(utils.get_caller_module().__file__)
        caller_dir = path.dirname(caller_file)
        name = path.basename(caller_file).rstrip('.py')
        config_file = path.join(caller_dir, _config_template_name.format(name))
        parser = argparse.ArgumentParser(description=f'Download "{name}" dataset', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
        parser.add_argument('-o', '--output-dir', type=utils.output_dir_arg, default='dataset', help='the output directory')
        args = parser.parse_args()
        _on_dir(args.output_dir, config_file, func, temp_files)
