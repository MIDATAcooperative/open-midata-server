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
	    .state('developer.manageapp', {
	      url: '/app/:appId',
	      templateUrl: 'views/developers/manageapp/overview.html',
	      allowDelete : false,
	      allowStudyConfig : false,
	      allowExport : false
	    })
	    .state('developer.editapp', {
	      url: '/app/:appId/edit',
	      templateUrl: 'views/developers/manageapp/manageapp.html',
	      allowDelete : false,
	      allowStudyConfig : false,
	      allowExport : false
	    })
	    .state('developer.registerapp', {
	      url: '/newapp',
	      templateUrl: 'views/developers/manageapp/manageapp.html',
	      allowDelete : false,
	      allowStudyConfig : false,
	      allowExport : false
	    })
	   .state('admin.manageapp', {
		      url: '/app/:appId',
		      templateUrl: 'views/developers/manageapp/overview.html',
		      allowDelete : true,
		      allowStudyConfig : true,
		      allowExport : true
	   })
	   .state('admin.editapp', {
		      url: '/app/:appId/edit',
		      templateUrl: 'views/developers/manageapp/manageapp.html',
		      allowDelete : true,
		      allowStudyConfig : true,
		      allowExport : true
	   })
	   .state('admin.registerapp', {
	      url: '/newapp',
	      templateUrl: 'views/developers/manageapp/manageapp.html',
	      allowDelete : false,
	      allowStudyConfig : false,
	      allowExport : false
	    });
}]);