from .cli import dont_run
from .files import is_dir_readable, is_file_readable, is_dir_writable, is_file_writable
dont_run()


def output_file_arg(arg):
    if not is_file_writable(arg):
        import argparse
        raise argparse.ArgumentTypeError('not a writable file')
    return arg


def output_dir_arg(arg):
    if not is_dir_writable(arg):
        import argparse
        raise argparse.ArgumentTypeError('not a writable dir')
    return arg


def input_file_arg(arg):
    if not is_file_readable(arg):
        import argparse
        raise argparse.ArgumentTypeError('not a readable file')
    return arg


def bool_arg(arg):
    arg = arg.lower()
    if arg in ('1', 'true', 'y', 'yes'):
        return True
    elif arg in ('0', 'false', 'n', 'no'):
        return False
    else:
        import argparse
        raise argparse.ArgumentTypeError('not a boolean value')


def input_file_or_parent_argb(file):

    def process(arg):
        import os
        child = os.path.join(arg, file)
        if is_file_readable(arg):
            return arg
        elif is_dir_readable(arg) and is_file_readable(child):
            return child
        else:
            import argparse
            raise argparse.ArgumentTypeError(f'not a readable file nor a directory with "{child}" file')

    return process


def input_dir_arg(arg):
    if not is_dir_readable(arg):
        import argparse
        raise argparse.ArgumentTypeError('not a readable dir')
    return arg
