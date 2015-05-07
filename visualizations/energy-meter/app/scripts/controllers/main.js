'use strict';

angular.module('energyMeterApp')
  /**
   * Main controller for the visualization, it implements the following
   * functionality:
   * 1. Get the records with the authorization token passed through the
   *    URL, filter out all but the Jawbone workouts and preprocess them.
   * 2. Implement the functionality for picking weeks using the HTML5 input
   *    element.
   * 3. Respond to changes in the selected week and display the weekly
   *    data in the map and the summary div.
   */
  .controller('MainCtrl', ['$scope', '$routeParams', '$filter',
    'JawboneWorkout', '$http',
    function ($scope, $routeParams, $filter, JawboneWorkout, $http) {
      // Initialize basic variables for the display
      $scope.mapParams = {
        center : {
          latitude : 47.3667,
          longitude : 8.5500
        },
        zoom : 11
      };

      $scope.weekDays = [
        {name : 'Sunday', colorClass : 'sunday'},
        {name : 'Monday', colorClass : 'monday'},
        {name : 'Tuesday', colorClass : 'tuesday'},
        {name : 'Wednesday', colorClass : 'wednesday'},
        {name : 'Thursday', colorClass : 'thursday'},
        {name : 'Friday', colorClass : 'friday'},
        {name : 'Saturday', colorClass : 'saturday'}
      ];

      $scope.error = {show : false};

      // Define functionality regarding the info window for each workout in
      // the map...
      /**
       * Reset the info window object.
       */
      function resetInfoWindow(){
        $scope.infoWindow = {
          coords : {
            latitude : 0,
            longitude : 0
          },
          show : false,
          workout : null
        };
      }

      // ... and initialize it
      resetInfoWindow();

      /**
       * Show the info window when a marker is clicked. This calls $apply
       * because the click is triggered outside AngularJS.
       */
      function showInfoWindow($markerModel){
        $scope.infoWindow.show = true;
        $scope.infoWindow.coords.latitude = $markerModel.latitude;
        $scope.infoWindow.coords.longitude = $markerModel.longitude;
        $scope.infoWindow.workout = $markerModel.workout;
        $scope.$apply();
      }
      /**
       * Close the info window by resetting the info window object.
       * This calls $apply when called from a click in the map (i.e. outside an
       * AngularJS event).
       */
      $scope.closeInfoWindow = function(doApply){
        resetInfoWindow();
        if(doApply){
          $scope.$apply();
        }
      }

      // Functionality for legend collapse
      $scope.legendCollapsed = false;
      /**
       * Collapse/expand the legend when the title is clicked.
       */
      $scope.collapseLegend = function(){
        $scope.legendCollapsed = !$scope.legendCollapsed;
      }

      /**
       * Function that responds to changes in the selected week.
       * This checks if there is data for the given week and if then it
       * produces the marker models and summary object for display
       * in the view.
       */
      $scope.changedWeekSelection = function(){
        if($scope.dateForm.dateInput.$valid){
          $scope.closeInfoWindow(false);
          var weekNumber = $filter('date')($scope.selectedWeek, 'yyyy-Www');
          $scope.selectedWeekStart = new Date($scope.selectedWeek.getFullYear(),
            $scope.selectedWeek.getMonth(),
            $scope.selectedWeek.getDate() - $scope.selectedWeek.getDay());
          $scope.selectedWeekEnd = new Date($scope.selectedWeek.getFullYear(),
            $scope.selectedWeek.getMonth(),
            $scope.selectedWeek.getDate() - $scope.selectedWeek.getDay() + 6);
          if(_.has($scope.availableData, weekNumber)){
            $scope.dataPresent = true;
            $scope.weeklyMarkers = JawboneWorkout.generateMarkers(
              $scope.availableData[weekNumber]);

            // define the onClick function here to access the showInfoWindow function
            _.each($scope.weeklyMarkers, function(marker) {
              marker.onClick = function() {
                showInfoWindow(marker);
              }
            });
            $scope.summary = JawboneWorkout.generateSummary(
              $scope.availableData[weekNumber]);
            $scope.summary.ready = true;
            $scope.presentWeekDays = _.filter($scope.weekDays,
              function isDayPresent(day, idx){
                var found = _.find($scope.availableData[weekNumber],
                  function findDayInData(element){
                    return element.date.getDay() === idx;
                  });
                return found !== undefined;
              });
          } else {
            $scope.dataPresent = false;
            $scope.weeklyMarkers = [];
            $scope.presentWeekDays = [];
            $scope.summary = {ready: false};
          }
        }
      }

      // filter out records that are not jawbone workouts and bring them
      // into the desired form
      function preprocessRecords(records) {
        var filteredRecords = _.compact(_.map(records, JawboneWorkout.preprocessRecord));
        var sortedRecords = _.sortBy(filteredRecords, 'data');
        $scope.availableData = _.groupBy(sortedRecords,
          function getWeekNumber(element){
            return $filter('date')(element.date, 'yyyy-Www');
          });
        if(_.keys($scope.availableData).length === 0){
          $scope.error.show = true;
          $scope.error.message = 'Did not find any workout records in this space.';
        }
      }

      // get the records
      function getRecords(recordIds) {
          data.properties = {"_id": recordIds};
          data.fields = ["data", "data.data"];
          $http.post("https://" + window.location.hostname +
              ":9000/api/visualizations/records", JSON.stringify(data)).
              success(function(records) {
                preprocessRecords(records);
              }).
              error(function(err) {
                  $scope.error.show = true;
                  $scope.error.message = "Failed to load records: " + err;
              });
      }

      // get the ids of the records assigned to this space
      var data = {"authToken": $routeParams.authToken};
      $http.post("https://" + window.location.hostname +
          ":9000/api/visualizations/ids", JSON.stringify(data)).
          success(function(recordIds) {
              getRecords(recordIds);
          }).
          error(function(err) {
              $scope.error.show = true;
              $scope.error.message = "Failed to load records: " + err;
          });

    }]);
