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
.directive('formrow', ['views', function (views) {
    return {
      template: function(element, attrs) {
    	  return '<div class="form-group form-row" ng-class="{ \'has-error\' :  myform.' +
    	          attrs.myid + 
    	          '.$invalid && (myform.' + attrs.myid + '.$dirty || submitted), \'was-validated\' : submitted }">'+
                  '<label for="'+attrs.myid+'" class="col-sm-4 col-form-label midata-label" translate="'+attrs.label+'"></label>'+
                  '<div class="col-sm-8"><div class="" ng-transclude-replace></div>'+
                  '<p ng-show="myform.'+attrs.myid+'.$error.required && (myform.'+attrs.myid+'.$dirty || submitted)" class="invalid-feedback" translate="error.missing.input_field">'+
                  '</p></div></div>';
      },
      restrict: 'E',
      transclude: true,
      replace:true  
    };
}])

.directive('ngTranscludeReplace', ['$log', function ($log) {
              return {
                  terminal: true,
                  restrict: 'EA',

                  link: function ($scope, $element, $attr, ctrl, transclude) {
                      if (!transclude) {
                          $log.error('orphan',
                                     'Illegal use of ngTranscludeReplace directive in the template! ' +
                                     'No parent directive that requires a transclusion found. ');
                          return;
                      }
                      transclude(function (clone) {
                          if (clone.length) {
                              $element.replaceWith(clone);
                          }
                          else {
                              $element.remove();
                          }
                      });
                  }
              };
}])

.directive('formrowfloat', ['views', function (views) {
    return {
      template: function(element, attrs) {
    	 var classes = "mi-at-input__fieldset mi-at-input__fieldset--floating-label mi-at-input__fieldset--white";
    	 if (attrs.type == "select") classes = "mi-at-select__fieldset";
    	 else if (attrs.type == "checkbox") classes ="mi-at-input__fieldset mi-at-input--checkbox mi-at-input--checkbox-white";
    	 return '<div><fieldset class="'+classes+'" ng-class="{ \'has-error\' :  myform.' +
         attrs.myid + 
         '.$invalid && (myform.' + attrs.myid + '.$dirty || submitted) }">'+                
                '<span class="" ng-transclude-replace></span>'+
                (attrs.label ? ('<label for="'+attrs.myid+'" class="new mi-at-text" translate="'+attrs.label+'">'+
                '</label>') : '') + '</fieldset><div ng-show="myform.'+attrs.myid+'.$error.required && (myform.'+attrs.myid+'.$dirty || submitted)" class="mi-or-login__error"><span class="mi-at-text mi-at-text--smallest mi-at-text--white mi-at-text--highlighted" translate="error.missing.input_field"></span></div></div>';    	  
      },
      restrict: 'E',
      transclude: true,
      replace:true
    };
}])

.directive('floatingLabel', ['views', function (views) {
    return {      
      link: function( scope, elem, attrs ) {
          elem.bind('keyup', function() { if (elem[0].value) angular.element(elem).addClass("mi-x-has_value"); else elem.removeClass("mi-x-has_value"); /*setAttribute('value', elem.value);*/ });
          elem.bind('change', function() { if (elem[0].value) angular.element(elem).addClass("mi-x-has_value"); else elem.removeClass("mi-x-has_value"); /*setAttribute('value', elem.value);*/ });          
          if (scope.$eval(attrs.ngModel)) angular.element(elem).addClass("mi-x-has_value");
      },
      restrict: 'A'
    };
}]);