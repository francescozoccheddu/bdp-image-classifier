
from ..utils.launcher import dont_run
from ..utils import files
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
    return files.join(files.get_home(), '.ssh/authorized_keys')


def namenode_dir():
    return 'namenode'


def datanode_dir():
    return 'datanode'


def all_dirs():
    return [spark_dir(), hadoop_dir(), ssh_key_dir(), temp_dir(), jdk_dir(), namenode_dir(), datanode_dir()]


def add_argparse_install_dir(parser):
    from ..utils import cli
    default = files.join(files.get_home(), '.fz-bdp-ic')
    parser.add_argument('-o', '--install-dir', type=cli.output_dir_arg, default=default, help='the install directory')


def ensure_supported_platform():
    import platform
    from ..utils.cli import get_command_path
    system = platform.system()
    if system not in ('Linux'):
        raise RuntimeError(f'Unsupported platform "{system}", only Linux is supported')
    get_command_path('ssh')
    get_command_path('sshd')
    files.create_dir_tree(files.parent(ssh_authorized_keys_file()))
    with open(ssh_authorized_keys_file(), 'a'):
        pass
