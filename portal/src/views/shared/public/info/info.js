angular.module('portal')
.controller('InfoCtrl', ['$scope', 'ENV', function($scope, ENV) {
  $scope.ENV = ENV;
}]);