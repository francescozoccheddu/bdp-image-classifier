from ..dataset_utils import downloader, images_dir
from ...utils import files


@downloader([])
def download():
    files.create_dir(images_dir())
    files.download_and_extract('https://storage.googleapis.com/kaggle-data-sets/915557/1552478/upload/images.zip?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=gcp-kaggle-com%40kaggle-161607.iam.gserviceaccount.com%2F20210617%2Fauto%2Fstorage%2Fgoog4_request&X-Goog-Date=20210617T110943Z&X-Goog-Expires=259199&X-Goog-SignedHeaders=host&X-Goog-Signature=a21975b55aef90169e952f5fd899e02eb766b5ac4b870bc749114d912b10c60a12c6907651fee04c4ce6ec374c3b49b93d7042d151f7b05b52a348329e30dac2de3afe6335d5c7385051fe235b3d415a5e9ffc95194917b2c058081ca59c2bbe6b3f97bf21e083778ddcf0687843034d9763b0a602a0b374c59f82a69bfd19d22c2d755d991acdcab790b12231f784a306cc0f8ecfd0177d7d9f649bef0d1f277450cc2d2dcf575868f71dee06516aae1edba15d5f27c6fc808f1a13e7004c0d283765bd3fa8fd6c6255a3f0786bc5963669a03d267310d23ce60460b799d66d80889da342cd6fdc81de8fc2dbc39ab05f35e4c5acca03b751e58fb6976b1155', images_dir(), 'zip')
