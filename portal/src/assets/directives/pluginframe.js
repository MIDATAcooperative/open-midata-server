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
.directive('pluginframe', [ function () {
    return {
      template: function(element, attrs) {
    	  return '<div id="'+attrs.id+'"></div>';  
      },
      restrict: 'E',
      transclude: false,
      replace:true,     
      scope: true,
      link: function postLink(scope, element, attrs) {
    	  attrs.$observe('mysrc', function(value) {
    		  if (value) {
    			 var html = '<iframe name="'+attrs.id+'" id="'+attrs.id+'_fr" src="'+value+'" height="'+attrs.height+'" width="'+attrs.width+'"></iframe>';
    			
    		    document.getElementById(attrs.id).innerHTML = html;
    		    
    		  }
    	  });
    	  scope.$on('$messageIncoming', function (event, data){		
    		 if (data && data.name == attrs.id && data.viewHeight && data.viewHeight !== "0px") {
    			 //console.log("adjust height for "+attrs.id+" to:"+data.viewHeight);
    		   	document.getElementById(attrs.id+"_fr").height = data.viewHeight;
    		 }  else if (data && data.name == attrs.id && data.type==="link") {    	  		 
    	  		console.log(data);
    	  		if (scope.openAppLink) scope.openAppLink(data);
    		 }
    	  });
      }
    };
  }]);