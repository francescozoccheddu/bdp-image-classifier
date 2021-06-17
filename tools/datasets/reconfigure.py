default_print_summary = True
default_data_temp_file = 'hdfs:///image-classifier/data'
default_model_output_file = 'hdfs:///image-classifier/model'
default_summary_output_file = 'hdfs:///image-classifier/summary'
default_dataset_cwd = '.'


def reconfigure(
        print_summary=True,
        data_temp_file=default_data_temp_file,
        model_output_file=default_model_output_file,
        summary_output_file=default_summary_output_file,
        dataset_cwd=default_dataset_cwd):
    pass