from ..utils.launcher import main
from ..utils import files

_default_data_save = None
_default_data_temp_file = 'hdfs:///image-classifier/data'
_default_featurization_save = None
_default_training_save = 'hdfs:///image-classifier/model'
_default_testing_save = 'hdfs:///image-classifier/summary'
_default_testing_print = True
_default_data_cwd = '.'


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


def reconfigure(
        file,
        data_save=_default_data_save,
        data_temp_file=_default_data_temp_file,
        data_cwd=_default_data_cwd,
        featurization_save=_default_featurization_save,
        training_save=_default_training_save,
        testing_save=_default_testing_save,
        testing_print=_default_testing_print):
    import json
    config = json.loads(files.read(file))
    data = config.get('data')
    featurization = config.get('featurization')
    training = config.get('training')
    testing = config.get('testing')
    if data is not None:
        data['save'] = data_save
        make = data.get('make')
        if make is not None:
            make['tempFile'] = data_temp_file
            datasetKeys = ('dataSet', 'trainingSet', 'testSet')
            for datasetKey in datasetKeys:
                dataset = make.get(datasetKey)
                if dataset is not None:
                    _map_dataset_cwd(dataset, data_cwd)
    if 'featurization' in config:
        featurization['save'] = featurization_save
    if 'training' in config:
        training['save'] = training_save
    if 'testing' in config:
        testing['print'] = testing_print
        testing['save'] = testing_save
    files.write(file, json.dumps(config))


@main
def _main():
    from ..utils import cliargs
    import argparse
    parser = argparse.ArgumentParser(description=f'Change output paths in a JSON configuration file', formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('config_file', metavar='CONFIG_FILE', type=cliargs.make_input_file_or_parent('config.json'), help='the JSON config file or the dataset directory')
    parser.add_argument('--data-save', type=str, default=_default_data_save, help='the data output file')
    parser.add_argument('--data-temp-file', type=str, default=_default_data_temp_file, help='the data temporary file')
    parser.add_argument('--data-cwd', type=str, default=_default_data_cwd, help='the datasets working directory')
    parser.add_argument('--featurization-save', type=str, default=_default_featurization_save, help='the features output file')
    parser.add_argument('--training-save', type=str, default=_default_training_save, help='the model output file')
    parser.add_argument('--testing-save', type=str, default=_default_testing_save, help='the testing summary output file')
    parser.add_argument('--testing-print', type=cliargs.boolean, default=_default_testing_print, help='whether to print the testing summary to stdout')
    args = parser.parse_args()
    reconfigure(args.config_file, args.data_save, args.data_temp_file, args.data_cwd, args.featurization_save, args.training_save, args.testing_save, args.testing_print)
