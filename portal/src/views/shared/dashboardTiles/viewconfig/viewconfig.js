angular.module('views')
.controller('ViewConfigCtrl', ['$scope', 'server', '$attrs', '$sce', 'views', 'status', 'spaces', 'session', 'apps', function($scope, server, $attrs, $sce, views, status, spaces, session, apps) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    $scope.done = false;
    
    session.currentUser.then(function(userId) { 
    	$scope.userId = userId;
    	$scope.reload(); 
    });
    
    $scope.test = function(vis) {
    	apps.isVisualizationInstalled(vis.id)
		.then(function(result) {
			console.log(vis);
			console.log(result.data);
			if (result.data == "true" || result.data === true) {
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
    	var lnk;
    	if (vis.spaces) {
    		lnk = ("^.visualization({ visualizationId : '" + vis.id+"', name : '"+vis.title+"', query:'"+encodeURIComponent(JSON.stringify(vis.spaces[0].query))+"', context:'"+encodeURIComponent(vis.spaces[0].context)+"'})");
    	} else {
    		lnk = ("^.visualization({ visualizationId : '" + vis.id+"', name : '"+vis.title+"', context:'"+encodeURIComponent(views.context)+"'})");
    	}
    	
    	var teaser = {
				id : "vis"+vis.id,
				name : vis.name,
				template : "/views/shared/dashboardTiles/summary/summary.html",
				title : vis.title,
				position : "small",
				active : true,
				setup : {
					text : vis.teaser,
		        	link : lnk,
		        	icon : "/images/icons/add.png",
		        	button : "Info + Install"
				},
				actions : {
					remove : "config"
				}
		};	
		views.layout.small.push(views.def(teaser));
    };
    
    $scope.addSpace = function(space) {
    	 var spacedef =
	     {
	    	   id : "space"+space._id.$oid,
	    	   template : "/views/shared/dashboardTiles/spacesummary/spacesummary.html",
	    	   title : space.name,
	    	   active : true,
	    	   position : $scope.view.position,
	    	   actions : { /*big : "/members/spaces/" + space._id.$oid,*/ remove : { space : space._id.$oid } },
	    	   setup : { allowSelection : false, spaceId : space._id.$oid, spaceType : space.type }
	     };
	     views.layout[$scope.view.position].push(views.def(spacedef)); 
    };
    
    $scope.reload = function() {
    	if (!$scope.view.active || !$scope.userId || $scope.done) return;	
    	$scope.done = true;
    	
    	
    	$scope.status.doBusy($scope.view.setup.context ? spaces.getSpacesOfUserContext($scope.userId, $scope.view.setup.context) : spaces.getSpacesOfUser($scope.userId))
    	.then(function(results) {
    		$scope.view.active = (results.data.length === 0 && $scope.view.setup.visualizations == null) && !$scope.view.setup.always;
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
  				template : "/views/shared/dashboardTiles/simpleadd/simpleadd.html",  				
  				position : "small",
  				active : true,
  				setup : {  					
  		        	link : "^.market({ next : '"+document.location.href+"', context : '"+$scope.view.setup.context+"'})",
  		        	icon : "/images/icons/add.png",
  		        	button : "dashboard.install_btn"
  				}
    		  }));  
    	  }
    	});
    	    	
    };
    
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);