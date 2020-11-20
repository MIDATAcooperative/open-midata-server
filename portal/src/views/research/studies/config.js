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
	    .state('research.studies', {
	      url: '/studies',
	      templateUrl: 'views/research/studies/studies.html'
	    })
	    .state('research.study', {
	      url: '/study/:studyId',
	      templateUrl: 'views/research/studies/studynav.html',
	      allowExecution : true
	    })
	    .state('developer.studies', {
	      url: '/studies',
	      templateUrl: 'views/research/studies/studies.html'
	    })
	    .state('developer.study', {
	      url: '/study/:studyId',
	      templateUrl: 'views/research/studies/studynav_simple.html'
	    })
	    .state('admin.studies', {
	      url: '/studies',
	      templateUrl: 'views/research/studies/studies.html'
	    })
	    .state('admin.study', {
	      url: '/study/:studyId',
	      templateUrl: 'views/research/studies/studynav_simple.html'
	    });
}]);