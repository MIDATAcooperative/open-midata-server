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

angular.module('services')
.factory('status', ['$q', '$state', '$document', function($q, $state, $document) {
	return function(showerrors, scope) {		
		this.loading = 0;
		this.isBusy = true;
		this.action = null;
		this.error = null;
		this.scope = scope;
		this.showerrors = showerrors;
		this.start = function() { this.loading++; this.isBusy = true; if (this.loading==1) this.error = null; };
		this.end = function() { this.loading--; if (this.loading<=0) { this.isBusy = false; this.action=null; } };		
		this.startAction = function(action) { this.loading++; this.action = action; if (this.loading==1) this.error = null; };		
		this.fail = function(msg, noerror) { 
			   console.log(msg);
			   this.loading--; 
			   this.error = msg; 
			   if (this.loading<=0) { this.isBusy = false;this.action=null; }
			   if (msg.status == 403 || msg.status == 401) {
				   // Now handeled by http interceptor
				   //alert("Please relogin. Your session has expired.");
				   //$state.go("public.login");
			   } else if (msg.status == 503 || msg.status == -1) {
				   document.location.reload();
			   } else {
			     if (this.showerrors && !noerror) alert("An error "+msg.status+" occured:"+(msg.data.message || msg.data));
			   }
		};
		this.doBusy = function(call) {
			var me = this;
		   	me.start();		 
		   	return call.then(function(result) { me.end();return result; }, function(err) { me.fail(err);return $q.reject(err); });		     
		};
		this.doAction = function(action, call) {
			var me = this;
		   	me.startAction(action);
		   	return call.then(function(result) { me.end();return result; }, function(err) { 		 
		   		console.log(err);
		   		if (err.data && err.data.field && err.data.type && me.scope && me.scope.myform) {		   			
		   			me.scope.error = err.data;
		   			if (me.scope.myform[err.data.field]) {
		   			  me.scope.myform[err.data.field].$setValidity(err.data.type, false);
		   			  var elem = $document[0].getElementById(err.data.field);
		   			  if (elem && elem.focus) elem.focus();
		   			} else {
		   			  err.data.field = undefined;
		   			}
		   			me.fail(err, true);
		   		} else {
		   			if (me.scope) me.scope.error = err.data;
		   			me.fail(err);
		   		}
		   		return $q.reject(err); 
		    });		     
		};
		this.doSilent = function(call) {
			var me = this;
			return call.then(function(result) { return result; }, function(err) { me.fail(err);return $q.reject(err); });
		};
	};
		
}]);