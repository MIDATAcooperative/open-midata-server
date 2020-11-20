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
.directive('modal', ['views', function (views) {
    return {
      template: '<div class="modal fade">' + 
          '<div class="modal-dialog modal-lg">' + 
            '<div class="modal-content">' + 
              '<div class="modal-header">' +                  
                '<h4 class="modal-title" translate="{{ view.title }}"></h4>' +
                '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>' +
              '</div>' + 
              '<div class="modal-body" ng-transclude></div>' + 
            '</div>' + 
          '</div>' + 
        '</div>',
      restrict: 'E',
      transclude: true,
      replace:true,
      scope:true,
      link: function postLink(scope, element, attrs) {        
        scope.view = views.getView(attrs.viewid || scope.def.id);
        scope.view.modal = true;

        scope.$watch("view.active", function(value){
          if(value === true)
            $(element).modal('show');
          else
            $(element).modal('hide');
        });

        $(element).on('shown.bs.modal', function(){
          scope.$apply(function(){
            scope.view.active = true;
          });
        });

        $(element).on('hidden.bs.modal', function(){
          scope.$apply(function(){
        	  scope.view.active = false;
          });
        });
      }
    };
  }]);