angular.module('services')
.factory('oauth', ['server', '$cookies', 'session', 'crypto', function(server, $cookies, session, crypto) {
	var service = {};
	var cred = {};
	
	var randomString = function() {
		var charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIHJKLMNOPQRSTUVWXYZ_01234567890";
		var mycrypto = window.crypto || window.msCrypto || {
			getRandomValues : function(vals) {
				for (var i = 0; i < vals.length; i++) vals[i] = Math.floor(Math.random()*16000);
			} 
		};
		            		       
		var values = new Uint32Array(20);
		mycrypto.getRandomValues(values);

		var result = "";
		for (var i = 0; i < 20; i++) {
		   result += charset[values[i] % charset.length];
		}
		
		return result;
	};
	
	var getDeviceId = function() {
		var devid;
		if (localStorage && localStorage.deviceId) {
		   devid = localStorage.deviceId;
		}
		if (!devid) {
			devid = $cookies.get("device");
		}
		if (!devid) {
			devid = randomString();						
		}
		if (localStorage) localStorage.deviceId = devid;
		$cookies.put("device", devid);
		
		return devid;
	};
	
	service.createDeviceId = getDeviceId;
	
	service.init = function(client_id, redirect_uri, state, code_challenge, code_challenge_method, devId) {
	   cred.appname = client_id;
	   cred.redirectUri = redirect_uri;
	   cred.state = state || "none";
	   cred.code_challenge = code_challenge;
	   cred.code_challenge_method = code_challenge_method;
	   cred.device = devId || getDeviceId();
	};
	
	service.setUser = function(email, password, role, studyLink) {
	   cred.username = email;
	   cred.password = password;
	   cred.studyLink = studyLink;
	   cred.role = role || "MEMBER";
	};
	
	service.setUnlockCode = function(code) {
		console.log("set:"+code);
		cred.unlockCode = code;
	};
	
	service.setSecurityToken = function(token) {
		cred.securityToken = token;
	};
	
	service.getAppname =function() {
		return cred.appname;
	};
	
	service.getDevice =function() {
		return cred.device;
	};
	
	service.getDeviceShort =function() {
		return cred.device.substr(0,3);
	};
	
	service.login = function(confirm, confirmStudy) {	    	
		cred.confirm = confirm || false;
		cred.confirmStudy = confirmStudy || (confirm && cred.confirmStudy);
				
		var pw = cred.password;
		
		var cred2 = JSON.parse(JSON.stringify(cred));
		cred2.password = crypto.getHash(cred.password);		
		var func = function(data) {
			return server.post("/v1/authorize", JSON.stringify(cred2))
		};
		
		return session.performLogin(func, cred2, pw)
		.then(function(result) {
			if (result.data.istatus === "ACTIVE") {							
				  cred.appname = null;
				  session.debugReturn();
				  document.location.href = cred.redirectUri + "?state=" + encodeURIComponent(cred.state) + "&code=" + result.data.code;
				  return "ACTIVE";
			} else
			return result.data;
			
		});
	};
	
	service.postLogin = function(result) {
		if (result.data.istatus === "ACTIVE") {							
			  cred.appname = null;
			  session.debugReturn();
			  document.location.href = cred.redirectUri + "?state=" + encodeURIComponent(cred.state) + "&code=" + result.data.code;
			  return "ACTIVE";
		} else return result.data;
	};
	
	return service;
}]);