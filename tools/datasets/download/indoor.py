from ..dataset_utils import downloader, download_kaggle, images_dir


@downloader(['indoorCVPR_09', 'indoorCVPR_09annotations', 'TestImages.txt', 'TrainImages.txt'])
def download():
    import shutil
    download_kaggle('itsahmad/indoor-scenes-cvpr-2019', '.')
    shutil.move('indoorCVPR_09/Images', images_dir())
