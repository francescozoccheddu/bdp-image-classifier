from ..utils import cli, files
from ..utils.launcher import dont_run
dont_run()


def download_kaggle(dataset, output_dir):
    with cli.no_stdout():
        from kaggle.api.kaggle_api_extended import KaggleApi
        api = KaggleApi()
        api.authenticate()
    api.dataset_download_files(dataset, output_dir, True, cli.is_logging(), True)


_config_template_name = '.{}-config.json.template'
_config_output_name = 'config.json'


def images_dir():
    return 'images'


def downloader(temp_files):

    def decorator(func):
        from ..utils import launcher
        import inspect
        module = inspect.getmodule(func)
        script_file = module.__file__
        script_dir = files.parent(script_file)
        dataset = files.name(script_file).rstrip('.py')
        config_file = files.join(script_dir, _config_template_name.format(dataset))

        def download(dir):
            with files.output_dir(dir, temp_files + [images_dir(), _config_output_name], True):
                func()
                files.copy(config_file, _config_output_name)
                for temp_file in temp_files:
                    files.delete(temp_file)

        if launcher.is_main_module(module):
            cli.set_logging()
            cli.set_exception_hook()
            from ..utils import cliargs
            import argparse
            parser = argparse.ArgumentParser(description=f'Download "{dataset}" dataset', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
            parser.add_argument('-o', '--output-dir', type=cliargs.output_dir, default='dataset', help='the output directory')
            args = parser.parse_args()
            download(args.output_dir)

        return download

    return decorator
