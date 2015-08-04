angular.module('views')
.controller('ViewConfigCtrl', ['$scope', '$http', '$attrs', '$sce', 'views', 'status', 'spaces', 'currentUser', 'apps', function($scope, $http, $attrs, $sce, views, status, spaces, currentUser, apps) {
	
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
		};	
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