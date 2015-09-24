angular.module('portal')
.directive('pluginframe', [ function () {
    return {
      template: function(element, attrs) {
    	  return '<iframe id="'+attrs.id+'" src="" height="'+attrs.height+'" width="'+attrs.width+'" ng-cloak></iframe>'; 
      },
      restrict: 'E',
      transclude: false,
      replace:true,     
      scope: true,
      link: function postLink(scope, element, attrs) {
    	  attrs.$observe('mysrc', function(value) {
    		  if (value) {
    		    document.getElementById(attrs.id).src = value;
    		  }
    	  });
    	  scope.$on('$messageIncoming', function (event, data){		
    		 if (data && data.viewHeight && data.viewHeight !== "0px") {
    			 console.log("adjust height for "+attrs.id+" to:"+data.viewHeight);
    		   	document.getElementById(attrs.id).height = data.viewHeight;
    		 }
    	  });
      }
    };
  }]);