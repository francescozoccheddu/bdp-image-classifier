from ..download import images_dir
from ...utils import files
from . import supermarket

_class_count = 2


def temp_files():
    return []


def download():
    from .. import download
    download.download(download.Dataset.supermarket, '.')
    for label in range(_class_count, supermarket._class_count):
        files.delete(f'{images_dir()}/{label}')
