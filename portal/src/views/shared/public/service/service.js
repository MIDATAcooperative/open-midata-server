/*
 * This file is part of the Open MIDATA Platform.
 *
 * The Open MIDATA platform is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Platform is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA platform.  If not, see <http://www.gnu.org/licenses/>.
 */

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
		} else if ($state.params.project) {
			var prjs = $state.params.project.split(",");
			for (var j=0;j<prjs.length;j++) {
				actions.push({ ac : "study", s : prjs[j] });		
			}					
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
