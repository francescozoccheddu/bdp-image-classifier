# Run the classifier on a local [Spark](https://spark.apache.org/) installation


1. Setup a local [Spark](https://spark.apache.org/) installation by running

    <pre lang="bash">bdp-ic-debug-env-install</pre>
    
    > **NOTE:**   
    > Only x86 and x64 Linux operating systems are currently supported.

2. Build the classifier app by running 

    <pre lang="bash">bdp-ic-app-assembly <i>PROJECT_DIRECTORY</i> -o assembly.jar</pre>
    
    or download a prebuilt JAR by running
    
    <pre lang="bash">bdp-ic-app-download <i>ARCHITECTURE</i> -o assembly.jar</pre>

3. Download a preconfigured dataset by running
    
    <pre lang="bash">bdp-ic-datasets-download <i>DATASET_NAME</i> -o dataset</pre>
    
    or get a dataset of your choice and create a custom JSON configuration by yourself.
    
    > **NOTE:**   
    > See [DATASETS.md](DATASETS.md) to choose a valid <code><i>DATASET_NAME</i></code>.

4. Run
    
    <pre lang="bash">bdp-ic-debug-env assembly.jar dataset</pre>
    
    > **NOTE:**   
    > You may want to change the default memory amount for the driver and the executor nodes.  
    > Use the `--help` flag for help.