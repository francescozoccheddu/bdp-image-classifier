
from .exceptions import LoggableError
from contextlib import contextmanager
from .launcher import dont_run
dont_run()

_debug = True


def set_exception_hook():
    import sys

    def hook(exc_type, exc, traceback):
        from .cli import err
        err(exc)
        if _debug:
            sys.__excepthook__(exc_type, exc, traceback)

    sys.excepthook = hook


def log(msg):
    import sys
    print(msg, file=sys.stdout)


def err(msg):
    import sys
    if isinstance(err, Exception):
        msg = humanize_exc(err)
    print(msg, file=sys.stderr)


def is_multiline(msg):
    return msg.strip().count('\n') > 0


def quote(msg):
    return f'"{msg}"'


def parens(msg):
    msg = msg.strip()
    if msg:
        return f'({msg})'
    else:
        return ''


def humanize_exc(exc):
    exc_type = type(exc)
    from .exceptions import LoggableError
    if issubclass(exc_type, LoggableError):
        return exc.message
    elif issubclass(exc_type, KeyboardInterrupt):
        return 'Cancelled by user.'
    elif issubclass(exc_type, ImportError):
        return f'Module "{exc.name}" cannot be imported. See README.md for installation help.'
    elif issubclass(exc_type, OSError):
        return concat('OS error', concat(exc.strerror, parens(listing(exc.filename, exc.filename2))), sep=':', empty_sep='.')
    else:
        return concat('Unhandled exception', str(exc), sep=':', empty_sep='.')


def listing(*msgs, quote=True):
    res = ''
    if quote:
        msgs = list(map(quote, msgs))
    else:
        msgs = list(filter(bool, map(str.strip, msgs)))
    for msg in msgs[:-1]:
        res += msg + ', '
    if len(msgs) > 1:
        res += ' and '
    if msgs:
        res += msgs[-1]
    return res


def concat(msg_a, msg_b, sep='', empty_sep=''):
    msg_a = msg_a.strip()
    msg_b = msg_b.strip()
    if not msg_b:
        return at_end(empty_sep)
    if not msg_a:
        return msg_b
    if is_multiline(msg_a) or is_multiline(msg_b):
        sep += '\n'
    else:
        sep += ' '
    return at_end(msg_a, sep) + msg_b


def at_end(msg, end):
    if msg.endswith(end):
        return msg
    else:
        return msg + end


@contextmanager
def no_stdout():
    import sys
    import os
    with open(os.devnull, 'w') as devnull:
        old_stdout = sys.stdout
        sys.stdout = devnull
        try:
            yield
        finally:
            sys.stdout = old_stdout


class NoCommandError(LoggableError):

    def __init__(self, command):
        super().__init__(f'Command "{command}" not found. See README.md for installation help.')


def get_command_path(cmd):
    import shutil
    path = shutil.which(cmd)
    if not path:
        raise NoCommandError(cmd)
    return path
