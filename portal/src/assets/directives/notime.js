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
}]);