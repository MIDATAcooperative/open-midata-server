
angular.module('fhirDocref', [ 'midata', 'ui.router','ui.bootstrap', 'angularFileUpload', 'pascalprecht.translate' ])
.config(['$stateProvider', '$urlRouterProvider', '$translateProvider', function($stateProvider, $urlRouterProvider, $translateProvider) {	    
    
	$translateProvider
	.useSanitizeValueStrategy('escape')	   	    
	.registerAvailableLanguageKeys(['en', 'de', 'it', 'fr'], {
	  'en_*': 'en',
	  'de_*': 'de',
	  'fr_*': 'fr',
	  'it_*': 'it',
	})
	.translations('en', en)
	.translations('de', de)
	.translations('it', it)
	.translations('fr', fr)
	.fallbackLanguage('en');
	
	 $stateProvider
	    .state('record', {
	      url: '/record?id&authToken',	   
	      templateUrl: 'single_record.html'
	    })	    
	    .state('overview', {
	      url: '/overview?lang&authToken',	   
	      templateUrl: 'overview.html'
	    })
	    .state('preview', {
	      url: '/preview?lang&authToken',	   
	      templateUrl: 'preview.html'
	    })
	    .state('create', {
	      url: '/create?measure&authToken',	    
	      templateUrl: 'create.html'
	    });
	 
	 $urlRouterProvider
	 .otherwise('/overview');  
}])
.run(['$translate', '$location', 'midataPortal', 'midataServer', function($translate, $location, midataPortal, midataServer) {
	console.log("Language: "+midataPortal.language);
    
	$translate.use(midataPortal.language);	
    midataPortal.autoresize();
				
	midataServer.authToken = $location.search().authToken;
	//var params = $location.search();
    
}])
.factory('configuration', ['$q','midataServer', function($q, midataServer) {
   var result = {};
   
   result.load = function() {
	   
	   return midataServer.getConfig(midataServer.authToken)
       .then(function (res) {
    	 result.config = { measures : [], owner : "self" }; 
         if (res.data) {
             if (res.data && res.data.readonly) {
                 result.readonly = true;
             } else {
                 result.config = res.data;                 
             }             
         }  
         return result.config;
       });
	   	  			
   };
   
   result.save = function() {	   
       result.saving = true;
       midataServer.setConfig(midataServer.authToken, result.config)
       .then(function() { result.saving = false; });  
   };
   
   result.getConfig = function() {
	   result.configPromise = result.configPromise || result.load();
	   return result.configPromise;
   };
   console.log("configuration");
   return result;
}])
.factory('fhirinfo', ['$q', '$translate', 'midataServer', 'midataPortal', function($q, $translate, midataServer, midataPortal) {
   var result = {};
      
   result.types = midataServer.searchContent(midataServer.authToken, { resourceType : "fhir/DocumentReference" }, [ "content", "label", "defaultCode", "resourceType", "subType", "defaultUnit", "category", "source" ])
   .then(function(res) {
	 angular.forEach(res.data, function(item) {
		switch(item.source) {
		case 'doctor' : item.order = 1;break;
		case 'specialist': item.order = 5;break;
		case 'hospital': item.order = 4;break;
		case 'patient': item.order = 2;break;
		case 'other' : item.order = 3;break;
		} 
		item.labelTranslation = item.label[midataPortal.language] || item.label.en;
	 });
     return res.data;
	});
   
   return result;
}])
.factory('data', ['$q', '$filter', 'fhirinfo', 'midataServer', 'midataPortal', function($q, $filter, fhirinfo, midataServer, midataPortal) {
		
	var result = {};
	
	result.owners = {};
	
	result.groupByCategory = function(records) {
		var categories = {};
		angular.forEach(records, function(record) {
			var cat = "other";		
			if (record.data.category) cat = result.getCodeableConcept(record.data.category);
			var proposed = fhirinfo.getCategory(record.content);
			if (proposed) cat = proposed; 
			var targetCategory = categories[cat];
			if (!targetCategory) {
				targetCategory = categories[cat] = { label : cat, records:[] };
			}
			targetCategory.records.push(record);
			record.label = result.getCodeableConcept(record.data.code);
		});
		return categories;
	};
		
	
	result.getRecords = function(params) { 	
		
		var query = { "format" : "fhir/DocumentReference" };
		if (params.content) query.content = params.content;
		if (params.owner) query.owner = params.owner;
		if (params.ids) query._id = params.ids;
		//if (params.after && params.before) query.index = { "effectiveDateTime" : { "!!!ge" : params.after, "!!!le" : params.before }};
		console.log(params);
		return midataServer.getRecords(midataServer.authToken, query, ["name", "created", "content", "data", "owner", "ownerName", "version"])
		.then(function(results) {
			angular.forEach(results.data, function(rec) { result.owners[rec.owner.$oid] = rec.ownerName; });
			console.log(result.owners);
			return results.data;			
		}, function(err) {
			result.error = "Failed to load records: " + err.data; 			
		});
	};
			
	result.getCodeableConcept = function(what) {
		if (what == null) return null;
		if (what.text) return what.text;
		if (what.coding && what.coding.length > 0) {
			return result.getCoding(what.coding[0]); 				
		}
		return "?";
	};
	
	result.getCoding = function(what) {
		if (what == null) return null;
		if (what.display != null) return what.display;
	    return what.code;
	};
	
	result.getReference = function(what) {
		if (what == null) return null;
		if (what.display) return what.display;
		return what.reference;
	};
		      
	
   console.log("data");
	return result;
}]);
