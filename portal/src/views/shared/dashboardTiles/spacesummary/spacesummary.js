angular.module('views')
.controller('SpaceSummaryCtrl', ['$scope', '$state', '$translate', 'server', '$attrs', '$sce', 'records', 'views', 'status', 'spaces', function($scope, $state, $translate, server, $attrs, $sce, records, views, status, spaces) {
	
	$scope.view = views.getView($attrs.viewid || $scope.def.id);
    $scope.status = new status(true);
    $scope.count = 0;
    $scope.last = null;
    
    $scope.reload = function() { 
        if (!$scope.view.active) return;	
    	               
    	var spaceId = $scope.view.setup.spaceId;
    	var recordId = $scope.view.setup.recordId;
    	
    	$scope.view.reload = function() {
    		var node = $("#"+$scope.view.id);
    		var parent = node.parent();
    		node.remove();
    		parent.append(node);
    		
    		//console.log($scope.view.id);
    		//var url = $scope.url+"&_random="+(new Date()).getTime();
    		//console.log(url);
    		//$scope.url = $sce.trustAsResourceUrl(url);
    		//$("#"+$scope.view.id).src = $scope.url+"&_random="+(new Date()).getTime();
    	};
    	
    	if ($scope.view.setup.url) {
    	  $scope.view.attach = $scope.view.setup.info;
    	  $scope.url = $sce.trustAsResourceUrl(spaces.url($scope.view.attach, $scope.view.setup.url, $scope.view.setup.params, $translate.use()));
    	  $scope.createIFrame();
    	  $scope.status.isBusy = false;
    	} else {
    	
    	$scope.status.doBusy(recordId ? records.getUrl(recordId) : spaces.getUrl(spaceId)).
		then(function(results) {
			$scope.previewType = results.data.type || "visualization";
			if (results.data.preview || $scope.view.setup.nopreview) {	
			  var url = $scope.view.setup.nopreview ? spaces.mainUrl(results.data, $translate.use()) : spaces.previewUrl(results.data, $translate.use());
			  $scope.view.attach = results.data;
			  $scope.url = $sce.trustAsResourceUrl(url);			 
			  if (results.data.add) $scope.view.showadd = true;
			  $scope.createIFrame();
			} else {			  
			  $scope.status.doBusy(records.getInfos(spaceId, {}, "ALL")).
			  then(function(results) {				 
				 if (results.data.length > 0) {
					 $scope.count = results.data[0].count;				 
					 $scope.last = results.data[0].newest;
				 }
			  });						
			}
		});
    	
    	}
    	    	    				
    };
        
    $scope.showSpace = function() {
    	if ($scope.view.setup.spaceType == "oauth1" || $scope.view.setup.spaceType == "oauth2") {
    	  $state.go('^.importrecords', { spaceId : $scope.view.setup.spaceId });
    	//} else if ($scope.view.setup.spaceType == "create") {
        //  $state.go('^.createrecord', { spaceId : $scope.view.setup.spaceId });
    	} else {
    	  if ($scope.view.actions.view) {
    		$scope.link($scope.view.actions.view.pos, $scope.view.actions.view.url, $scope.view.actions.view.params);
    	  } else {
    	    $state.go('^.spaces', { spaceId : $scope.view.setup.spaceId });
    	  }
    	}
    };
    
    $scope.createIFrame = function() {
    	$scope.iframe=$sce.trustAsHtml('<iframe class="frame" id="'+$scope.view.id+'" name="'+$scope.view.id+'" src="'+$scope.url+'"></iframe>');
    };
    
    $scope.showAdd = function() { 
    	if ($scope.view.actions.add) {
    	  $scope.link($scope.view.actions.add.pos, $scope.view.actions.add.url, $scope.view.actions.add.params);
    	} else {
    	  $state.go('^.spaces', { spaceId : $scope.view.setup.spaceId });
    	}
    };
    
    $scope.link = function(pos, url, params) {
       var spacedef =
		    {
		   	   id : "gen"+(new Date()).getTime(),
		   	   template : "/views/shared/dashboardTiles/spacesummary/spacesummary.html",
		   	   title : $scope.view.title,
		       active : true,
		       position : pos,
		       actions : {  },
		       setup : { allowSelection : false, info : $scope.view.attach, url : url, params : params, showview:false, showadd:false }
		     };
	   views.layout[pos].push(views.def(spacedef));   
    };
    
    $scope.view.makeBig = $scope.showSpace;
    
    $scope.showCreate = function() {    	
    	$state.go('^.createrecord', { appId : $scope.view.setup.appId });    		
    };
    
	$scope.$watch('view.setup', function() { $scope.reload(); });
	
}]);