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
	
	service.getMembers = function(properties, fields) {
		var data = {"properties": properties, "fields": fields};

		return $http.post(jsRoutes.controllers.Users.get().url, JSON.stringify(data));
	};
		
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

services.factory('views', function() {
	
	var mapping = {};
	var service = {};
	
	service.getView = function (id) {
		var r = mapping[id];
		if (r==null) {
		   r = { active: false, setup:null, links:{}, dependend:[], version:0 };
		   mapping[id] = r;
		} 
		return r;
	}
	
	service.setView = function (id, setup) {
		var view = service.getView(id);
		view.active = true; 
		view.setup = setup;
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
                '<h4 class="modal-title">{{ title }}</h4>' + 
              '</div>' + 
              '<div class="modal-body" ng-transclude></div>' + 
            '</div>' + 
          '</div>' + 
        '</div>',
      restrict: 'E',
      transclude: true,
      replace:true,
      scope:true,
      link: function postLink(scope, element, attrs) {
        scope.title = attrs.title;
        scope.view = views.getView(attrs.viewid);
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