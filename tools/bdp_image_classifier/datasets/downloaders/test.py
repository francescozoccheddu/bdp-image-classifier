from ..download import images_dir
from ...utils import files
from . import supermarket

_class_count = 2


def requires_kaggle():
    return False


def pack_urls():
    return ['https://drive.google.com/u/0/uc?export=download&confirm=_Rm9&id=1fHQUhGKb9fK6fE8RTvdCMPBOd4MiTft0', 'https://download939.mediafire.com/0m7vl9aukvvg/qxr8irg65twdyfza/test.zip']


def temp_files():
    return []


def download():
    from .. import download
    download.download(download.Dataset.supermarket, '.')
    for label in range(_class_count, supermarket._class_count):
        files.delete(f'{images_dir()}/{label}')
