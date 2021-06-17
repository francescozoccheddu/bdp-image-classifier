from .. import dataset_utils

_temp_files = ['training_list.csv', 'validation_list.csv', 'testing_list_blind.csv', 'README.txt', '.images']
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


def _process():
    import shutil
    import os
    dataset_utils.download('https://iplab.dmi.unict.it/MLC2018/dataset.zip', '.', 'zip')
    shutil.move('images', '.images')
    os.mkdir(dataset_utils.images_dir())
    images_map = list(map(lambda l: l[0] + l[1], zip(_process_csv('validation_list.csv'), _process_csv('training_list.csv'))))
    for label in range(_class_count):
        os.mkdir(f'{dataset_utils.images_dir()}/{label}')
    for label, images in enumerate(images_map):
        for image in images:
            shutil.move(f'.images/{image}', f'{dataset_utils.images_dir()}/{label}')


def download(output_dir):
    dataset_utils.on_dir(output_dir, _process, _temp_files)


dataset_utils.catch_main(_process, _temp_files)
