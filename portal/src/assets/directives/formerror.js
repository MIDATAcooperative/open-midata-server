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
