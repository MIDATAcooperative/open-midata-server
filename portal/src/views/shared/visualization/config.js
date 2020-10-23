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
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('member.visualization', {
	      url: '/visualization/:visualizationId?name&context&query&params',
	      templateUrl: 'views/shared/visualization/visualization.html'
	    })	    
	    .state('provider.visualization', {
	      url: '/visualization/:visualizationId?name&context&query&params',
	      templateUrl: 'views/shared/visualization/visualization.html'
	    })
	    .state('research.visualization', {
	      url: '/visualization/:visualizationId?name&context&query&params&study&user',
	      templateUrl: 'views/shared/visualization/visualization.html'
	    })
	    .state('developer.visualization', {
	      url: '/visualization/:visualizationId?name&context&query&params',
	      templateUrl: 'views/shared/visualization/visualization.html'
	    })
	    .state('admin.visualization', {
	      url: '/visualization/:visualizationId?name&context&query&params',
	      templateUrl: 'views/shared/visualization/visualization.html'
	    });
}]);