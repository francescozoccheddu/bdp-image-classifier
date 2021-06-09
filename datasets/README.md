# Datasets

## List of supported datasets

Here is a list of datasets that you can try:

| Name               | Size  | Images | Classes | Source                                                       |
| :----------------: | :---: | :----: | :-----: | :----------------------------------------------------------: |
| supermarket        | 275MB | 13360  | 16      | https://iplab.dmi.unict.it/MLC2018/                          |
| indoor             | 2.6GB | 15613  | 67      | https://www.kaggle.com/itsahmad/indoor-scenes-cvpr-2019      |
| natural            | 259MB | 17034  | 6       | https://www.kaggle.com/puneet6060/intel-image-classification |

## Downloading a dataset

Run <code>*DATASET_NAME*/download.sh *OUTPUT_DIRECTORY*</code>, like this:
```bash
supermarket/download.sh supermarket
```
The script will download the dataset and provide you a ready-to-use <code>*OUTPUT_DIRECTORY*/config.json</code> config file, that you can pass to the classifier app.  
You may want to edit it for better tuning of the parameters.
