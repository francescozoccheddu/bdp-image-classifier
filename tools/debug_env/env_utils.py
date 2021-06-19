
from ..utils.exceptions import LoggableError
from ..utils.launcher import dont_run
dont_run()


def spark_dir():
    return 'spark'


def hadoop_dir():
    return 'hadoop'


def jdk_dir():
    return 'jdk'


def temp_dir():
    return 'temp'


def ssh_key_dir():
    return 'ssh-keys'


def ssh_private_key_file():
    return 'ssh-key.pem'


def ssh_public_key_file():
    return 'ssh-key.pub'


def ssh_authorized_keys_file():
    from ..utils import files
    import os
    return os.path.join(files.get_home(), '.ssh/authorized_keys')


def namenode_dir():
    return 'namenode'


def datanode_dir():
    return 'datanode'


def all_dirs():
    return [spark_dir(), hadoop_dir(), ssh_key_dir(), temp_dir(), jdk_dir(), namenode_dir(), datanode_dir()]


def add_argparse_install_dir(parser):
    from ..utils import cliargs, files
    import os
    default = os.path.join(files.get_home(), '.fz-bdp-ic')
    parser.add_argument('-o', '--install-dir', type=cliargs.output_dir, default=default, help='the install directory')


class UnsupportedPlatformError(LoggableError):

    def __init__(self, platform):
        super().__init__(f'Unsupported platform "{platform}". Only Linux is currently supported.', None, platform)
        self._platform = platform

    @property
    def platform(self):
        return self._platform


def ensure_supported_platform():
    import platform
    from ..utils.cli import get_command_path
    system = platform.system()
    if system not in ('Linux'):
        raise UnsupportedPlatformError(system)
    get_command_path('ssh')
    get_command_path('sshd')
    import os
    os.makedirs(os.path.dirname(ssh_authorized_keys_file()), exist_ok=True)
    with open(ssh_authorized_keys_file(), 'a'):
        pass
