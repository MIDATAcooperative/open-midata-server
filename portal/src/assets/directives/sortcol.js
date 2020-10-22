/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('portal')
.directive('sortby', [function () {
    return {          	          
      template : function(element, attrs) {
    	  return '<th ng-click="setSort(\''+attrs.sortby+'\');" class="clickable sort" ng-class="{\'asc\':sortby==\''+attrs.sortby+'\' ,\'desc\':sortby==\'-'+attrs.sortby+'\' }" ng-transclude></th>';
      },
      restrict: 'A',
      transclude : true,
      replace : true
    };
}]);