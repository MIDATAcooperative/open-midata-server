/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('portal')
.controller('ApiKeysCtrl', ['$scope', '$state', 'session', 'status', 'services', 'views', '$window', function($scope, $state, session, status, services, views, $window) {

	$scope.status = new status(true);
			
	loadServices = function(studyId) {	
        
        if (studyId) {
            $scope.status.doBusy(services.listByStudy(studyId))
		    .then(function(data) {
                $scope.services = data.data;
                angular.forEach($scope.services, $scope.showKeys);
		    });
        } else {
            $scope.status.doBusy(services.list())
		    .then(function(data) {
                $scope.services = data.data;						
                angular.forEach($scope.services, $scope.showKeys);
		    });
        }
				
    };
    
    $scope.addKey = function(service) {
        $scope.status.doAction("add", services.addApiKey(service._id))
        .then(function(result) {
            $scope.showkey = result.data;

            views.setView("apikey", { key : result.data }, "API Keys");
            $scope.showKeys(service);
        });
    };

    $scope.showKeys = function(service) {
        $scope.status.doAction("add", services.listKeys(service._id))
        .then(function(result) {
            service.keys = result.data;
        });
    };

    $scope.deleteService = function(service, key) {
        $scope.status.doAction("add", services.removeService(service._id))
        .then(function() {
            loadServices($state.params.studyId);
        });
    };

    $scope.deleteKey = function(service, key) {
        $scope.status.doAction("add", services.removeApiKey(service._id, key._id))
        .then(function() {
            loadServices($state.params.studyId);
        });
    };

    $scope.copyToClip = function(elem) {
		elem = elem.currentTarget;
		elem.focus();
		elem.select();
		
		$window.document.execCommand("copy");
	};

	session.currentUser.then(function(userId) { loadServices($state.params.studyId); });
	

}]);