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
.config(["$stateProvider", function($stateProvider) {
	   $stateProvider
	    .state('research.createstudy', {
	      url: '/createstudy',
	      templateUrl: 'views/research/createstudy/createstudy.html'
	    })
	    .state('research.study.description', {
	      url: '/description',
	      templateUrl: 'views/research/createstudy/createstudy.html'	    
	    })
	    .state('developer.createstudy', {
	      url: '/createstudy',
	      templateUrl: 'views/research/createstudy/createstudy.html'
	    })
	    .state('developer.study.description', {
	      url: '/description',
	      templateUrl: 'views/research/createstudy/createstudy.html'	    
	    })
	    .state('admin.createstudy', {
	      url: '/createstudy',
	      templateUrl: 'views/research/createstudy/createstudy.html'
	    })
	    .state('admin.study.description', {
	      url: '/description',
	      templateUrl: 'views/research/createstudy/createstudy.html'	    
	    })
	    ;
}]);