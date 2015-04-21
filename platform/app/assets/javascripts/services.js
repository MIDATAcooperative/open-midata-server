var services = angular.module('services', []);
services.factory('currentUser', function($q, $http) {
	
	var deferred = $q.defer();
	
	$http(jsRoutes.controllers.Users.getCurrentUser()).
	success(function(userId) {
		deferred.resolve(userId);			
	});	
	 		
	return deferred.promise;
});
services.factory('users', function($q, $http) {
	
	var service = {};
	var dinfo;
	
	service.getMembers = function(properties, fields) {
		var data = {"properties": properties, "fields": fields};

		return $http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data));
	};
	
	service.getDashboardInfo = function(id) {
		if (!dinfo) {
		  dinfo = service.getMembers({"_id": id}, ["login", "news", "pushed", "shared", "apps", "visualizations"]);
		}
		return dinfo;
	}
		
	return service;
	
});
services.factory('hc', function($q, $http) {
	
	var service = {};
	
	service.list = function() {
	    return $http.get(jsRoutes.controllers.members.HealthProvider.list().url);
	};
	
	service.confirm = function(providerId) {
		var data = {"provider": providerId };

		return $http.post(jsRoutes.controllers.members.HealthProvider.confirmMemberKey().url, JSON.stringify(data));
	};
	
	service.reject = function(providerId) {
		var data = {"provider": providerId };

		return $http.post(jsRoutes.controllers.members.HealthProvider.rejectMemberKey().url, JSON.stringify(data));
	};
		
	return service;
	
});

services.factory('status', function() {
	return function(showerrors) {		
		this.loading = 0;
		this.isBusy = false;
		this.error = null;
		this.showerrors = showerrors;
		this.start = function() { this.loading++; this.isBusy = true; if (this.loading==1) this.error = null; };
		this.end = function() { this.loading--; if (this.loading<=0) this.isBusy = false; };
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
		   	return call.then(function(result) { me.end();return result; }, function(err) { me.fail(err); });		     
		};
		this.doSilent = function(call) {
			var me = this;
			return call.then(function(result) { return result; }, function(err) { me.fail(err); });
		}
	};
		
});
services.factory('views', function() {
	
	var mapping = {};
	var service = {};
	
	service.init = function(attrs) {
		var v = service.getView(attrs.viewid);
		if (attrs.title) v.title = attrs.title;
		return v;
	};
	
	service.def = function(attrs) {
		var v = service.getView(attrs.id);
		for (x in attrs) {
			v[x] = attrs[x];			
		}
		return v;
	}
	
	service.getView = function (id) {
		var r = mapping[id];
		if (r==null) {
		   r = { active: false, setup:null, title:"no title", links:{}, dependend:[] };
		   mapping[id] = r;
		} 
		return r;
	};
	
	service.setView = function (id, setup, title) {
		var view = service.getView(id);
		view.active = true; 
		view.setup = setup;
		if (title) view.title = title;
		return view;
	};
	
	service.link = function(id, name, viewid) {
		var view = service.getView(id);
		view.links[name] = viewid;
		service.getView(viewid).dependend.push(id);
	};
	
	service.updateLinked = function(view, name, setup) {
		if (!view.links[name]) return false;	
		service.setView(view.links[name], setup);
		return true;
	};
	
	service.disableView = function(id) {
		var view = service.getView(id);
		view.active = false; 
		view.setup = null;
	};
	
	service.changed = function(id) {
		var view = service.getView(id);
		_.each(view.dependend, function(x) { service.getView(x).setup = _.clone(service.getView(x).setup); });
	};
		
	return service;		
	
});

services.factory('records', function($http) {
	var service = {};
	
	service.getRecords = function(aps, properties, fields) {
		var data = {"properties": properties, "fields": fields};
		if (aps != null) data.aps = aps;
		return $http.post(jsRoutes.controllers.Records.getRecords().url, JSON.stringify(data));
	};
	
	service.getRecord = function(recordId) {
		var data = {"_id": recordId };		
		return $http.post(jsRoutes.controllers.Records.get().url, JSON.stringify(data));
	};
	
	service.unshare = function(aps, records, type) {
	  if (! angular.isArray(aps)) aps = [ aps ];
	  if (! angular.isArray(records)) records = [ records ];
	  var data = { records:records, started:[], stopped:aps, type:type };		
	  return $http.post(jsRoutes.controllers.Records.updateSharing().url, JSON.stringify(data));
	};
	
	service.share = function(aps, records, type) {
		  if (! angular.isArray(aps)) aps = [ aps ];
		  if (! angular.isArray(records)) records = [ records ];
		  var data = { records:records, started:aps, stopped:[], type:type };		
		  return $http.post(jsRoutes.controllers.Records.updateSharing().url, JSON.stringify(data));
	};
	
	service.search = function(query) {
		return $http(jsRoutes.controllers.Records.search(query));
	};
	
	return service;
});
services.factory('apps', function($http) {
	var service = {};

    service.getApps = function(properties, fields) {
   	   var data = {"properties": properties, "fields": fields};
	   return $http.post(jsRoutes.controllers.Apps.get().url, JSON.stringify(data));
    };

	return service;
});
services.factory('studies', function($http) {
	var service = {};
	
	service.search = function(properties, fields) {
		var data = {"properties": properties, "fields": fields };
		return $http.post(jsRoutes.controllers.members.Studies.search().url, JSON.stringify(data));
	};
	
	return service;
});
services.directive('modal', function (views) {
    return {
      template: '<div class="modal fade">' + 
          '<div class="modal-dialog modal-lg">' + 
            '<div class="modal-content">' + 
              '<div class="modal-header">' + 
                '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>' + 
                '<h4 class="modal-title">{{ view.title }}</h4>' + 
              '</div>' + 
              '<div class="" ng-transclude></div>' + 
            '</div>' + 
          '</div>' + 
        '</div>',
      restrict: 'E',
      transclude: true,
      replace:true,
      scope:true,
      link: function postLink(scope, element, attrs) {        
        scope.view = views.getView(attrs.viewid || scope.def.id);
        scope.view.modal = true;

        scope.$watch("view.active", function(value){
          if(value == true)
            $(element).modal('show');
          else
            $(element).modal('hide');
        });

        $(element).on('shown.bs.modal', function(){
          scope.$apply(function(){
            scope.view.active = true;
          });
        });

        $(element).on('hidden.bs.modal', function(){
          scope.$apply(function(){
        	  scope.view.active = false;
          });
        });
      }
    };
  });