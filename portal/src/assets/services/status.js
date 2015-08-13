angular.module('services')
.factory('status', ['$q', function($q) {
	return function(showerrors) {		
		this.loading = 0;
		this.isBusy = false;
		this.error = null;
		this.showerrors = showerrors;
		this.start = function(action) { this.loading++; this.isBusy = action; if (this.loading==1) this.error = null; };
		this.end = function() { this.loading--; if (this.loading<=0) this.isBusy = false; };		
		this.fail = function(msg) { 
			   console.log(msg);
			   this.loading--; 
			   this.error = msg; 
			   if (this.loading<=0) { this.isBusy = false; }
			   if (this.showerrors) alert("An error "+msg.status+" occured:"+msg.data);
		};
		this.doBusy = function(call) {
			return this.doAction(true, call);		     
		};
		this.doAction = function(action, call) {
			var me = this;
		   	me.start(action);
		   	return call.then(function(result) { me.end();return result; }, function(err) { me.fail(err);return $q.reject(err); });		     
		};
		this.doSilent = function(call) {
			var me = this;
			return call.then(function(result) { return result; }, function(err) { me.fail(err);return $q.reject(err); });
		};
	};
		
}]);