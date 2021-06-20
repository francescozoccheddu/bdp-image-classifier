from ..utils.launcher import main
from ..utils import files


def _map_file_cwd(file, cwd):
    from urllib.parse import urlparse
    uri = urlparse(file)
    if uri.scheme or uri.netloc or files.isabs(uri.path):
        return file
    else:
        return f'{cwd}/{file}'


def _map_dataset_cwd(dataset, cwd):
    for images in dataset:
        for i, image in enumerate(images):
            images[i] = _map_file_cwd(image, cwd)


def reconfigure_file(input_file, output_file, *args, **kwargs):
    import json
    config = json.loads(files.read(input_file))
    config = reconfigure(config, *args, **kwargs)
    files.write(output_file, json.dumps(config))


def reconfigure(
        config,
        data_save=None,
        data_temp_file=None,
        data_cwd=None,
        featurization_save=None,
        training_save=None,
        testing_save=None,
        testing_print=None):
    data = config.get('data')
    featurization = config.get('featurization')
    training = config.get('training')
    testing = config.get('testing')
    if data is not None:
        if data_save is not None:
            data['save'] = data_save or None
        make = data.get('make')
        if make is not None:
            if data_temp_file is not None:
                make['tempFile'] = data_temp_file or None
            if data_cwd is not None:
                datasetKeys = ('dataSet', 'trainingSet', 'testSet')
                for datasetKey in datasetKeys:
                    dataset = make.get(datasetKey)
                    if dataset is not None:
                        _map_dataset_cwd(dataset, data_cwd)
    if 'featurization' in config:
        if featurization_save is not None:
            featurization['save'] = featurization_save or None
    if 'training' in config:
        if training_save is not None:
            training['save'] = training_save or None
    if 'testing' in config:
        if testing_print is not None:
            testing['print'] = testing_print
        if testing_save is not None:
            testing['save'] = testing_save or None
    return config


@main
def _main():
    from ..utils import cli
    import argparse
    parser = argparse.ArgumentParser(description=f'Change output paths in a JSON configuration file', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('config_file', metavar='CONFIG_FILE', type=cli.make_input_file_or_parent_arg('config.json'), help='the JSON config file or the dataset directory')
    parser.add_argument('--data-save', help='the data output file')
    parser.add_argument('--data-temp-file', help='the data temporary file')
    parser.add_argument('--data-cwd', help='the datasets working directory')
    parser.add_argument('--featurization-save', help='the features output file')
    parser.add_argument('--training-save', help='the model output file')
    parser.add_argument('--testing-save', help='the testing summary output file')
    parser.add_argument('--testing-print', type=cli.bool_arg, help='whether to print the testing summary to stdout')
    parser.add_argument('-o', '--output-file', type=cli.make_opt_arg(cli.output_file_arg), help='the reconfigured JSON output file, if different than CONFIG_FILE')
    args = parser.parse_args()
    cli.set_exception_hook()
    reconfigure_file(args.config_file, args.output_file or args.config_file, args.data_save, args.data_temp_file, args.data_cwd, args.featurization_save, args.training_save, args.testing_save, args.testing_print)
