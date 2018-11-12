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
       $scope.device = "debug";
       
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
	          }, function(x) { $scope.results = x.status + ":" + JSON.stringify(x.data, null, 2); });
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
		
	
	var requestAccessToken = function(code) {
		
	    console.log("CODE: "+code);
	    var body = "grant_type=authorization_code&redirect_uri=x&client_id="+encodeURIComponent($scope.app.filename)+"&code="+encodeURIComponent(code);	    
	    $http.post(ENV.apiurl+"/v1/token", body, { headers : { 'Content-Type': 'application/x-www-form-urlencoded' } }).then(function(result) {
		   console.log(result.data);
		   
		   $scope.authheader = "Bearer " + result.data.access_token;
		   $scope.refreshToken = result.data.refresh_token;
		   
		}); 
	};
	
	onAuthorized = function(url) {

		var message = null;
		var error = null;

		var arguments1 = url.split("&");
		var keys = _.map(arguments1, function(argument) { return argument.split("=")[0]; });
		var values = _.map(arguments1, function(argument) { return argument.split("=")[1]; });
		var params = _.object(keys, values);
		
		if (_.has(params, "error")) {
			error = "The following error occurred: " + params.error + ". Please try again.";
		} else if (_.has(params, "code")) {
			message = "User authorization granted. Requesting access token...";
			requestAccessToken(params.code);
		} else if (_.has(params, "oauth_verifier")) {
			message = "User authorization granted. Requesting access token...";
			requestAccessToken(params.oauth_verifier, params);
		} else {
			error = "An unknown error occured while requesting authorization. Please try again.";
		}
 
		
		
		$scope.message = message;
		$scope.error = error;
		if (error) {
			$scope.authorizing = false;
		}
	
		//authWindow.close();
	};	
	
	
	$scope.init = function(userId, appId) {
		$scope.userId = userId;
		$scope.appId = appId;
			    
	    $scope.status.doBusy(apps.getApps({ "_id" : appId }, ["filename", "name", "targetUserRole", "secret", "redirectUri"]))
		.then(function(data) { 
			$scope.app = data.data[0];
			//sessionStorage.returnTo = document.location.href;
			//$scope.body = "{\n    \"appname\":\""+$scope.app.filename+"\",\n    \"device\" : \"debug\",\n    \"secret\" : \""+$scope.app.secret+"\",\n    \"username\" : \"FILLOUT\",\n    \"password\" : \"FILLOUT\",\n    \"role\" : \""+$scope.app.targetUserRole+"\"\n}";
			
			if ($state.params.code) {
				requestAccessToken($state.params.code);
			}
		});
	};
	
	$scope.reload = function() {
		$scope.init($scope.userId, $scope.appId);
	};
	
	$scope.getOAuthLogin = function() {
		if (!$scope.app || !$scope.app.redirectUri) return "";
		var back = document.location.href;				
		if (back.indexOf("?") > 0) back = back.substr(0, back.indexOf("?"));
		return "/oauth.html#/portal/oauth2?response_type=code&client_id="+encodeURIComponent($scope.app.filename)+"&redirect_uri="+encodeURIComponent(back)+"&device_id="+encodeURIComponent($scope.device);
	};
		
	$translatePartialLoader.addPart("developers");
	
	session.currentUser.then(function(userId) { $scope.init(userId, $state.params.appId); });	
}]);