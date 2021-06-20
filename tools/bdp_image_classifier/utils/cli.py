
import os
import sys
from contextlib import contextmanager
from .launcher import dont_run
import argparse
from . import files
dont_run()

_debug = True
_log = False


def _stop(msg):
    msg = msg.capitalize()
    if msg.endswith('.'):
        return msg
    else:
        return msg + '.'


def _humanize_exc(exc, exc_type):
    if issubclass(exc_type, KeyboardInterrupt):
        return 'Canceled by user.'
    if issubclass(exc_type, ImportError):
        return f'Cannot import module "{exc.name}".See README.md for installation help.'
    if issubclass(exc_type, OSError) and exc.strerror:
        msg = _stop(exc.strerror)
        if exc.filename is not None:
            msg += f' (file "{exc.filename}")'
        return msg
    if exc.args:
        return _stop(exc.args[0])
    return 'Unhandled exception.'


def _exc_hook(exc_type, exc, traceback):

    err(_humanize_exc(exc, exc_type))

    if _debug:
        sys.__excepthook__(exc_type, exc, traceback)


def set_exception_hook(enabled=True):
    if enabled:
        sys.excepthook = _exc_hook
    else:
        sys.excepthook = sys.__excepthook__


def set_logging(enabled=True):
    global _log
    _log = enabled


def err(msg):
    print(msg, file=sys.stderr)


def log(msg):
    if _log:
        print(msg, file=sys.stdout)


@contextmanager
def no_stdout():
    with open(os.devnull, 'w') as devnull:
        old_stdout = sys.stdout
        sys.stdout = devnull
        try:
            yield
        finally:
            sys.stdout = old_stdout


def get_command_path(cmd):
    import shutil
    path = shutil.which(cmd)
    if not path:
        raise RuntimeError(f'Command {cmd} not found')
    return path


def is_logging():
    return _log


def add_argparse_quiet(parser):
    parser.add_argument('-q', '--quiet', action='store_true', help='disable logging')


def run(cmd, args, cwd=os.getcwd(), input=None, enable_out=None, enable_err=None):
    if enable_out is None:
        enable_out = _log
    if enable_err is None:
        enable_err = _log
    from . import files
    file = files.abs(get_command_path(cmd))
    import subprocess
    stdout = sys.stdout if enable_out else subprocess.DEVNULL
    stderr = sys.stderr if enable_err else subprocess.DEVNULL
    return subprocess.run([file, *args], cwd=cwd, input=input, stdout=stdout, stderr=stderr).returncode


def output_file_arg(arg):
    arg = files.expand_user(arg)
    if not files.is_file_writable(arg):
        raise argparse.ArgumentTypeError('not a writable file')
    return arg


def output_dir_arg(arg):
    arg = files.expand_user(arg)
    if not files.is_dir_writable(arg):
        raise argparse.ArgumentTypeError('not a writable dir')
    return arg


def input_file_arg(arg):
    arg = files.expand_user(arg)
    if not files.is_file_readable(arg):
        raise argparse.ArgumentTypeError('not a readable file')
    return arg


def bool_arg(arg):
    arg = arg.lower()
    if arg in ('1', 'true', 'y', 'yes'):
        return True
    elif arg in ('0', 'false', 'n', 'no'):
        return False
    else:
        raise argparse.ArgumentTypeError('not a boolean value')


def make_input_file_or_parent_arg(file):

    def process(arg):
        arg = files.expand_user(arg)
        child = files.join(arg, file)
        if files.is_file_readable(arg):
            return arg
        elif files.is_dir_readable(arg) and files.is_file_readable(child):
            return child
        else:
            raise argparse.ArgumentTypeError(f'not a readable file nor a directory with "{file}" file')

    return process


def make_int_arg(min=None, max=None):

    def process(arg):
        msg = 'not an integer'
        if min is not None:
            if max is not None:
                msg += f' between {min} and {max}'
            else:
                msg += f' greater than {min}'
        elif max is not None:
            msg += f' smaller than {max}'
        try:
            value = int(arg)
        except ValueError:
            raise argparse.ArgumentError(msg)
        if (min is not None and value < min) or (max is not None and value > max):
            raise argparse.ArgumentError(msg)
        value

    return process


def input_dir_arg(arg):
    arg = files.expand_user(arg)
    if not files.is_dir_readable(arg):
        raise argparse.ArgumentTypeError('not a readable dir')
    return arg


def pause():
    input('Press ENTER to continue or CTRL+C to cancel... ')
