from . import env_utils
from .. import utils


def uninstall(install_dir):
    env_utils._ensure_supported_platform()
    import os
    if not os.path.isdir(install_dir):
        
    with utils.cwd(install_dir):
        try:
            revoke_ssh('.')
        except utils.LoggableError as e:
            e.log()
        utils.delete(env_utils.spark_dir(), True)
        utils.delete(env_utils.hadoop_dir(), True)
        utils.delete(env_utils.jdk_dir(), True)
        utils.delete(env_utils.data_dir(), True)
        utils.delete(env_utils.temp_dir(), True)
        utils.delete(env_utils.ssh_tag_file(), True)
        utils.delete_if_empty_dir(abs_dir, True)

class No

class BadSSHTagError(utils.LoggableError):

    def __init__(self):
        super().__init__('Malformed SSH tag.')


def revoke_ssh(install_dir):
    env_utils._ensure_supported_platform()
    import os
    if op
    private_key_file = os.path.join(install_dir, env_utils.ssh_tag_file())
    public_key_file = os.path.join(install_dir, env_utils.ssh_tag_file())
    if os.path.isfile(ssh_tag_file):
        try:
            with open(ssh_tag_file) as f:
                lines = f.readlines()
        except Exception:
            raise BadSSHTagError()
        if len(lines) != 1:
            raise BadSSHTagError()
        tag = lines[0]
        import re
        if not (re.fullmatch(r'[a-z0-9-]+', tag, re.IGNORECASE) and tag.startswith(env_utils.ssh_tag_prefix())):
            raise BadSSHTagError()
        with open("file.txt", "r+") as f:
            lines = f.readlines()
            f.seek(0)
            for line in lines:
                if not line.rstrip().endswith(tag):
                    f.write(line)
            f.truncate()
