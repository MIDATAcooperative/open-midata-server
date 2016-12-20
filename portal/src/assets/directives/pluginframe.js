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
    			 console.log(html);
    		    document.getElementById(attrs.id).innerHTML = html;
    		    console.log("DONE HTML");
    		  }
    	  });
    	  scope.$on('$messageIncoming', function (event, data){		
    		 if (data && data.name == attrs.id && data.viewHeight && data.viewHeight !== "0px") {
    			 console.log("adjust height for "+attrs.id+" to:"+data.viewHeight);
    		   	document.getElementById(attrs.id+"_fr").height = data.viewHeight;
    		 }
    	  });
      }
    };
  }]);