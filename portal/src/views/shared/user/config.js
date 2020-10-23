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
	    .state('member.user', {
	      url: '/user/:userId',
	      templateUrl: 'views/shared/user/user.html' 
	    })
	   
	    .state('provider.user', {
	      url: '/user/:userId',
	      templateUrl: 'views/shared/user/user.html' 
	    })
	    .state('research.user', {
	      url: '/user/:userId',
	      templateUrl: 'views/shared/user/user.html' 
	    })
	    .state('admin.user', {
	      url: '/user/:userId',
	      templateUrl: 'views/shared/user/user.html' 
	    })
	    .state('developer.user', {
	      url: '/user/:userId',
	      templateUrl: 'views/shared/user/user.html' 
	    })
	    
	    
	     .state('member.accountwipe', {
	      url: '/accountwipe',
	      templateUrl: 'views/shared/user/accountwipe.html' 
	    })
	   
	    .state('provider.accountwipe', {
	      url: '/accountwipe',
	      templateUrl: 'views/shared/user/accountwipe.html' 
	    })
	    .state('research.accountwipe', {
	      url: '/accountwipe',
	      templateUrl: 'views/shared/user/accountwipe.html' 
	    })	    
	    .state('developer.accountwipe', {
	      url: '/accountwipe',
	      templateUrl: 'views/shared/user/accountwipe.html' 
	    })
	    .state('admin.accountwipe', {
	      url: '/accountwipe',
	      templateUrl: 'views/shared/user/accountwipe.html' 
	    });	 
	       
}]);