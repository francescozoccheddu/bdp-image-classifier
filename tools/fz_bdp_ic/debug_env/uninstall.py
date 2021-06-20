from ..utils.launcher import main
from . import env_utils
from ..utils import files


def uninstall(install_dir):
    env_utils.ensure_supported_platform()
    if files.is_dir(install_dir):
        revoke_ssh(install_dir)
        files.delete_output_dir(install_dir, env_utils.all_dirs(), False)
        return True
    else:
        return False


def revoke_ssh_key(key):
    env_utils.ensure_supported_platform()
    if files.exists(env_utils.ssh_authorized_keys_file()):
        files.filter_file_lines(env_utils.ssh_authorized_keys_file(), lambda l: key not in l and l.strip())
        return True
    else:
        return False


def revoke_ssh(install_dir):
    env_utils.ensure_supported_platform()
    ssh_dir = files.join(install_dir, env_utils.ssh_key_dir())
    pub_file = files.join(ssh_dir, env_utils.ssh_public_key_file())
    if files.exists(pub_file) and files.exists(env_utils.ssh_authorized_keys_file()):
        pub_key_lines = files.read(pub_file).strip().splitlines()
        if len(pub_key_lines) != 1:
            raise RuntimeError('Bad SSH key')
        key = pub_key_lines[0].strip()
        if not key:
            raise RuntimeError('Empty SSH key')
        revoke_ssh_key(key)
        revoked = True
    else:
        revoked = False
    files.delete(ssh_dir)
    return revoked


@main
def _main():
    import argparse
    from ..utils import cli
    parser = argparse.ArgumentParser(description='Uninstall a debug environment', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    env_utils.add_argparse_install_dir(parser)
    cli.add_argparse_quiet(parser)
    args = parser.parse_args()
    cli.set_exception_hook()
    cli.set_logging(not args.quiet)
    uninstall(args.install_dir)
