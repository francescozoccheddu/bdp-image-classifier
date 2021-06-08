# Datasets

## List of supported datasets

Here is a list of datasets that you can try:

| Name               | Size  | Images | Classes | Source                              |
| :----------------: | :---: | :----: | :-----: | :---------------------------------: |
| supermarket        | 275MB | 13300  | 16      | https://iplab.dmi.unict.it/MLC2018/ |

## Downloading a dataset

Run *`<DATASET_NAME>`*`/download.sh "`*`<OUTPUT_DIRECTORY>`*`"`, like this:
```bash
supermarket/download.sh supermarket
```
The script will download the dataset and provide you a ready-to-use *`<OUTPUT_DIRECTORY>`*`/config.json` config file, that you can pass to the classifier app.  
You may want to edit it for better tuning of the parameters.