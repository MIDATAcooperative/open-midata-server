'use strict';

angular.module('timeSeriesApp')
  /**
   * Main controller for the time series visualization, it takes care of
   * the following functionality.
   * 1. Get the data with the authorization token.
   * 2. Provide the available datasets in a control that allows the user
   *    control over what is displayed.
   * 3. Attach the selected datasets to the time-series directive for display.
   * 4. Allow the removal of displayed datasets.
   * 5. Ensure that no more than 5 datasets are showed in the directive
   *    at the same time.
   */
  .controller('MainCtrl', ['$scope', '$routeParams', 'FitbitResources', '$http',
    function ($scope, $routeParams, FitbitResources, $http) {
      // Initialize the error variable
      $scope.error = {show: false};

      // Get the ids of the records assigned to this space
      var data = {authToken : $routeParams.authToken};
      $http.post("https://" + window.location.hostname +
        ":9000/v1/plugin_api/records/ids", JSON.stringify(data)).
        success(function(recordIds) {
          getRecords(recordIds);
        }).
        error(function(err) {
          $scope.error.show = true;
          $scope.error.msg = "Failed to load records: " + err;
        });

      // Get the records
      function getRecords(recordIds) {
        data.properties = {"_id": recordIds};
        data.fields = ["data"];
        $http.post("https://" + window.location.hostname +
          ":9000/v1/plugin_api/records/search", JSON.stringify(data)).
          success(function(records) {
            // only the data field is used
            records = _.compact(_.map(records, function(record) { return record.data; }));
            cleanRecords(records);
          }).
          error(function(err) {
            $scope.error.show = true;
            $scope.error.msg = "Failed to load records: " + err;
          });
      }

      // Clean the list of records according to the defined resources
      // in the fitbit service
      function cleanRecords(recordList) {
        var cleanList = _.filter(recordList, function(ele){
          return _.keys(ele).length === 1 &&
            _.find(FitbitResources.timeSeriesResourcesKeys, function findKey(val){
              return val.localeCompare(_.keys(ele)[0]) === 0;
            }) !== undefined;
        });
        if(cleanList.length === 0){
          $scope.error.show = true;
          $scope.error.msg = 'Did not find any valid records for the space.';
        } else {
          computeAvailableDataSets(cleanList);
        }
      }

      // Manipulate the records to adapt them to the directive's data format
      // this implies creating Javascript Date objects and ensuring
      // that the values are float
      function computeAvailableDataSets(cleanList) {
        // first, create an object with one list of records for each resource
        var resources = {};
        _.each(cleanList, function(record) {
          var dataKey = _.keys(record)[0];
          var recordData = record[dataKey][0];
          if (!(dataKey in resources)) {
            resources[dataKey] = {
              title: FitbitResources.timeSeriesResources[dataKey].title,
              units : FitbitResources.timeSeriesResources[dataKey].units,
              data: []
            };
          }
          resources[dataKey].data.push({
            datetime: new Date(recordData.dateTime),
            value: parseFloat(recordData.value)
          });
        });
        $scope.availableDataSets = _.values(resources);
        $scope.displayedDatasets = [];
        $scope.displayTimeSeries = false;
      }

      /**
       * UI control that determines when the display button can be clicked.
       * This is done to control issues with datasets already added
       * or when the number of displayed datasets is over 5.
       */
      function disableDisplayButton(){
        if($scope.selectedDataset){
          var found = _.find($scope.displayedDatasets, function(ele){
            return ele.title.localeCompare($scope.selectedDataset.title) === 0;
          });
          if(found !== undefined){
            $scope.disableDisplayButton = true;
          } else if($scope.displayedDatasets.length < 5){
            $scope.disableDisplayButton = false;
          } else if($scope.displayedDatasets.length > 5){
            $scope.disableDisplayButton = true;
          }
        } else {
          $scope.disableDisplayButton = true;
        }
      }
      $scope.$watch('selectedDataset', disableDisplayButton);

      /**
       * Display the selected dataset in the graph.
       */
      $scope.displayDataset = function displayDataset(){
        if($scope.selectedDataset && $scope.displayedDatasets.length < 5){
          $scope.displayedDatasets.push($scope.selectedDataset);
          disableDisplayButton();
          $scope.displayTimeSeries = true;
        }
      };

      /**
       * Remove a dataset from the graph.
       */
      $scope.hideDataset = function hideDataset(datasetIndex){
        $scope.displayedDatasets.splice(datasetIndex, 1);
        disableDisplayButton();
        if($scope.displayedDatasets.length === 0){
          $scope.displayTimeSeries = false;
        }
      };
    }]);
