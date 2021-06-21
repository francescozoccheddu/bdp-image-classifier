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

    import importlib
    parent_module = '.'.join(__name__.split('.')[:-1])
    module = importlib.import_module(f'.downloaders.{dataset.value}', parent_module)
    config_file = files.resource(_config_template_name.format(dataset), module)
    temp_files = module.temp_files()
    downloader = module.download()

    with files.output_dir(output_dir, temp_files + [images_dir(), _config_output_name], True):
        downloader()
        files.copy(config_file, _config_output_name)
        for temp_file in temp_files:
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


def requires_kaggle(dataset):
    return dataset in (Dataset.indoor,)


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
