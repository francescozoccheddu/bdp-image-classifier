{
  "data": {
    "make": {
      "dataSet": [["./images/0/*.jpg"], ["./images/1/*.jpg"]],
      "tempFile": "hdfs:///image-classifier/data",
      "testSample": {
        "maxFractionPerClass": 0.2
      }
    }
  },
  "featurization": {
    "make": {
      "descriptor": {
        "algorithm": "sift",
        "maxFeatureCount": 100,
        "minFeatureCount": 20
      },
      "codebook": {
        "size": 200,
        "assignNearest": true,
        "imageSample": {
          "maxCountPerClass": 100
        },
        "featureSample": {
          "maxCountPerClass": 5000
        }
      }
    }
  },
  "training": {
    "save": "hdfs:///image-classifier/model",
    "make": {
      "algorithm": "nn"
    }
  },
  "testing": {
    "save": "hdfs:///image-classifier/summary"
  }
}
