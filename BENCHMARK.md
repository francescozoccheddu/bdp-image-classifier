# Benchmark results

`land` dataset (see [tools/DATASETS.md](tools/DATASETS.md)) on `m5.xlarge` [EC2](https://aws.amazon.com/ec2/) instances.

Tested by running

<pre lang="bash">
bdp-ic-emr-run-test land -m S3 -c <i>INSTANCES_COUNT</i> -t m5.xlarge
</pre>

| Instances count | Total time | Processing time\* | Speed-up | Gain      |
| :-------------: | :--------: | :---------------: | :------: | :-------: |
| 1               | 38:24      | 37:08             | 1.00x    | 100.00%   |
| 2               | 21:05      | 19:46             | 1.88x    | 94.00%    |
| 3               | 15:06      | 13:47             | 2.70x    | 90.00%    |
| 5               | 11:16      | 09:53             | 3.77x    | 75.40%    |

\* The processing time does not include the time spent in merging the input images, as this step would only be executed once and without the need of a cluster in a real use case.  