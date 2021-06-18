from ..dataset_utils import downloader, images_dir

_class_count = 15


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


@downloader(['training_list.csv', 'validation_list.csv', 'testing_list_blind.csv', 'README.txt', '.images'])
def download():
    import shutil
    import os
    from ...utils import files
    files.download_and_extract('https://iplab.dmi.unict.it/MLC2018/dataset.zip', '.', 'zip')
    shutil.move(images_dir(), '.images')
    os.mkdir(images_dir())
    images_map = list(map(lambda l: l[0] + l[1], zip(_process_csv('validation_list.csv'), _process_csv('training_list.csv'))))
    for label in range(_class_count):
        os.mkdir(f'{images_dir()}/{label}')
    for label, images in enumerate(images_map):
        for image in images:
            shutil.move(f'.images/{image}', f'{images_dir()}/{label}')
