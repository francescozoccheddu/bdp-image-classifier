
from ..utils.launcher import main
from ..utils import files
from . import env_utils
import os


def _retrieve(url, output_dir, format, name):
    if os.path.isdir(output_dir):
        from ..utils import cli
        cli.log(f'Skipping {name}')
        return
    files.delete(output_dir)
    files.download_and_extract(url, output_dir, format, True, f'Downloading {name}')


def _copy(src, dst, vars={}):
    import os
    src = os.path.join(os.path.dirname(__file__), f'.{src}.template')
    cnt = files.read(src)
    for k, v in vars.items():
        cnt = cnt.replace(k, v)
    files.write(dst, cnt)


def authorize_ssh(install_dir):
    env_utils.ensure_supported_platform()
    from tools.debug_env.uninstall import revoke_ssh
    revoke_ssh(install_dir)
    import os
    from cryptography.hazmat.primitives.asymmetric import rsa
    from cryptography.hazmat.primitives import serialization as crypto_serialization
    from cryptography.hazmat.backends import default_backend as crypto_default_backend
    key = rsa.generate_private_key(65537, 2047, backend=crypto_default_backend())
    prv_key = key.private_bytes(crypto_serialization.Encoding.PEM, crypto_serialization.PrivateFormat.TraditionalOpenSSL, crypto_serialization.NoEncryption()).decode("utf-8")
    pub_key = key.public_key().public_bytes(crypto_serialization.Encoding.OpenSSH, crypto_serialization.PublicFormat.OpenSSH).decode("utf-8")
    with files.output_dir(os.path.join(install_dir, env_utils.ssh_key_dir()), [env_utils.ssh_private_key_file(), env_utils.ssh_public_key_file()], True):
        pub_key_content = pub_key.split()[1]
        files.write(env_utils.ssh_public_key_file(), pub_key_content)
        files.write(env_utils.ssh_private_key_file(), prv_key)
        os.chmod(env_utils.ssh_private_key_file(), 0o600)
        files.append(env_utils.ssh_authorized_keys_file(), '\n' + pub_key + '\n')
        return pub_key_content


def format_hdfs(install_dir):
    from ..utils import cli
    cmd = os.path.join(install_dir, env_utils.hadoop_dir(), 'bin', 'hdfs')
    cli.run(cmd, ['namenode', '-format'], input=b'Y', noOut=True, noErr=True)


_hadoop_conf_dir = os.path.join(env_utils.hadoop_dir(), 'etc/hadoop')
_spark_conf_dir = os.path.join(env_utils.spark_dir(), 'conf')


def _make_executable(dir):
    for root, dirs, files in os.walk(dir):
        for f in files + dirs:
            os.chmod(os.path.join(root, f), 0o777)


def install(install_dir):
    env_utils.ensure_supported_platform()
    key = None
    try:
        with files.output_dir(install_dir, env_utils.all_dirs(), False):
            key = authorize_ssh('.')
            import struct
            import getpass
            bits = struct.calcsize('P') * 8
            from ..utils.cli import log
            _retrieve(f'https://builds.openlogic.com/downloadJDK/openlogic-openjdk/8u262-b10/openlogic-openjdk-8u262-b10-linux-x{bits}.tar.gz', env_utils.jdk_dir(), 'gztar', 'JDK    (1/3)')
            _retrieve('https://downloads.apache.org/hadoop/common/hadoop-3.2.2/hadoop-3.2.2.tar.gz', env_utils.hadoop_dir(), 'gztar', 'Hadoop (2/3)')
            _retrieve('https://downloads.apache.org/spark/spark-3.1.2/spark-3.1.2-bin-hadoop3.2.tgz', env_utils.spark_dir(), 'gztar', 'Spark  (3/3)')
            log('Configuring')
            files.create_dir(env_utils.namenode_dir(), True)
            files.create_dir(env_utils.datanode_dir(), True)
            files.create_dir(env_utils.temp_dir(), True)
            vars = {
                '%INSTALL_DIR%': os.path.abspath('.'),
                '%JDK_DIR%': env_utils.jdk_dir(),
                '%HADOOP_DIR%': env_utils.hadoop_dir(),
                '%SPARK_DIR%': env_utils.spark_dir(),
                '%NAMENODE_DIR%': env_utils.namenode_dir(),
                '%DATANODE_DIR%': env_utils.datanode_dir(),
                '%TEMP_DIR%': env_utils.temp_dir(),
                '%SSH_KEY_DIR%': env_utils.ssh_key_dir(),
                '%SSH_PRIVATE_KEY_FILE%': env_utils.ssh_private_key_file(),
                '%SSH_PUBLIC_KEY_FILE%': env_utils.ssh_public_key_file(),
                '%USR%': getpass.getuser()
            }
            _copy('core-site.xml', f'{_hadoop_conf_dir}/core-site.xml', vars)
            _copy('hdfs-site.xml', f'{_hadoop_conf_dir}/hdfs-site.xml', vars)
            _copy('log4j.properties', f'{_spark_conf_dir}/log4j.properties', vars)
            _copy('env.sh', f'{_hadoop_conf_dir}/hadoop-env.sh', vars)
            _copy('env.sh', f'{_spark_conf_dir}/spark-env.sh', vars)
            files.create_dir(os.path.join(env_utils.hadoop_dir(), 'logs'))
            _make_executable(env_utils.jdk_dir())
            _make_executable(env_utils.hadoop_dir())
            _make_executable(env_utils.spark_dir())
            format_hdfs(install_dir)
    except BaseException:
        if key is not None:
            from .uninstall import revoke_ssh_key
            revoke_ssh_key(key)
        raise


@main
def _main():
    import argparse
    parser = argparse.ArgumentParser(description='Install a debug environment', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    env_utils.add_argparse_install_dir(parser)
    args = parser.parse_args()
    install(args.install_dir)
