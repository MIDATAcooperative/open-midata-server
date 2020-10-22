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
.controller('AdminTermsCtrl', ['$scope', '$state', 'views', 'status', 'terms', 'session', function($scope, $state, views, status, terms, session) {

	$scope.status = new status(true);
		
	$scope.filter = { name : "" };
	
	$scope.init = function(userId) {	
		$scope.status.doBusy(terms.search({ }, ["name", "version", "language", "title", "createdAt"]))
    	.then(function(results) {
    	
    	  var names = {};
    	  angular.forEach(results.data, function(term) { 
    		  if (names[term.name]) names[term.name].push(term);
    		  else names[term.name] = [ term ];
    	  });
    	  
    	  $scope.terms = [];
    	  angular.forEach(names, function(v,k) {
    		  var versions = {};
    		  angular.forEach(v, function(terms) {
    			  if (versions[terms.version]) versions[terms.version].push(terms);
    			  else versions[terms.version] = [ terms ];
    		  });
    		  
    		  var v = Object.values(versions);
    		  v.sort(function(a,b) { return a[0].version < b[0].version ? 1 : a[0].version > b[0].version ? -1 : 0; });
    		  console.log(v);
    		  $scope.terms.push(v);
    	  });
    	  
    	  $scope.terms.sort(function(a,b) { return a[0][0].name < b[0][0].name ? -1 : a[0][0].name > b[0][0].name ? 1 : 0});
    	});
	};
	
	$scope.byName = function(t) {
		return t[0][0].name.indexOf($scope.filter.name) >= 0;
	};
		
	$scope.name = function(term) {
		return term.name+"--"+term.version;
	};
		
	session.load("AdminTermsCtrl", $scope, ["filter"]);
	
	$scope.init();
	
}]);