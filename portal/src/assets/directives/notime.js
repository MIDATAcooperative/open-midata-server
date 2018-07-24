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