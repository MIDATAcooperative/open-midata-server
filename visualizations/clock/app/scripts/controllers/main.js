'use strict';

angular.module('clockApp')
    /**
     * Main controller for the clock visualization, it implements the following
     * functions:
     * 1. Read the data from the URL parameters.
     * 2. Parse the data as JSON objects and filter/group it according
     *    to the expected format by the directive.
     * 3. Provide UI functionality to select the date of the data to display.
     * 4. Supply the selected data to the directive.
     */
    .controller('MainCtrl', ['$scope', '$routeParams', '$http', 'clockJawboneService',
        function ($scope, $routeParams, $http, clockJawboneService) {
            $scope.error = false;
            $scope.errorMessage = '';

            // get the ids of the records assigned to this space
            var data = {"authToken": $routeParams.authToken};
            $http.post("https://" + window.location.hostname +
                ":9000/api/visualizations/ids", JSON.stringify(data)).
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
                data.fields = ["data", "data.data"];
                $http.post("https://" + window.location.hostname +
                    ":9000/api/visualizations/records", JSON.stringify(data)).
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
                 * Brings the records into the required format, with the top-level
                 * type: one of 'moveList', 'moveTicks', 'sleepList', and 'sleepTicks'
                 * date: date without time
                 * data: record data
                 */
                var jawboneRecordList =
                    _.compact(_.map(recordList, clockJawboneService.preprocessRecord));
                var cleanRecordList = _.union(jawboneRecordList);

                if (cleanRecordList.length === 0) {
                    $scope.error = true;
                    $scope.errorMessage =
                        'Sorry, we cannot find any move or sleep event';
                } else {
                    // Sort and group records so that the lastest group comes first
                    cleanRecordList = _.sortBy(cleanRecordList, function (element) {
                        return element.date.getTime();
                    }).reverse();

                    var recordGroupList = [],
                        dateList = [],
                        currentDate = cleanRecordList[0].date,
                        recordGroup = {
                            moveList: undefined,
                            moveTicks: undefined,
                            sleepList: undefined,
                            sleepTicks: undefined
                        };
                    cleanRecordList.forEach(function (element) {
                        if (currentDate - element.date === 0) {
                            recordGroup[element.type] = element;
                        } else {
                            recordGroupList.push(recordGroup);
                            dateList.push(currentDate);

                            currentDate = element.date;
                            recordGroup = {
                                moveList: undefined,
                                moveTicks: undefined,
                                sleepList: undefined,
                                sleepTicks: undefined
                            };
                            recordGroup[element.type] = element;
                        }
                    });

                    dateList.push(currentDate);
                    recordGroupList.push(recordGroup);

                    $scope.recordGroupList = recordGroupList;
                    $scope.selectedDateIndex = 0;
                    $scope.dateList = dateList;
                    $scope.moveList =
                        $scope.recordGroupList[$scope.selectedDateIndex].moveList;
                    $scope.moveTicks =
                        $scope.recordGroupList[$scope.selectedDateIndex].moveTicks;
                    $scope.sleepList =
                        $scope.recordGroupList[$scope.selectedDateIndex].sleepList;
                    $scope.sleepTicks =
                        $scope.recordGroupList[$scope.selectedDateIndex].sleepTicks;

                    $scope.$watch('selectedDateIndex', function (newValue) {
                        $scope.moveList =
                            $scope.recordGroupList[newValue].moveList;
                        $scope.moveTicks =
                            $scope.recordGroupList[newValue].moveTicks;
                        $scope.sleepList =
                            $scope.recordGroupList[newValue].sleepList;
                        $scope.sleepTicks =
                            $scope.recordGroupList[newValue].sleepTicks;
                    });
                }
            }
        }]);