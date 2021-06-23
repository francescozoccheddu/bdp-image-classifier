from ..download import images_dir
from ...utils import files


def temp_files():
    return []


def download():
    files.create_dir(images_dir())
    files.download_and_extract('https://storage.googleapis.com/kaggle-data-sets/915557/1552478/upload/images.zip?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=gcp-kaggle-com%40kaggle-161607.iam.gserviceaccount.com%2F20210623%2Fauto%2Fstorage%2Fgoog4_request&X-Goog-Date=20210623T083141Z&X-Goog-Expires=259199&X-Goog-SignedHeaders=host&X-Goog-Signature=226bb5fd11f079002d99e2dbb0afdcb6005da230bc4da9672ea7caabd6a0cda1de83b35273686377050e3c6e65fc062fc7d1886b54b576628ae152c9ebde6a893b22fec0116c7c6834f225b2c5e431821cda44545b2460800c4a38c646393ac62cd95b0ffa01b81c4f149d5e8bba36e6370e616900902108912abe0ee06414dbf5a2421480c5c6153741820d494994f8e37431fb4356d8ce693ec961754dec891ade6e8ae4ffe2ab2e1438fd6dfa7587c738abc7d2935e3df6a6bb114471fa24c21c7ce666149878127cb737a9d1528aafae09de0f5e1250357d252c27bf5ac82b3da2470d1bbe40d9fce769e2a0c56b32d88b90945fc7cffb97d8b0c60fa8be', images_dir(), 'zip')
