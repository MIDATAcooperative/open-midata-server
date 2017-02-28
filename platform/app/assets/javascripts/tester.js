    var phonecatApp = angular.module('testerApp', []);
     
    phonecatApp.controller('TesterCtrl', function ($scope, $http) {
   
       $scope.server = "https://"+document.location.host;
       $scope.url = "";
       $scope.body = "{}";
       $scope.type = "POST";
       $scope.authheader = "";
       //$scope.extra = window.localStorage.extra;
       $scope.results = "empty";
       $scope.count = 1;
       $scope.saved = [];
       
       //if ($scope.extra == null) $scope.extra = "";

       $scope.dosubmit = function() {
         var url = $scope.server + $scope.url;
         
         console.log(url);
         
         var body = "";
         try {
        	 body = JSON.parse($scope.body);
         } catch (e) {
        	 $scope.results = "Error processing body: \n"+e;
        	 return;
         }
         
         var call = { method: $scope.type, url: url, data : body }; 
         if ($scope.authheader) call.headers = { "Authorization" : $scope.authheader };

         $scope.results = "";
         console.log("count="+$scope.count);
         
         for (var count = 0;count < Number($scope.count); count++) {
        	 console.log(count);
	         $http(call)
	         .success(function(data) { $scope.results += JSON.stringify(data, null, 2); })
	         .error(function(x,p) { $scope.results = p + ":" + JSON.stringify(x, null, 2); });
         }
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
    
    phonecatApp.controller('ApsTestCtrl', function ($scope, $http) {
    	   
        $scope.server = "https://localhost:9000";
        $scope.aps = "-";
        $scope.token = "";
        
        $scope.dosubmit = function() {
            var url = $scope.server + "/debug/aps/"+$scope.aps;
            //window.localStorage.extra = $scope.extra;
            console.log(url);            
            $http({ method: "GET", url: url, headers : { "X-Session-Token" : $scope.token }  })
            .success(function(data) { $scope.data = data; })
            .error(function(x,p) { $scope.data = JSON.stringify(x); });
          };
    });