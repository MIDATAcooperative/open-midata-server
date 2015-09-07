angular.module('portal')
.directive('formerror', ['views', function (views) {
    return {
      template: function(element, attrs) {
    	  return '<p ng-show="myform.'+attrs.myid+'.$error.'+attrs.type+' && submitted" class="help-block">' +
    	  attrs.message + '</p>';
      },
      restrict: 'E',
      transclude: false,
      replace:true
    };
}]);
