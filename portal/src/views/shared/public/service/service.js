angular.module('portal')
.controller('ServiceCtrl', ['$scope', '$state', function($scope, $state) {
		
	
	$scope.init = function() {
		
		var actions = [];
		var params = {};
		
		if ($state.params.login) {
			params.login = $state.params.login;
		}
		
		if ($state.params.pluginName) {
			actions.push({ ac : "use", c : $state.params.pluginName });
		}
		
		if ($state.params.consent) {
			actions.push({ ac : "confirm", c : $state.params.consent });
		} else {
			actions.push({ ac : "unconfirmed" });
		}
		
		if ($state.params.callback) {
			actions.push({ ac : "leave", c : $state.params.callback });
		} else {
			actions.push({ ac : "leave" });
		}
		params.action=JSON.stringify(actions);
		$state.go("public.login", params);		
	};
	
	
	$scope.init();
}]);
