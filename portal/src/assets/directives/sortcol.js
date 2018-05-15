angular.module('portal')
.directive('sortby', [function () {
    return {          	          
      template : function(element, attrs) {
    	  return '<th ng-click="setSort(\''+attrs.sortby+'\');" class="clickable sort" ng-class="{\'asc\':sortby==\''+attrs.sortby+'\' ,\'desc\':sortby==\'-'+attrs.sortby+'\' }" ng-transclude></th>';
      },
      restrict: 'A',
      transclude : true,
      replace : true
    };
}]);