
from .. import utils


def _retrieve(url, output_dir):
    import os
    if os.path.isdir(output_dir):
        return
    utils.delete(output_dir)
    file = utils.download(url)
    utils.extract(file, output_dir, 'tgz')


def install(install_dir):
    import os
    with utils.cwd(install_dir):
        pass
