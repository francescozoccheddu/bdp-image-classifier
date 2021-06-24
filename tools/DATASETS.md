# Preconfigured test datasets

Here is a list of preconfigured datasets for testing purposes:

| Name               | Size  | Download size | Images | Classes | Source                                                         |
| :----------------: | :---: | :-----------: | :----: | :-----: | :------------------------------------------------------------: |
| test               | 12MB  | 275MB         | 555    | 2       | https://iplab.dmi.unict.it/MLC2018/                            |
| supermarket        | 275MB | 275MB         | 13360  | 16      | https://iplab.dmi.unict.it/MLC2018/                            |
| land               | 1.1GB | 2.0GB         | 10500  | 21      | https://www.kaggle.com/apollo2506/landuse-scene-classification |
| indoor             | 2.6GB | 2.6GB         | 15613  | 67      | https://www.kaggle.com/itsahmad/indoor-scenes-cvpr-2019        |

You can download them by running

<pre lang="bash">
bdp-ic-datasets-download <i>DATASET_NAME</i>
</pre>

> **NOTE:**  
> You need to provide a set of valid credentials to download [Kaggle](https://www.kaggle.com/) datasets.   
> See [Kaggle API credentials](https://github.com/Kaggle/kaggle-api#api-credentials) for help.