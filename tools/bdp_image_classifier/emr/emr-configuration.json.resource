[
  {
    "Classification": "hdfs-site",
    "Properties": {
      "dfs.block.size": "33554432"
    }
  },
  {
    "Classification": "spark-log4j",
    "Properties": {
      "log4j.rootCategory": "ERROR, console",
      "log4j.appender.console": "org.apache.log4j.ConsoleAppender",
      "log4j.appender.console.target": "System.err",
      "log4j.appender.console.layout": "org.apache.log4j.PatternLayout",
      "log4j.appender.console.layout.ConversionPattern": "%d{HH:mm:ss} %c{1}: %m%n",
      "log4j.logger.com.github.francescozoccheddu.bdp_image_classifier": "INFO"
    }
  }
]
