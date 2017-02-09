angular.module('services')
.factory('oauth', ['server', function(server) {
	var service = {};
	var cred = {};
	
	service.init = function(client_id, redirect_uri, state) {
	   cred.appname = client_id;
	   cred.redirectUri = redirect_uri;
	   cred.state = state || "none";
	};
	
	service.setUser = function(email, password, role) {
	   cred.username = email;
	   cred.password = password;
	   cred.role = role || "MEMBER";
	};
	
	service.getAppname =function() {
		return cred.appname;
	};
	
	service.login = function() {	    			
		return server.post("/v1/authorize", JSON.stringify(cred)).
		then(function(result) {	
			cred.appname = null;
			document.location.href = cred.redirectUri + "?state=" + encodeURIComponent(cred.state) + "&code=" + result.data.code;			
		});
	};
	
	return service;
}]);