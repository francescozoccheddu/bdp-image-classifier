from ..utils import cli, files
from ..utils.launcher import main


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


def download(dataset, output_dir):

    module = _get_module(dataset)
    config_file = files.resource(_config_template_name.format(dataset), module)

    with files.output_dir(output_dir, module.temp_files() + [images_dir(), _config_output_name], True):
        done = False
        for pack_url in module.pack_urls():
            try:
                files.download_and_extract(pack_url, output_dir, 'zip')
                done = True
                break
            except Exception:
                cli.log('Failed to download packed version. Moving to another source.')
        if not done:
            module.download()
        files.copy(config_file, _config_output_name)
        for temp_file in module.temp_files():
            files.delete(temp_file)


class Dataset(cli.Choice):
    test = 'test'
    supermarket = 'supermarket'
    land = 'land'
    indoor = 'indoor'


def get_kaggle_credentials():
    with cli.no_stdout():
        from kaggle.api.kaggle_api_extended import KaggleApi
        api = KaggleApi()
        api.authenticate()
    return api.config_values[api.CONFIG_NAME_USER], api.config_values[api.CONFIG_NAME_KEY]


def get_kaggle_credentials_for_dataset(dataset):
    if requires_kaggle(dataset):
        try:
            return get_kaggle_credentials()
        except Exception:
            if any(_get_module(dataset).pack_urls()):
                cli.log('No Kaggle credentials found. Continuing anyway as the dataset provides some alternative sources.')
            else:
                raise
    else:
        return '', ''


def requires_kaggle(dataset):
    return _get_module(dataset).requires_kaggle()


def _get_module(dataset):
    import importlib
    parent_module = '.'.join(__name__.split('.')[:-1])
    return importlib.import_module(f'.downloaders.{dataset.value}', parent_module)


@main
def _main():
    import argparse
    parser = argparse.ArgumentParser(description=f'Download preconfigured dataset', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('dataset', metavar='DATASET', type=Dataset, choices=Dataset.choices(), help='the dataset')
    parser.add_argument('-o', '--output-dir', type=cli.output_dir_arg, default='dataset', help='the output directory')
    cli.add_argparse_quiet(parser)
    args = parser.parse_args()
    cli.set_exception_hook()
    cli.set_logging(not args.quiet)
    download(args.dataset, args.output_dir)
