angular.module('portal')
.directive('formrow', ['views', function (views) {
    return {
      template: function(element, attrs) {
    	  return '<div class="form-group" ng-class="{ \'has-error\' :  myform.' +
    	          attrs.myid + 
    	          '.$invalid && (myform.' + attrs.myid + '.$dirty || submitted) }">'+
                  '<label for="'+attrs.myid+'" class="col-sm-4 control-label">'+attrs.label+'</label>'+
                  '<div class="col-sm-8"><div class="" ng-transclude></div>'+
                  '<p ng-show="myform.'+attrs.myid+'.$error.required && (myform.'+attrs.myid+'.$dirty || submitted)" class="help-block">'+
                  'Please fill out this input field.</p></div></div>';
      },
      restrict: 'E',
      transclude: true,
      replace:true  
    };
}]);