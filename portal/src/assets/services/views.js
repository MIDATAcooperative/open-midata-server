angular.module('services')
.factory('views', function() {
	
	var mapping = {};
	var service = {};
	
	service.reset = function() {
		mapping = {};	
	};
	
	service.init = function(attrs) {
		var v = service.getView(attrs.viewid);
		if (attrs.title) v.title = attrs.title;
		return v;
	};
	
	service.def = function(attrs) {
		var v = service.getView(attrs.id);
		for (var x in attrs) {
			v[x] = attrs[x];			
		}
		return v;
	};
	
	service.getView = function (id) {
		var r = mapping[id];
		if (r == null) {
		   r = { active: false, setup:null, title:"no title", links:{}, dependend:[], order : 0 };
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
