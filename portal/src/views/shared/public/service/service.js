angular.module('portal')
.controller('ServiceCtrl', ['$scope', '$state', function($scope, $state) {
		
	
	$scope.init = function() {
		
		var actions = [];
		var params = {};
		
		var copy = ["login","family","given","country","language","birthdate"];
		for (var i=0;i<copy.length;i++)
		if ($state.params[copy[i]]) {
			params[copy[i]] = $state.params[copy[i]];
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

		if ($state.params.isnew) {
          $state.go("public.registration", params);
		} else {
		  $state.go("public.login", params);
		}		
	};
	
	
	$scope.init();
}]);
