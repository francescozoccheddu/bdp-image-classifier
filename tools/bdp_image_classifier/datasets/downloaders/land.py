from ..download import images_dir
from ...utils import files


def requires_kaggle():
    return False


def pack_urls():
    return ['https://drive.google.com/u/0/uc?export=download&confirm=2dS6&id=1iuSSUPWhptPoV8-6h0b9OSjHdHIZJRXo', 'https://download1481.mediafire.com/wfl8o977whrg/ea8eq25cdcg234b/land.zip']


def temp_files():
    return []


def download():
    files.create_dir(images_dir())
    files.download_and_extract('https://storage.googleapis.com/kaggle-data-sets/915557/1552478/upload/images.zip?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=gcp-kaggle-com%40kaggle-161607.iam.gserviceaccount.com%2F20210624%2Fauto%2Fstorage%2Fgoog4_request&X-Goog-Date=20210624T092223Z&X-Goog-Expires=259199&X-Goog-SignedHeaders=host&X-Goog-Signature=9ec588307b9b0c84c9ee15d5bf4236c8eb41410396ab5e77b6e56ed1911c037ca38db9b7a3fa446ec4afb483dc656c9ad53be773f520c818fa87d59d045d31d5c9a3bae96b124828bd3e6ac196247c76c51d5a28b5452d07a2a0b81b07d7e21d312a29a2a2f9f832d6f92f6ee258816bb9bc6e3b55a1a66510fb0482e27f1cce0ff1f620c99ffa02222abb45906dfb8534f2716b8b558906fedc0e66655a8edf750c008c09d00516a94bce193e8ff5ea71ce53aeca069de967c281f6710ec5952b63d9462281cb5c34a3a53075a9c1a372e08e252af8acdca23ac5193ba746ca29da85eb06f97ac2bc8a889aaeb5e55fefbd2442e267b8f28a54867577eb7700', images_dir(), 'zip')
