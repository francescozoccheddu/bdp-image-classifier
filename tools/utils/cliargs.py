from . import files
from .launcher import dont_run
import argparse
dont_run()


def output_file(arg):
    if not files.is_file_writable(arg):
        raise argparse.ArgumentTypeError('not a writable file')
    return arg


def output_dir(arg):
    if not files.is_dir_writable(arg):
        raise argparse.ArgumentTypeError('not a writable dir')
    return arg


def input_file(arg):
    if not files.is_file_readable(arg):
        raise argparse.ArgumentTypeError('not a readable file')
    return arg


def boolean(arg):
    arg = arg.lower()
    if arg in ('1', 'true', 'y', 'yes'):
        return True
    elif arg in ('0', 'false', 'n', 'no'):
        return False
    else:
        raise argparse.ArgumentTypeError('not a boolean value')


def make_input_file_or_parent(file):

    def process(arg):
        child = files.join(arg, file)
        if files.is_file_readable(arg):
            return arg
        elif files.is_dir_readable(arg) and files.is_file_readable(child):
            return child
        else:
            raise argparse.ArgumentTypeError(f'not a readable file nor a directory with "{child}" file')

    return process


def input_dir(arg):
    if not files.is_dir_readable(arg):
        raise argparse.ArgumentTypeError('not a readable dir')
    return arg
