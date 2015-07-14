var views = angular.module('views', ['services']);
views.controller('FlexibleRecordListCtrl', ['$scope', '$http', '$attrs', 'views', 'records', 'status', function($scope, $http, $attrs, views, records, status) {
			
	$scope.records = [];	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	$scope.limit = 4;
	
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;
		$scope.limit = $scope.view.position == "small" ? 4 : 20;
		$scope.status.doBusy(records.getRecords($scope.view.setup.aps, $scope.view.setup.properties, $scope.view.setup.fields)).
		then(function (result) { $scope.records = result.data; });
	};
	
	$scope.showDetails = function(record) {
		if (!views.updateLinked($scope.view, "record", { id : record.id })) {
		  window.location.href = portalRoutes.controllers.Records.details(record.id).url;
		}
	};
	
	$scope.removeRecord = function(record) {
		$scope.status.doSilent(records.unshare($scope.view.setup.aps, record._id.$oid, $scope.view.setup.type));
		$scope.records.splice($scope.records.indexOf(record), 1);
	};
	
	$scope.shareRecords = function() {
		var selection = _.filter($scope.records, function(rec) { return rec.marked; });
		selection = _.chain(selection).pluck('_id').pluck('$oid').value();
		$scope.status.doSilent(records.share($scope.view.setup.targetAps, selection, $scope.view.setup.type))
		.then(function () {
		   views.changed($attrs.viewid);
		   views.disableView($attrs.viewid);
		});
	};
	
	$scope.addRecords = function() {
		views.updateLinked($scope.view, "shareFrom", 
				 { aps : null, 
			       properties:{}, 
			       fields : $scope.view.setup.fields, 
			       targetAps : $scope.view.setup.aps, 
			       allowShare : true,
			       type : $scope.view.setup.type,
			       sharedRecords : $scope.records
			      });
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);
views.controller('FlexibleStudiesCtrl', ['$scope', '$http', '$attrs', 'views', 'studies', 'status', function($scope, $http, $attrs, views, studies, status) {
	
	$scope.studies = [];	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	$scope.limit = 4;
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;
		$scope.limit = $scope.view.position == "small" ? 4 : 20;
		$scope.status.doBusy(studies.search($scope.view.setup.properties, $scope.view.setup.fields)).
		then(function (result) { $scope.studies = result.data; });
	};
	
	$scope.showDetails = function(study) {
		window.location.href = portalRoutes.controllers.MemberFrontend.studydetails(study._id.$oid).url;
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);
views.controller('RecordDetailCtrl', ['$scope', '$http', '$attrs', 'views', 'records', 'apps', 'status', function($scope, $http, $attrs, views, records, apps, status) {
		
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.record = {};
	$scope.status = new status(true);
	
	$scope.reload = function() {
	   if (!$scope.view.active) return;	
       $scope.status.doBusy(records.getRecord($scope.view.setup.id)).
	   then(function(result) {
			$scope.record = result.data;
			$scope.record.json = JSON.stringify($scope.record.data, null, "\t");
			if (_.has($scope.record.data, "type") && $scope.record.data.type === "file") {
				$scope.downloadLink = jsRoutes.controllers.Records.getFile(recordId).url;
			}
			
			loadUserNames();
			
			apps.getApps({"_id": $scope.record.app}, ["name"]).
			then(function(result) { $scope.record.app = result.data[0].name; });
			
			console.log($scope.record);
			//var split = $scope.record.created.split(" ");
			//$scope.record.created = split[0] + " at " + split[1];
		});
	};
    
    
	var loadUserNames = function() {		
		var data = {"properties": {"_id": [$scope.record.owner, $scope.record.creator]}, "fields": ["firstname", "sirname"]};
		$scope.status.doSilent($http.post(jsRoutes.controllers.Users.getUsers().url, JSON.stringify(data))).
			then(function(result) {				
				_.each(result.data, function(user) {
					if ($scope.record.owner && $scope.record.owner.$oid === user._id.$oid) { $scope.record.owner = (user.firstname+" "+user.sirname).trim(); }
					if ($scope.record.creator && $scope.record.creator.$oid === user._id.$oid) { $scope.record.creator = (user.firstname+" "+user.sirname).trim(); }
				});
				if (!$scope.record.owner) $scope.record.owner = "?";
				if (!$scope.record.creator) $scope.record.creator = "Same as owner";
			});
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);
views.controller('AddRecordsCtrl', ['$scope', '$http', '$attrs', 'views', 'records', 'status', function($scope, $http, $attrs, views, records, status) {
	
	$scope.foundRecords = [];
	$scope.criteria = { query : "" };
	$scope.title = $attrs.title;
	$scope.viewid = $attrs.viewid || $scope.def.id;
	$scope.view = views.getView($scope.viewid);
	$scope.records = [];
	$scope.status = new status(true);
	$scope.newest = true;
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;	
		$scope.foundRecords = [];
		$scope.criteria.query = "";
								
		$scope.searchRecords();
		
		if ($scope.view.setup.sharedRecords != null) $scope.records = _.chain($scope.view.setup.sharedRecords).pluck('_id').value();
	};
			
	$scope.shareRecords = function() {
		var selection = _.filter($scope.foundRecords, function(rec) { return rec.checked; });
		selection = _.chain(selection).pluck('_id').pluck('$oid').value();
		console.log(selection);
		$scope.status.doSilent(records.share($scope.view.setup.targetAps, selection, $scope.view.setup.type))
		.then(function () {
		   views.changed($scope.viewid);
		   views.disableView($scope.viewid);
		});
	};
	
	// check whether record is not already in active space
	$scope.isntInSpace = function(record) {		
		return !$scope.containsRecord($scope.records, record._id);
	};
	
	// helper method for contains
	$scope.containsRecord = function(recordIdList, recordId) {
		var ids = _.map(recordIdList, function(element) { return element.$oid; });
		return _.contains(ids, recordId.$oid);
	};
	
	$scope.showDetails = function(record) {
		if (!views.updateLinked($scope.view, "record", { id : record.id })) {
		  window.location.href = portalRoutes.controllers.Records.details(record.id).url;
		}
	};
	
	// search for records
	$scope.searchRecords = function() {		
		var query = $scope.criteria.query;
		$scope.foundRecords = [];
		
		if (query) {			
			$scope.newest = false;
			$scope.status.doBusy(records.search(query)).
				then(function(results) {
					$scope.error = null;
					$scope.foundRecords = results.data;					
				});
		} else {
			$scope.newest = true;
			
		    $scope.status.doBusy(records.getRecords(null, { "max-age" : 86400 * 31 }, [ "ownerName", "created", "id", "name" ])).
			then(function (result) { 
				$scope.foundRecords = result.data;
				$scope.searching = false; 
			});
		}
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);
views.controller('ListHealthProviderCtrl', ['$scope', '$http', '$attrs', 'views', 'hc', 'status', function($scope, $http, $attrs, views, hc, status) {
	
	$scope.results =[];
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
	
	$scope.reload = function() {
			
		$scope.status.doBusy(hc.list()).
		then(function(results) { 				
			$scope.results = results.data;				
			$scope.showNewHCRecords();
		});
	};
	
	$scope.confirm = function(memberKey) {
		hc.confirm(memberKey.provider.$oid).then(function() { $scope.reload(); });		
	};
	
	$scope.reject = function(memberKey) {
		hc.reject(memberKey.provider.$oid).then(function() { $scope.reload(); });
	};
	
	$scope.mayReject = $scope.mayConfirm = function(memberKey) {
		return memberKey.status == "UNCONFIRMED";
	};
	
	
	$scope.showNewHCRecords = function() {
		var creators = [];
		var aps = null;
		_.each($scope.results, function(hc) {
			console.log(hc);
			if (hc.provider) {
				creators.push(hc.provider.$oid);
				aps = hc.member.$oid;
			}
		});
		
		if (aps != null) {
		  views.setView("hcrecords", { aps : aps, properties: { "max-age" : 60*60*24*31, "creator" : creators }, fields : [ "creatorName", "created", "id", "name" ]});
		} else {
		  views.disableView("hcrecords");
		}
	};
	
	$scope.showRecords = function(mk) {
		views.setView("records", { aps : mk.aps.$oid, properties: {}, fields : [ "ownerName", "created", "id", "name" ], allowAdd : true, type:"memberkeys"}, mk.name);
	};
	
	$scope.reload();
	
}]);
views.controller('CreateRecordCtrl', ['$scope', '$http', '$attrs', '$sce', 'views', 'status', 'apps', 'currentUser', function($scope, $http, $attrs, $sce, views, status, apps, currentUser) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    $scope.apps = null;
    $scope.showapp = false;    

    currentUser.then(function(userId) { 
    	$scope.userId = userId;
    	$scope.reload(); 
    });
    
    $scope.reload = function() {
    	if (!$scope.view.active || !$scope.userId) return;	
    	
    	var appId = $scope.view.setup.appId;
    	var userId = $scope.view.setup.userId;
    
    	if (appId) {
    		$scope.selectApp(appId);
    	} else {
    		$scope.showAppList();
    	}
    	    	
    };
    
    $scope.selectApp = function(appId, title) {
    	if (!$scope.view.setup.inline) {
    		window.location.href = "/members/records/create/" + appId;
    		return;
    	}
    	
        $scope.showapp = true;
        // if (title != null) $scope.view.title = title;
        $scope.status.doBusy($http(jsRoutes.controllers.Apps.getPreviewUrl(appId))).
		then(function(results) {			
			$scope.url = $sce.trustAsResourceUrl(results.data);
		});
    };
    
    $scope.showAppList = function() {
    	$scope.showapp = false;
    	if (!$scope.apps) $scope.loadAppList();
    };
    
    $scope.loadAppList = function() {
    	$scope.status.doBusy(apps.getAppsOfUser($scope.userId, ["create","oauth1","oauth2"], ["name", "type", "previewUrl"]))
    	.then(function(results) {
    	   $scope.apps = results.data;   
    	  _.each(results.data, function(app) {
    		  if (!app.previewUrl) return; 
     		  var appdef =
     		     {
     		    	   id : "app"+app._id.$oid,
     		    	   template : "/assets/views/members/createrecord.html",
     		    	   title : app.name,
     		    	   active : true,
     		    	   position : "small",
     		    	   actions : { big : "/members/records/create/" + app._id.$oid },
     		    	   setup : { appId : app._id.$oid, inline : true }
     		     };
     		 views.layout.small.push(views.def(appdef)); 
     	  });     
    	  
    	  
    	});
    };
	/* 
	$scope.memberUrl = portalRoutes.controllers.ProviderFrontend.member(userId).url;
	console.log($scope.memberUrl);
	
	
	$http(jsRoutes.controllers.Apps.getUrlForMember(appId, userId)).
		success(function(url) {
			$scope.error = null;
			$scope.url = $sce.trustAsResourceUrl(url);
		}).
		error(function(err) { $scope.error = "Failed to load app: " + err; });
	*/
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);
views.controller('ShowSpaceCtrl', ['$scope', '$http', '$attrs', '$sce', 'views', 'status', 'spaces', 'currentUser', function($scope, $http, $attrs, $sce, views, status, spaces, currentUser) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    $scope.spaces = null;
    $scope.showspace = false;    

    currentUser.then(function(userId) { 
    	$scope.userId = userId;
    	$scope.reload(); 
    });
    
    $scope.reload = function() {
    	if (!$scope.view.active || !$scope.userId) return;	
    	
    	var spaceId = $scope.view.setup.spaceId;    	
    
    	if (spaceId) {
    		$scope.selectSpace(spaceId);
    	} else {
    		$scope.showSpaceList();
    	}
    	    	
    };
    
    $scope.selectSpace = function(spaceId, title) {
        $scope.showspace = true;
        // if (title != null) $scope.view.title = title;
        $scope.status.doBusy(spaces.getUrl(spaceId)).
		then(function(results) {			
			$scope.url = $sce.trustAsResourceUrl(results.data);
		});
    };
        
    $scope.showSpaceList = function() {
    	$scope.showspace = false;
    	$scope.loadSpaceList();
    	// if ($scope.spaces == null) { $scope.loadSpaceList(); }
    };
    
    $scope.loadSpaceList = function() {
    	$scope.status.doBusy(spaces.getSpacesOfUser($scope.userId))
    	.then(function(results) {
    	  $scope.spaces = results.data;    	  
    	});
    };
	 
	
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);
views.controller('SummaryCtrl', ['$scope', '$http', '$attrs', '$sce', 'views', 'status', function($scope, $http, $attrs, $sce, views, status) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    
    $scope.reload = function() { };
    
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);
views.controller('SpaceSummaryCtrl', ['$scope', '$http', '$attrs', '$sce', 'records', 'views', 'status', 'spaces', function($scope, $http, $attrs, $sce, records, views, status, spaces) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    $scope.count = 0;
    $scope.last = null;
    
    $scope.reload = function() { 
        if (!$scope.view.active) return;	
    	
    	var spaceId = $scope.view.setup.spaceId;
    	var recordId = $scope.view.setup.recordId;
    	
    	$scope.status.doBusy(recordId ? records.getUrl(recordId) : ($scope.view.setup.nopreview ? spaces.getUrl(spaceId) : spaces.getPreviewUrl(spaceId))).
		then(function(results) {
			if (results.data) {
			  $scope.url = $sce.trustAsResourceUrl(results.data);
			} else {
			  $scope.status.doBusy(records.getRecords(spaceId, {}, ["created"])).
			  then(function(results) {
				 $scope.count = results.data.length;		
				 $scope.last = $scope.count > 0 ? _.chain(results.data).pluck('created').max().value() : null;
			  });						
			}
		});
    	    	    				
    };
        
    $scope.showSpace = function() {    	
    	window.location.href = "/members/spaces/"+$scope.view.setup.spaceId;
    };
    
    $scope.showCreate = function() {    	
    	window.location.href = "/members/records/create/" + $scope.view.setup.appId;    		
    };
    
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);
views.controller('ViewConfigCtrl', ['$scope', '$http', '$attrs', '$sce', 'views', 'status', 'spaces', 'currentUser', 'apps', function($scope, $http, $attrs, $sce, views, status, spaces, currentUser, apps) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    $scope.done = false;
    
    currentUser.then(function(userId) { 
    	$scope.userId = userId;
    	$scope.reload(); 
    });
    
    $scope.test = function(vis) {
    	apps.isVisualizationInstalled(vis.id)
		.then(function(result) {
			if (result.data == "true") {
				$scope.install(vis);
			} else {
				$scope.addTeaser(vis);
			}
		});
    };
    
    $scope.install = function(vis) {
    	angular.forEach(vis.spaces, function(space) {
    		space.visualization = vis.id;
    		spaces.add(space)
    		.then (function(result) {
    			console.log(result);
    			$scope.addSpace(result.data);
    		});
    	});
    };
    
    $scope.addTeaser = function(vis) {
    	var teaser = {
				id : "vis"+vis.id,
				template : "/assets/views/members/info/summary.html",
				title : vis.title,
				position : "small",
				active : true,
				setup : {
					text : vis.teaser,
		        	link : ("/members/visualizations/" + vis.id+"#?next="+encodeURIComponent(document.location.href)+"&name="+encodeURIComponent(vis.title)+"&query="+encodeURIComponent(JSON.stringify(vis.spaces[0].query))+"&context="+encodeURIComponent(vis.spaces[0].context)),
		        	icon : "/assets/images/icons/add.png",
		        	button : "Info + Install"
				}
		}		
		views.layout.small.push(views.def(teaser));
    };
    
    $scope.addSpace = function(space) {
    	 var spacedef =
	     {
	    	   id : "space"+space._id.$oid,
	    	   template : "/assets/views/members/spacesummary.html",
	    	   title : space.name,
	    	   active : true,
	    	   position : "small",
	    	   actions : { /*big : "/members/spaces/" + space._id.$oid,*/ remove : { space : space._id.$oid } },
	    	   setup : { allowSelection : false, spaceId : space._id.$oid, appId : (space.app ? space.app.$oid : null) }
	     };
	     views.layout.small.push(views.def(spacedef)); 
    };
    
    $scope.reload = function() {
    	if (!$scope.view.active || !$scope.userId || $scope.done) return;	
    	$scope.done = true;
    	
    	
    	$scope.status.doBusy($scope.view.setup.context ? spaces.getSpacesOfUserContext($scope.userId, $scope.view.setup.context) : spaces.getSpacesOfUser($scope.userId))
    	.then(function(results) {
    		$scope.view.active = (results.data.length == 0 && $scope.view.setup.visualizations == null) && !$scope.view.setup.always;
    		var usedvis = {};
    	  _.each(results.data, function(space) {
    		  usedvis[space.visualization.$oid] = true;
    		  $scope.addSpace(space);    		
    	  });
    	  if ($scope.view.setup.visualizations) {
    	  _.each($scope.view.setup.visualizations, function(vis) {
    		if (!usedvis[vis.id]) {
    			$scope.test(vis);    			
    		}  
    	  });
    	  }
    	  if ($scope.view.setup.always) {
    		  views.layout.small.push(views.def({
    			id : "addmore",
    			order : 1000,
  				template : "/assets/views/members/info/simpleadd.html",
  				title : "Add more",
  				position : "small",
  				active : true,
  				setup : {  					
  		        	link : "/members/market#?next="+encodeURIComponent(document.location.href)+"&context="+encodeURIComponent($scope.view.setup.context),
  		        	icon : "/assets/images/icons/add.png",
  		        	button : "Install from Market"
  				}
    		  }));  
    	  }
    	});
    	    	
    };
    
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);
views.controller('AccountDataCtrl', ['$scope', '$http', '$attrs', 'users', 'views', 'status', 'currentUser', function($scope, $http, $attrs, users, views, status, currentUser) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    
    currentUser.then(function(userId) { 
    	$scope.userId = userId;
    	$scope.reload(); 
    });
    
    $scope.reload = function() { 
    	if (!$scope.view.active || !$scope.userId) return;	
    	
    	$scope.status.doBusy(users.getMembers({ "_id" : $scope.userId }, ["midataID", "firstname", "sirname", "birthday", "address1", "address2", "zip", "city", "country"]))
    	.then(function(results) { $scope.member = results.data[0]; });
    };
    
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);
views.controller('ResearchSettingsCtrl', ['$scope', '$http', '$attrs', '$sce', 'views', 'status', function($scope, $http, $attrs, $sce, views, status) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    
    $scope.reload = function() { };
    
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);

views.controller('MessagesCtrl', ['$scope', '$http', '$attrs', 'currentUser', 'views', 'status', function($scope, $http, $attrs, currentUser, views, status) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    $scope.limit = 4;
    
    currentUser.then(function(userId) { 
    	$scope.userId = userId;
    	$scope.reload(); 
    });
    
    $scope.reload = function() { 
    	if (!$scope.view.active || !$scope.userId) return;
    	$scope.limit = $scope.view.position == "small" ? 4 : 20;
    	
    	getFolders($scope.userId);
    };
    
    // get messages
	getFolders = function(userId) {
		var properties = {"_id": userId};
		var fields = ["messages"];
		var data = {"properties": properties, "fields": fields};
		$scope.status.doBusy($http.post(jsRoutes.controllers.Users.getUsers().url, JSON.stringify(data))).
		then(function(results) {
			    var users = results.data;
				$scope.inbox = users[0].messages.inbox;
				//$scope.archive = users[0].messages.archive;
				//$scope.trash = users[0].messages.trash;
				var messageIds = $scope.inbox;
				getMessages(messageIds);
		});
	};
	
	getMessages = function(messageIds) {
		var properties = {"_id": messageIds};
		var fields = ["sender", "created", "title"];
		var data = {"properties": properties, "fields": fields};
		$scope.status.doBusy($http.post(jsRoutes.controllers.Messages.get().url, JSON.stringify(data))).
		then(function(results) {
			    $scope.messages = results.data;
			    //var messages = results.data;
				//_.each(messages, function(message) { $scope.messages[message._id.$oid] = message; });
				//var senderIds = _.map(messages, function(message) { return message.sender; });
				//senderIds = _.uniq(senderIds, false, function(senderId) { return senderId.$oid; });
				//getSenderNames(senderIds);
		});
	};
	
	$scope.showMessage = function(messageId) {
		window.location.href = jsRoutes.controllers.Messages.details(messageId).url;
	};
            
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);
views.controller('CirclesCtrl', ['$scope', '$http', '$attrs', 'views', 'circles', 'status', function($scope, $http, $attrs, views, circles, status) {
	
	$scope.circles = [];	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);
	$scope.limit = 4;
	$scope.alreadyadded = false;
	$scope.form = { newCircleName : "" };
	$scope.errors = {};
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;
		$scope.limit = $scope.view.position == "small" ? 4 : 20;
		$scope.status.doBusy(circles.get($scope.view.setup.properties, $scope.view.setup.fields)).
		then(function (result) { 
			$scope.circles = result.data;
			
			if ($scope.alreadyadded || !$scope.view.setup.instances) return;
			$scope.alreadyadded = true;
						
			_.each($scope.circles, function(circle) {
				var circledef =
	   		     {
	   		    	   id : "circle"+circle._id.$oid,
	   		    	   template : "/assets/views/members/flexiblerecords.html",
	   		    	   title : circle.ownerName ? circle.ownerName : circle.name,
	   		    	   active : true,
	   		    	   position : "small",
	   		    	   setup : { aps : circle.aps.$oid, properties : { "max-age" : 86400 * 31 } , fields : [ "ownerName", "created", "id", "name" ] }
	   		     };
	   		     views.layout.small.push(views.def(circledef)); 
			});
		});
	};
	
	$scope.createCircle = function() {
		console.log($scope.form);
		if ($scope.form.newCircleName.trim() == "") {
			$scope.errors.newCircleName = "Please enter a valid name";
			return;
		} else { $scope.errors.newCircleName = null; }
		
		circles.createNew($scope.form.newCircleName).
		then(function(results) {
			window.location.href = "/members/circles/"+results.data._id.$oid;
		});		
	};
	
	/*$scope.showDetails = function(study) {
		window.location.href = portalRoutes.controllers.MemberFrontend.studydetails(study._id.$oid).url;
	};*/
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);
views.controller('ShareCtrl', ['$scope', '$http', '$attrs', 'views', 'circles', 'spaces', 'records', 'status', function($scope, $http, $attrs, views, circles, spaces, records, status) {
	
	$scope.circles = [];	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
	$scope.status = new status(true);	
	$scope.form = { newCircleName : "" };
	$scope.errors = {};
	$scope.selectedCircle = null;
	
	$scope.reload = function() {		
		if (!$scope.view.active) return;
		$scope.limit = $scope.view.position == "small" ? 4 : 20;
		$scope.status.doBusy(circles.get({ owner : true }, [ "name", "order", "members" ])).
		then(function (result) { 
			$scope.circles = result.data;
            if ($scope.circles.length > 0) $scope.selectedCircle = $scope.circles[0];
            $scope.circles.push({ name : "<Create new...>", isNew : true });			
		});
		$scope.status.doBusy(spaces.get({ "_id" : { "$oid" : $scope.view.setup.space  }}, [ "rules" ]))
		.then(function(result) {
			if (result.data) $scope.rules = result.data[0].rules;
		});
	};
	
	$scope.createCircle = function() {
		console.log($scope.form);
		if ($scope.form.newCircleName.trim() == "") {
			$scope.errors.newCircleName = "Please enter a valid name";
			return;
		} else { $scope.errors.newCircleName = null; }
		
		circles.createNew($scope.form.newCircleName).
		then(function(results) {
			$scope.circles.push(results.data);
			$scope.selectedCircle = results.data;
			//window.location.href = "/members/circles/"+results.data._id.$oid;
		});		
	};
	
	$scope.share = function() {
		records.shareSpaceWithCircle($scope.view.setup.space, $scope.selectedCircle._id.$oid)
		.then(function() {
		   $scope.success = true;
           //views.disableView($scope.view.id);			
		});
	};
	
	$scope.cancel = function() {
		views.disableView($scope.view.id);
	};
		
	
	$scope.$watch('view.setup', function() { $scope.reload(); });	
	
}]);