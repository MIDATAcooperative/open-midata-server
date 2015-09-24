angular.module('services')
.factory('status', ['$q', function($q) {
	return function(showerrors) {		
		this.loading = 0;
		this.isBusy = true;
		this.action = null;
		this.error = null;
		this.showerrors = showerrors;
		this.start = function() { this.loading++; this.isBusy = true; if (this.loading==1) this.error = null; };
		this.end = function() { this.loading--; if (this.loading<=0) { this.isBusy = false; this.action=null; } };		
		this.startAction = function(action) { this.loading++; this.action = action; if (this.loading==1) this.error = null; };		
		this.fail = function(msg) { 
			   console.log(msg);
			   this.loading--; 
			   this.error = msg; 
			   if (this.loading<=0) { this.isBusy = false; }
			   if (this.showerrors) alert("An error "+msg.status+" occured:"+msg.data);
		};
		this.doBusy = function(call) {
			var me = this;
		   	me.start();		 
		   	return call.then(function(result) { me.end();return result; }, function(err) { me.fail(err);return $q.reject(err); });		     
		};
		this.doAction = function(action, call) {
			var me = this;
		   	me.startAction(action);
		   	return call.then(function(result) { me.end();return result; }, function(err) { me.fail(err);return $q.reject(err); });		     
		};
		this.doSilent = function(call) {
			var me = this;
			return call.then(function(result) { return result; }, function(err) { me.fail(err);return $q.reject(err); });
		};
	};
		
}]);