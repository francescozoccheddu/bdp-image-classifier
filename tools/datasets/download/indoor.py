from ..dataset_utils import downloader, download_kaggle, images_dir
from ...utils import files

@downloader(['indoorCVPR_09', 'indoorCVPR_09annotations', 'TestImages.txt', 'TrainImages.txt'])
def download():
    download_kaggle('itsahmad/indoor-scenes-cvpr-2019', '.')
    files.move('indoorCVPR_09/Images', images_dir())
