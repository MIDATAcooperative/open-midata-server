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

angular.module('portal').directive('noTime', ['$parse', function ($parse) {
    var directive = {
        restrict: 'A',
        require: ['ngModel'],
        link: link
    };
    return directive;

    function link(scope, element, attr, ctrls) {
        var ngModelController = ctrls[0];

       
        ngModelController.$parsers.push(function (viewValue) {
            
            viewValue.setMinutes(viewValue.getMinutes() - viewValue.getTimezoneOffset());
           
            return viewValue;
        });
    }
}]).directive('asDate', function() {
	  return {
	    require: 'ngModel',
	    link: function(scope, element, attrs, modelCtrl) {
	      modelCtrl.$formatters.push(function(input) {
	        var transformedInput;
	        if (input) transformedInput = new Date(input);
	        else transformedInput = new Date();
	        if (transformedInput !== input) {
	          modelCtrl.$setViewValue(transformedInput);
	          modelCtrl.$render();
	        }
	        return transformedInput;
	      });
	     }
	  };
});