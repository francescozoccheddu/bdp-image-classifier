
def _is_main(mod):
    return mod.__name__ == '__main__'


def launcher(func):

    def actual_launcher(main):
        def prevent():
            raise RuntimeError('Main function call.')

        import inspect
        module = inspect.getmodule(main)
        if _is_main(module):
            from . import cli
            cli.set_exception_hook()
            func(main)
        return prevent

    return actual_launcher


def dont_run():
    import inspect
    frame = inspect.stack()[1]
    if _is_main(inspect.getmodule(frame[0])):
        import sys
        sys.exit('You should not run this script directly. See README.md for help.')


@launcher
def main(func):
    func()


dont_run()
