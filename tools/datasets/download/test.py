from ..dataset_utils import downloader, images_dir
from ...utils import files
from . import supermarket

_class_count = 2


@downloader([])
def download():
    supermarket.download('.')
    for label in range(_class_count, supermarket._class_count):
        files.delete(f'{images_dir()}/{label}')
