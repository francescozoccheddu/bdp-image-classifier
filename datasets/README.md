# Datasets

## List of supported datasets

Here is a list of datasets that you can try:

| Name               | Size  | Images | Classes | Source                                                         |
| :----------------: | :---: | :----: | :-----: | :------------------------------------------------------------: |
| supermarket        | 275MB | 13360  | 16      | https://iplab.dmi.unict.it/MLC2018/                            |
| land               | 1.1GB | 10500  | 21      | https://www.kaggle.com/apollo2506/landuse-scene-classification |
| indoor             | 2.6GB | 15613  | 67      | https://www.kaggle.com/itsahmad/indoor-scenes-cvpr-2019        |

## Downloading a dataset

Run <code>*DATASET_NAME*/download.sh *OUTPUT_DIR*</code>.

The script will download the dataset and provide you a ready-to-use <code>*OUTPUT_DIR*/config.json</code> config file, that you can pass to the classifier app.
You may want to edit it for better tuning of the parameters.
