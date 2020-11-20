/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('portal')
.directive('accessquery', ['views', function (views) {
    return {
      templateUrl: 'assets/directives/accessquery.html',
      restrict: 'E',
      transclude: false, 
      scope : {
    	"query" : "=",
    	"details" : "@",
    	"isapp" : "@"
      },
      controller : ['$scope', '$translate', 'labels', function($scope, $translate, labels) {
    	  
        $scope.$watch('query', function() {
        	if ($scope.query) {
              $scope.blocks = labels.parseAccessQuery($translate.use(), $scope.query);
        	}
        });
      }]
    };
}]);
