

def is_main_module(mod):
    return mod.__name__ == '__main__'


def dont_run(skip=0):
    if is_main_module(get_caller_module(skip + 1)):
        import sys
        sys.exit('You should not run this script directly. See README.md for help.')


def get_caller_module(skip=0):
    import inspect
    frame = inspect.stack()[skip + 1]
    return inspect.getmodule(frame[0])


def is_caller_main(skip=0):
    is_main_module(get_caller_module(skip + 1))


def main(func):
    import inspect
    if is_main_module(inspect.getmodule(func)):
        from . import cli
        cli.set_log_enabled()
        cli.set_exception_hook()
        func()
    return func


dont_run()
