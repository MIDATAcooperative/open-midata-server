angular.module('portal')
.directive('modal', ['views', function (views) {
    return {
      template: '<div class="modal fade">' + 
          '<div class="modal-dialog modal-lg">' + 
            '<div class="modal-content">' + 
              '<div class="modal-header">' + 
                '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>' + 
                '<h4 class="modal-title">{{ view.title }}</h4>' + 
              '</div>' + 
              '<div class="" ng-transclude></div>' + 
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