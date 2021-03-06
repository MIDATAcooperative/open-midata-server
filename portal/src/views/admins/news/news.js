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
.controller('AdminNewsCtrl', ['$scope', '$state', 'views', 'status', 'news', function($scope, $state, views, status, news) {

	$scope.status = new status(true);
		
	$scope.init = function(userId) {	
		$scope.status.doBusy(news.get({ }, ["content", "created", "title", "studyId", "url", "expires", "language"]))
    	.then(function(results) {
		  $scope.news = results.data;
    	});
	};
	
	$scope.deleteNews = function(newsItem) {
		$scope.status.doAction('delete', news.delete(newsItem._id))
		.then(function(data) { $scope.init(); });
	};
			
	$scope.init();
	
}]);