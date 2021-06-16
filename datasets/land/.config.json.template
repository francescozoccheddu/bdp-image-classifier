{
  "data": {
    "make": {
      "dataSet": [
        ["./images/agricultural/*.png"],
        ["./images/airplane/*.png"],
        ["./images/baseballdiamond/*.png"],
        ["./images/beach/*.png"],
        ["./images/buildings/*.png"],
        ["./images/chaparral/*.png"],
        ["./images/denseresidential/*.png"],
        ["./images/forest/*.png"],
        ["./images/freeway/*.png"],
        ["./images/golfcourse/*.png"],
        ["./images/harbor/*.png"],
        ["./images/intersection/*.png"],
        ["./images/mediumresidential/*.png"],
        ["./images/mobilehomepark/*.png"],
        ["./images/overpass/*.png"],
        ["./images/parkinglot/*.png"],
        ["./images/river/*.png"],
        ["./images/runway/*.png"],
        ["./images/sparseresidential/*.png"],
        ["./images/storagetanks/*.png"],
        ["./images/tenniscourt/*.png"]
      ],
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
        "maxFeatureCount": 200,
        "minFeatureCount": 50
      },
      "codebook": {
        "size": 700,
		"maxIterations": 20,
		"initSteps": 3,
        "assignNearest": true
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
	"save": "hdfs:///image-classifier/summary",
    "labels": [
      "Agricultural",
      "Airplane",
      "Baseball diamond",
      "Beach",
      "Buildings",
      "Chaparral",
      "Dense residential",
      "Forest",
      "Freeway",
      "Golf course",
      "Harbor",
      "Intersection",
      "Medium residential",
      "Mobile home park",
      "Overpass",
      "Parking lot",
      "River",
      "Runway",
      "Sparse residential",
      "Storage tanks",
      "Tennis court"
    ]
  }
}
