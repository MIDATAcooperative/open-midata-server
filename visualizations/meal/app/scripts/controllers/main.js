'use strict';

angular.module('mealApp')
  .controller('MainCtrl', ['$scope', '$routeParams', '$http', 'mealJawboneService',
    function ($scope, $routeParams, $http, mealJawboneService) {
        $scope.error = false;
        $scope.errorMessage = '';

        // get the ids of the records assigned to this space
        var data = {"authToken": $routeParams.authToken};
        $http.post("https://" + window.location.hostname +
            ":9000/v1/plugin_api/records/ids", JSON.stringify(data)).
            success(function(recordIds) {
                getRecords(recordIds);
            }).
            error(function(err) {
                $scope.error = true;
                $scope.errorMessage = "Failed to load records: " + err;
            });

        // get the records
        function getRecords(recordIds) {
            data.properties = {"_id": recordIds};
            data.fields = ["data"];
            $http.post("https://" + window.location.hostname +
                ":9000/v1/plugin_api/records/search", JSON.stringify(data)).
                success(function(records) {
                    buildDataStructures(records);
                }).
                error(function(err) {
                    $scope.error = true;
                    $scope.errorMessage = "Failed to load records: " + err;
                });
        }

        // builds the data structures for the clock directive
        function buildDataStructures(recordList) {
            /**
             * Brings the records into the required format, with the fields
             * type: 'mealList'
             * date: date without time (format: YYYY-MM-dd)
             * data: record data
             */
            var jawboneRecordList =
                _.compact(_.map(recordList, mealJawboneService.preprocessRecord));
            var cleanRecordList = _.union(jawboneRecordList);

            if (cleanRecordList.length === 0) {
                $scope.error = true;
                $scope.errorMessage = 'Sorry, we cannot find any meal record';
            } else {
                // Sort and group records so that the lastest group comes first
                cleanRecordList = _.sortBy(cleanRecordList, function (element) {
                    return element.date.getTime();
                }).reverse();

                var dateList = [];

                cleanRecordList.forEach(function (element) {
                    dateList.push(element.date);
                });

                $scope.recordList = cleanRecordList;
                $scope.selectedDateIndex = 0;
                $scope.dateList = dateList;
                $scope.mealList = $scope.recordList[$scope.selectedDateIndex];

                $scope.$watch('selectedDateIndex', function (newValue) {
                    $scope.mealList = $scope.recordList[newValue];
                    console.log($scope.mealList);
                });
            }
        }
    }]);
