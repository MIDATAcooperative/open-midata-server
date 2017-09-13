'use strict';

angular.module('waterMeterApp')
  /**
   * Main controller for the visualization. It implements the following
   * functionality.
   * 1. Configure a date picker to select the day to display.
   * 2. Load an animation with the water glass directive based on the
   *    water consumption data for the selected day.
   */
  .controller('MainCtrl', ['$scope', '$interval', '$routeParams', '$http',
    function ($scope, $interval, $routeParams, $http) {
      // Initialize basic elements for the controller
      $scope.error = {show : false};
      var recommendedWaterConsumption = 2000;
      var fillingGlassesPromise = null;
      $scope.waterGlasses = [[], []];

      /**
       * React to date changes by modifying the message displayed in the main
       * jumbotron in the HTML template and initiate the animation with the
       * glasses filled up depending on the day water value.
       */
      $scope.dateChanged = function dateChanged(){
        $scope.noDataInDate = false;
        $scope.goalAccomplished = false;
        $scope.goalUnaccomplished = false;
        if(true || $scope.dateForm.inputDate.$valid){
          var currentWaterLevel = 0;
          if(_.has($scope.waterData, $scope.selectedDate)){
            var dayWater = $scope.waterData[$scope.selectedDate];
            if(dayWater.summary.water >= recommendedWaterConsumption){
              $scope.goalAccomplished = true;
            } else {
              $scope.goalUnaccomplished = true;
            }
            currentWaterLevel = dayWater.summary.water;
          } else {
            $scope.noDataInDate = true;
          }
          $scope.waterGlasses = [[], []];
          if(fillingGlassesPromise !== null){
            $interval.cancel(fillingGlassesPromise);
          }
          fillingGlassesPromise = $interval(function fillGlasses(){
            $scope.waterGlasses[$scope.waterGlasses[0].length === 5 ? 1 : 0]
              .push({
                value: currentWaterLevel > 200 ? 200 : currentWaterLevel
              });
            currentWaterLevel -= currentWaterLevel > 200 ? 200 :
              currentWaterLevel;
          }, 100, 10);
        }
      };

      function preprocessRecords(records) {
        // filter out records that are not fitbit water consumption records
        var filteredRecords = _.filter(records, function(record) {
          return _.has(record, "data") && _.has(record.data, "water") && record.data.water.length > 0;
        });

        // check if we have any records left
        if(filteredRecords.length === 0){
          $scope.error.show = true;
          $scope.error.message = 'Did not find any water records in this space.';
        } else {
          // create a list of dates and of record data
          var dates = _.map(filteredRecords, function(record) { return record.name.slice(-10); });
          var data = _.map(filteredRecords, function(record) { return record.data; });
          // waterData is an object with dates as keys and data as values
          $scope.waterData = _.object(dates, data);
          
          dates.sort();
          console.log(dates);          
          var newestDate = dates[dates.length - 1];
          console.log(newestDate);
          $scope.selectedDate = newestDate;
          $scope.dateChanged();
          
        }
      }

      // get the records
      function getRecords(recordIds) {
        data.properties = {"_id": recordIds};
        data.fields = ["name", "data"];
        $http.post("https://" + window.location.hostname +
          ":9000/v1/plugin_api/records/search", JSON.stringify(data)).
          success(function(records) {
            preprocessRecords(records);
          }).
          error(function(err) {
            $scope.error.show = true;
            $scope.error.message = "Failed to load records: " + err;
          });
      }

      // get the ids of the records assigned to this space
      var data = {authToken : $routeParams.authToken};
      $http.post("https://" + window.location.hostname +
        ":9000/v1/plugin_api/records/ids", JSON.stringify(data)).
        success(function(recordIds) {
          getRecords(recordIds);
        }).
        error(function(err) {
          $scope.error.show = true;
          $scope.error.message = "Failed to load records: " + err;
        });

    }]);
