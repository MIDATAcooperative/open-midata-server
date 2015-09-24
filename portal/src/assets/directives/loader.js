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