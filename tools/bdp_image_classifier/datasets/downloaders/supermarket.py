from ..download import images_dir
from ...utils import files

_class_count = 15


def requires_kaggle():
    return False


def pack_urls():
    return ['https://drive.google.com/u/0/uc?export=download&confirm=_Rm9&id=1NpXmYM6kBalC0GCOtf1le4qSPuCsP8aG', 'https://download1080.mediafire.com/ffe5scm06p2g/mymy93mh1zwykjd/supermarket.zip']


def _process_csv(file):
    import csv
    res = [[] for _ in range(_class_count)]
    with open(file, newline='') as f:
        reader = csv.reader(f)
        for row in reader:
            label = int(row[5]) - 1
            file = row[0]
            res[label] += [file]
    return res


def temp_files():
    return ['training_list.csv', 'validation_list.csv', 'testing_list_blind.csv', 'README.txt', '.images']


def download():
    files.download_and_extract('https://iplab.dmi.unict.it/MLC2018/dataset.zip', '.', 'zip')
    files.move(images_dir(), '.images')
    files.create_dir(images_dir())
    images_map = list(map(lambda l: l[0] + l[1], zip(_process_csv('validation_list.csv'), _process_csv('training_list.csv'))))
    for label in range(_class_count):
        files.create_dir(f'{images_dir()}/{label}')
    for label, images in enumerate(images_map):
        for image in images:
            files.move(f'.images/{image}', f'{images_dir()}/{label}')
