'use strict';

describe('Directive: mealDirective', function () {

  // load the directive's module
  beforeEach(module('mealApp'));

  var element,
    scope;

  beforeEach(inject(function ($rootScope) {
    scope = $rootScope.$new();
  }));

  it('should make hidden element visible', inject(function ($compile) {
    element = angular.element('<meal-directive></meal-directive>');
    element = $compile(element)(scope);
    expect(element.text()).toBe('this is the mealDirective directive');
  }));
});
