    var phonecatApp = angular.module('testerApp', []);
     
    phonecatApp.controller('TesterCtrl', function ($scope, $http) {
   
       $scope.server = "https://195.65.191.10:9000";
       $scope.url = "";
       $scope.body = "";
       //$scope.extra = window.localStorage.extra;
       $scope.results = "empty";
       $scope.saved = [];
       
       //if ($scope.extra == null) $scope.extra = "";

       $scope.dosubmit = function() {
         var url = $scope.server + $scope.url;
         //window.localStorage.extra = $scope.extra;
         console.log(url);
         
         $http({ method: $scope.type, url: url, data : JSON.parse($scope.body) })
         .success(function(data) { $scope.results = JSON.stringify(data); })
         .error(function(x,p) { $scope.results = p + ":" + JSON.stringify(x); });
       };

       $scope.dosave = function() {
         $scope.saved.push({ url : $scope.url, body : $scope.body } );
         window.localStorage.urls = JSON.stringify($scope.saved);
       };

       $scope.doload = function(e) {
         $scope.url = e.url;
         $scope.body = e.body;
       };

       $scope.dodelete = function(e) {
         $scope.saved.splice($scope.saved.indexOf(e), 1);
         window.localStorage.urls = JSON.stringify($scope.saved);
       };

       if (window.localStorage.urls) {
         $scope.saved = JSON.parse(window.localStorage.urls);
         if (!$scope.saved) $scope.saved = [];
         console.log($scope.saved);
       };

    });