from ..utils.launcher import main
from . import env_utils
from ..utils import files, exceptions
import os


def uninstall(install_dir):
    env_utils.ensure_supported_platform()
    import os
    if os.path.isdir(install_dir):
        revoke_ssh(install_dir)
        files.delete_output_dir(install_dir, env_utils.all_dirs(), False)
        return True
    else:
        return False


class BadSSHTagError(exceptions.LoggableError):

    def __init__(self):
        super().__init__('Malformed SSH tag.')

def revoke_ssh_key(key):
    if os.path.exists(env_utils.ssh_authorized_keys_file()):
        with open(env_utils.ssh_authorized_keys_file(), "r+") as f:
            lines = f.readlines()
            f.seek(0)
            for line in lines:
                if key not in line and line.strip():
                    f.write(line)
            f.truncate()
        return True
    else:
        return False

def revoke_ssh(install_dir):
    env_utils.ensure_supported_platform()
    ssh_dir = os.path.join(install_dir, env_utils.ssh_key_dir())
    pub_file = os.path.join(ssh_dir, env_utils.ssh_public_key_file())
    if os.path.exists(pub_file) and os.path.exists(env_utils.ssh_authorized_keys_file()):
        pub_key_lines = files.read(pub_file).strip().splitlines()
        if len(pub_key_lines) != 1:
            raise BadSSHTagError()
        key = pub_key_lines[0].strip()
        if not key:
            raise BadSSHTagError()
        revoke_ssh_key(key)
        revoked = True
    else:
        revoked = False
    files.delete(ssh_dir)
    return revoked

@main
def _main():
    import argparse
    parser = argparse.ArgumentParser(description='Uninstall a debug environment', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    env_utils.add_argparse_install_dir(parser)
    args = parser.parse_args()
    uninstall(args.install_dir)