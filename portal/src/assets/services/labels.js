angular.module('services')
.factory('labels', ['server', '$q', function(server, $q) {
	var service = {};
	
	var content_translations = {};
	var group_translations;
	var storedLang;
	
	
	service.reset = function(lang) {
		content_translations = {};
		group_translations = undefined;
		storedLang = lang;
	};
	
	service.loadGroups = function(lang) {		
		return server.get(jsRoutes.controllers.FormatAPI.listGroups().url)
		.then(function(result) {
			group_translations = {};
			angular.forEach(result.data, function(group) {
				group_translations[group.system+":"+group.name] = group.label[lang] || group.label.en || group.name;
			});
		});		
	};
	
		
	service.getContentLabel = function(lang, name) {
		if (lang != storedLang) service.reset(lang);
		
		var existing = content_translations[name];
		if (!existing) {
			return server.post(jsRoutes.controllers.FormatAPI.searchContents().url, JSON.stringify({ "properties" : { "content" : name } , "fields" : ["content", "label"] }))
			.then(function(result) {
				console.log(result.data);
				var content = result.data[0];
				content_translations[content.content] = content.label[lang] || content.label.en || content.content;
				return 	content_translations[content.content];
				
			});
		} else {
			return $q.when(existing);
		}
	};
	
	service.getGroupLabel = function(lang, system, name) {
		if (lang != storedLang) service.reset(lang);
		
		if (name.startsWith("cnt:")) return service.getContentLabel(lang, name.substring(4));
		
		if (!group_translations) {
			return service.loadGroups(lang).then(function() { return group_translations[system+":"+name]; });
		}
		var existing = group_translations[system+":"+name];
		if (!existing) {			
			return undefined;
		} else return $q.when(existing);
	};
		
	return service;
}]);