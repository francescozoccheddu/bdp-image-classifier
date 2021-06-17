from .. import dataset_utils

_temp_files = ['indoorCVPR_09', 'indoorCVPR_09annotations', 'TestImages.txt', 'TrainImages.txt']


def _process():
    import shutil
    dataset_utils.download_kaggle('itsahmad/indoor-scenes-cvpr-2019', '.')
    shutil.move('indoorCVPR_09/Images', 'images')


def download(output_dir):
    dataset_utils.on_dir(output_dir, _process, _temp_files)


dataset_utils.catch_main(_process, _temp_files)
