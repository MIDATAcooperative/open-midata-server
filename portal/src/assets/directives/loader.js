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
.directive('loader', ['views', function (views) {
    return {
      template: function(element, attrs) {
    	  return '<div><div ng-show="'+attrs.busy+'" class="loader"><div><img  src="/images/ajax-loader.gif"></div></div><div class="ng-hide" ng-hide="'+attrs.busy+'" ng-transclude></div></div>';
      },
      restrict: 'E',
      transclude: true,
      replace:true  
    };
}]);