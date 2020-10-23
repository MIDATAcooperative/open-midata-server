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
	    .state('member.records', {
	      url: '/records',
	      templateUrl: 'views/shared/records/records.html' 
	    })
	    .state('member.delete_records', {
	      url: '/records-delete',
	      templateUrl: 'views/shared/records/records.html',
	      allowDelete : true,
	    })
	    .state('member.recordsharing', {
	      url: '/records/:selectedType/:selected',
	      templateUrl: 'views/shared/records/records.html' 
	    })
	    .state('provider.recordsharing', {
	      url: '/records/:selectedType/:selected',
	      templateUrl: 'views/shared/records/records.html' 
	    })
	    .state('provider.delete_records', {
	      url: '/records-delete',
		  templateUrl: 'views/shared/records/records.html',
		  allowDelete : true,
		})
	    .state('developer.delete_records', {
	      url: '/records-delete',
		  templateUrl: 'views/shared/records/records.html',
		  allowDelete : true,
		})
	    .state('developer.records', {
	      url: '/records',
	      templateUrl: 'views/shared/records/records.html' 
	    })
	    .state('developer.recordsharing', {
	      url: '/records/:selectedType/:selected',
	      templateUrl: 'views/shared/records/records.html' 
	    })
	    
	    .state('admin.delete_records', {
	      url: '/records-delete',
		  templateUrl: 'views/shared/records/records.html',
		  allowDelete : true,
		  allowDeletePublic : true
		})
	    .state('admin.records', {
	      url: '/records',
	      templateUrl: 'views/shared/records/records.html' 
	    })
	    .state('admin.recordsharing', {
	      url: '/records/:selectedType/:selected',
	      templateUrl: 'views/shared/records/records.html' 
	    })
	    
	    .state('research.records', {
	      url: '/records',
	      templateUrl: 'views/shared/records/records.html',
	      role : 'research'
	    })
	    .state('research.recordsharing', {
	      url: '/records/:selectedType/:selected',
	      templateUrl: 'views/shared/records/records.html' 
	    })
	    .state('research.study.recordsharing', {
	      url: '/records/:selectedType/:selected',
	      templateUrl: 'views/shared/records/records.html' 
	    })
	    .state('research.delete_records', {
	      url: '/records-delete',
		  templateUrl: 'views/shared/records/records.html',
		  allowDelete : true,
		});
}]);