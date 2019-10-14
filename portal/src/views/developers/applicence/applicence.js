angular.module('portal')
.controller('AppLicenceCtrl', ['$scope', '$state', 'server', 'apps', 'status', function($scope, $state, server, apps, status) {
	
	// init
	$scope.error = null;
	
	$scope.status = new status(false, $scope);
	$scope.entities = ["USER","USERGROUP","ORGANIZATION"];
				
	$scope.loadApp = function(appId) {
		$scope.status.doBusy(apps.getApps({ "_id" : appId }, ["_id", "version", "creator", "filename", "name", "description", "licenceDef"]))
		.then(function(data) { 
			$scope.app = data.data[0];
			if ($scope.app.licenceDef) {
			  $scope.licence = $scope.app.licenceDef;
			  $scope.licence.required = true;
			} else {
			  $scope.licence = { required : false, allowedEntities : [] };
			}
			$scope.licence.version = $scope.app.version;
		});
	};
	
	
	$scope.updateApp = function() {		
		$scope.submitted = true;	
					
		$scope.app.msgOnly = true;				
		$scope.status.doAction('submit', server.post(jsRoutes.controllers.Market.updateLicence($scope.app._id).url, JSON.stringify($scope.licence)))
		.then(function() { $scope.submitted = false;$state.go("^.manageapp", { appId : $scope.app._id });  });
		
	};	
	
	$scope.toggle = function(array,itm) {		
		var pos = array.indexOf(itm);
		if (pos < 0) array.push(itm); else array.splice(pos, 1);
	};
			
	$scope.loadApp($state.params.appId);	
}]);