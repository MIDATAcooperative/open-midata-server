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
