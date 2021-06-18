
from contextlib import contextmanager


def _get_caller_caller_module():
    import inspect
    frame = inspect.stack()[3]
    return inspect.getmodule(frame[0])


def get_caller_module():
    return _get_caller_caller_module()


def is_caller_main():
    return _get_caller_caller_module().__name__ == '__main__'


def dont_run():
    if is_caller_main():
        import sys
        sys.exit('You should not directly run this script. See README.md for help.')


dont_run()


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


_debug = True


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


def set_exception_hook():

    import sys

    def hook(exc_type, exc, traceback):
        err(exc)
        if _debug:
            sys.__excepthook__(exc_type, exc, traceback)

    sys.excepthook = hook
