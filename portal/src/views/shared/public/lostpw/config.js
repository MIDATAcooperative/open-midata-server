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
	    .state('public.lostpw', {
	      url: '/lostpw',
	      templateUrl: 'views/shared/public/lostpw/lostpw.html',
	      data : { role : "member" }
	    })
	    .state('public_provider.lostpw', {
	      url: '/lostpw',
	      templateUrl: 'views/shared/public/lostpw/lostpw.html',
	      data : { role : "hpuser" } 
	    })
	    .state('public_research.lostpw', {
	      url: '/lostpw',
	      templateUrl: 'views/shared/public/lostpw/lostpw.html',
		  data : { role : "research" }
	    })
	    .state('public_developer.lostpw', {
	      url: '/lostpw',
	      templateUrl: 'views/shared/public/lostpw/lostpw.html',
		  data : { role : "developer" }
	    })
	    .state('public.setpw', {
	      url: '/setpw',
	      templateUrl: 'views/shared/public/lostpw/setpw.html'
	    });
}]);