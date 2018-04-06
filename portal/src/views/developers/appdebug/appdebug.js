angular.module('portal')
.controller('AppDebugCtrl', ['$scope', '$state', 'session', 'server', 'status', '$translatePartialLoader', 'apps', '$http', 'ENV', function($scope, $state, session, server, status, $translatePartialLoader, apps, $http, ENV) {
	
	
	
	   $scope.server = ENV.apiurl;
       $scope.url = "/v1/auth";
       

       $scope.type = "POST";
       $scope.authheader = "";
       //$scope.extra = window.localStorage.extra;
       $scope.results = "empty";
       $scope.count = 1;
       $scope.saved = [];
       
       //if ($scope.extra == null) $scope.extra = "";

       $scope.dosubmit = function() {
         var url = $scope.server + $scope.url;
         
       
         
         var body = "";
         
         if ($scope.type !== "GET") {
         try {
        	 body = JSON.parse($scope.body);
         } catch (e) {
        	 $scope.results = "Error processing body: \n"+e;
        	 return;
         }
         }
         
         var call = { method: $scope.type, url: url, data : body }; 
         if ($scope.authheader) call.headers = { "Authorization" : $scope.authheader, "Prefer" : "return=representation" };

         $scope.results = "Processing request...";
        
         
         //for (var count = 0;count < Number($scope.count); count++) {
        //	 console.log(count);
	         $http(call)
	         .then(function(data1) {
	        	 var data  = data1.data;
	        	 if ($scope.url === "/v1/auth" && data.authToken) {
	        		 $scope.authheader = "Bearer " + data.authToken;
	        	 } 
	        	 $scope.results = JSON.stringify(data, null, 2); 
	          }, function(x,p) { $scope.results = p + ":" + JSON.stringify(x, null, 2); });
         //}
       };

       $scope.dosave = function() {
         $scope.saved.push({ type : $scope.type, url : $scope.url, body : ($scope.type != "GET" ? $scope.body : "") } );
         window.localStorage.urls = angular.toJson($scope.saved);
       };

       $scope.doload = function(e) {
         $scope.url = e.url;
         $scope.body = e.body;
         $scope.type = e.type;
       };

       $scope.dodelete = function(e) {
         $scope.saved.splice($scope.saved.indexOf(e), 1);
         window.localStorage.urls = JSON.stringify($scope.saved);
       };

       if (window.localStorage.urls) {
         $scope.saved = angular.fromJson(window.localStorage.urls);
         if (!$scope.saved) $scope.saved = [];
         angular.forEach($scope.saved, function(s) { s.$$hashKey = undefined; });
         console.log($scope.saved);
       }

	
	
	
	$scope.status = new status(true);    
	$scope.calls = [];
			
	$scope.init = function(userId, appId) {
		$scope.userId = userId;
		$scope.appId = appId;
			    
	    $scope.status.doBusy(apps.getApps({ "_id" : appId }, ["filename", "name", "targetUserRole", "secret"]))
		.then(function(data) { 
			$scope.app = data.data[0];
			
			$scope.body = "{\n    \"appname\":\""+$scope.app.filename+"\",\n    \"device\" : \"debug\",\n    \"secret\" : \""+$scope.app.secret+"\",\n    \"username\" : \"FILLOUT\",\n    \"password\" : \"FILLOUT\",\n    \"role\" : \""+$scope.app.targetUserRole+"\"\n}";
		});
	};
	
	$scope.reload = function() {
		$scope.init($scope.userId, $scope.appId);
	};
	
		
	$translatePartialLoader.addPart("developers");
	
	session.currentUser.then(function(userId) { $scope.init(userId, $state.params.appId); });	
}]);