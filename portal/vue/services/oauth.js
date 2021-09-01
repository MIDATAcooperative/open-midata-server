/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

import crypto from './crypto.js';
import session from './session.js';
import server from './server.js';

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
	
	var getDeviceId = function(vuescope) {
		var devid;
		if (localStorage && localStorage.deviceId) {
		   devid = localStorage.deviceId;
		}
		if (!devid) {
			devid = vuescope.$cookies.get("device");
		}
		if (!devid) {
			devid = randomString();						
		}
		if (localStorage) localStorage.deviceId = devid;
		vuescope.$cookies.set("device", devid);
		
		return devid;
	};
	
	service.createDeviceId = getDeviceId;
	
	service.init = function(vuescope, client_id, redirect_uri, state, code_challenge, code_challenge_method, devId) {
	   cred.appname = client_id;
	   cred.redirectUri = redirect_uri;
	   cred.state = state || "none";
	   cred.code_challenge = code_challenge;
	   cred.code_challenge_method = code_challenge_method;
	   cred.device = devId || getDeviceId(vuescope);
	};
	
	service.setUser = function(email, password, role, studyLink) {
	   cred.username = email;
	   cred.password = password;
	   cred.studyLink = studyLink;
	   cred.role = role || "MEMBER";
	};
	
	service.setUnlockCode = function(code) {
		
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
	
	service.setJoinCode = function(joinCode) {
		cred.joinCode = joinCode;
	};
	
	service.setProject = function(project) {
		cred.project = project;
	};
	
	service.setDuringRegistration = function(reg) {
		cred.reg = reg;
	}
	
	service.getProject = function() { return cred.project; };
	
	service.login = function(confirm, confirmStudy) {	    	
		cred.confirm = confirm || false;
		cred.confirmStudy = confirmStudy || (confirm && cred.confirmStudy);
       		
		var pw = cred.password;
		
		var cred2 = JSON.parse(JSON.stringify(cred));
		cred2.password = crypto.getHash(cred.password);		
		var func = function(data) {
			return server.post("/v1/authorize", cred2)
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
	
	export default service;