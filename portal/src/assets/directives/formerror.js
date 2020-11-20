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
.directive('formerror', ['views', function (views) {
    return {
      template: function(element, attrs) {
    	  return '<p ng-show="myform.'+attrs.myid+'.$error.'+attrs.type+' && submitted" class="invalid-feedback" translate="'+attrs.message+'"></p>';
      },
      restrict: 'E',
      transclude: false,
      replace:true
    };
}])
.directive('formError', ['views', function (views) {
    return {
      template: function(element, attrs) {
    	  return '<div ng-show="myform.'+attrs.myid+'.$error.'+attrs.type+' && submitted" class="mi-or-login__error"><span class="mi-at-text mi-at-text--smallest mi-at-text--white mi-at-text--highlighted" translate="'+attrs.message+'"></span></div>';
      },
      restrict: 'E',
      transclude: false,
      replace:true
    };
}]);
