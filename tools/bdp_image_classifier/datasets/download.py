from ..utils import cli, files
from ..utils.launcher import main
import threading

_index = threading.local()
_index.time = None


def update_index():
    import requests
    import time
    _index.time = time.time()
    _index_url = 'https://github.com/francescozoccheddu/bdp-image-classifier/releases/download/latest/datasets.json'
    _index.datasets = requests.get(_index_url).json()


def dataset_names():
    ensure_index_updated()
    return list(_index.datasets.keys())


def ensure_index_updated():
    _index_expire_time = 60 * 5
    import time
    index_time = _index.time
    if index_time is None or time.time() - index_time > _index_expire_time:
        update_index()


_config_output_name = 'config.json'


def _download_data(urls, output_dir):
    import random
    for group in urls:
        group = group.copy()
        random.shuffle(group)
        for url in group:
            try:
                files.download_and_extract(url, output_dir, 'zip')
                return
            except Exception as exc:
                cli.log('Download attempt failed. Moving to the next source.')
                last_exc = exc
    raise last_exc


def download(dataset, output_dir):
    ensure_index_updated()
    import json
    dataset = _index.datasets[dataset]
    with files.output_dir(output_dir, ['images', _config_output_name]):
        _download_data(dataset['urls'], '.')
        files.write(_config_output_name, json.dumps(dataset['config']))


def _print_datasets():
    ensure_index_updated()
    from tabulate import tabulate
    from ..utils.cli import humanize_size
    table = [[k, humanize_size(v['size']), v['classes'], v['images'], v['source']] for k, v in _index.datasets.items()]
    headers = ['name', 'size', 'classes', 'images', 'source']
    print(tabulate(table, headers=headers))


@main
def _main():
    import argparse
    ensure_index_updated()
    parser = argparse.ArgumentParser(description=f'Download preconfigured dataset', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('dataset', metavar='DATASET', choices=dataset_names(), nargs='?', help='the dataset')
    parser.add_argument('-o', '--output-dir', type=cli.output_dir_arg, default='dataset', help='the output directory')
    parser.add_argument('-l', '--list-datasets', action='store_true', help='list the available datasets and exit')
    cli.add_argparse_quiet(parser)
    args = parser.parse_args()
    cli.set_exception_hook()
    cli.set_logging(not args.quiet)
    if args.list_datasets:
        _print_datasets()
    else:
        if not args.dataset:
            parser.error('DATASET argument is required')
        download(args.dataset, args.output_dir)
