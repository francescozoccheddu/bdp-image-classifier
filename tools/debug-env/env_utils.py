
from .. import utils
utils.dont_run()

_spark_dir = 'spark'
_hadoop_dir = 'hadoop'
_jdk_dir = 'jdk'
_data_dir = 'data'
_temp_dir = 'temp'
_ssh_private_key_file = '.ssh-key'
_ssh_public_key_file = '.ssh-key.pub'


def spark_dir():
    return _spark_dir


def hadoop_dir():
    return _hadoop_dir


def jdk_dir():
    return _jdk_dir


def data_dir():
    return _data_dir


def temp_dir():
    return _temp_dir


def ssh_private_key_file():
    return _ssh_private_key_file


def ssh_public_key_file():
    return _ssh_public_key_file


class UnsupportedPlatformError(utils.LoggableError):

    def __init__(self, platform):
        super().__init__(f'Unsupported platform "{platform}". Only Linux is currently supported.')


def _ensure_supported_platform():
    import platform
    system = platform.system()
    if system not in ('Linux'):
        raise UnsupportedPlatformError(system)
