'use strict';

angular.module('mealApp')
.directive('mealDirective', ['$window', 'mealJawboneService', function ($window, mealJawboneService) {
    function makeEllipses (scope) {
        var ellipses = [];

        if (scope.width < 420) {
            var radius = scope.width;

            ellipses.push({
                cx: 0.42 * radius,
                cy: 0.55 * radius,
                rx: 0.25 * radius,
                ry: 0.25 * radius
            });

            ellipses.push({
                cx: 0.7 * radius,
                cy: 0.78 * radius,
                rx: 0.22 * radius,
                ry: 0.22 * radius
            });

            ellipses.push({
                cx: 0.6 * radius,
                cy: 0.28 * radius,
                rx: 0.19 * radius,
                ry: 0.19 * radius
            });

            ellipses.push({
                cx: 0.16 * radius,
                cy: 0.76 * radius,
                rx: 0.16 * radius,
                ry: 0.16 * radius
            });

            ellipses.push({
                cx: 0.28 * radius,
                cy: 0.30 * radius,
                rx: 0.14 * radius,
                ry: 0.14 * radius
            });

            ellipses.push({
                cx: 0.36 * radius,
                cy: 0.88 * radius,
                rx: 0.12 * radius,
                ry: 0.12 * radius
            });

            ellipses.push({
                cx: 0.15 * radius,
                cy: 0.55 * radius,
                rx: 0.1 * radius,
                ry: 0.1 * radius
            });

            ellipses.push({
                cx: 0.88 * radius,
                cy: 0.4 * radius,
                rx: 0.08 * radius,
                ry: 0.08 * radius
            });

            ellipses.push({
                cx: 0.15 * radius,
                cy: 0.2 * radius,
                rx: 0.06 * radius,
                ry: 0.06 * radius
            });

        } else {
            var width = scope.width,
                height = scope.height;

            ellipses.push({
                cx: 0.5 * width,
                cy: 0.5 * height,
                rx: 0.33 * height,
                ry: 0.33 * height
            });

            ellipses.push({
                cx: 0.72 * width,
                cy: 0.72 * height,
                rx: 0.27 * height,
                ry: 0.27 * height
            });

            ellipses.push({
                cx: 0.76 * width,
                cy: 0.28 * height,
                rx: 0.24 * height,
                ry: 0.24 * height
            });

            ellipses.push({
                cx: 0.2 * width,
                cy: 0.8 * height,
                rx: 0.19 * height,
                ry: 0.19 * height
            });

            ellipses.push({
                cx: 0.28 * width,
                cy: 0.30 * height,
                rx: 0.16 * height,
                ry: 0.16 * height
            });

            ellipses.push({
                cx: 0.45 * width,
                cy: 0.12 * height,
                rx: 0.12 * height,
                ry: 0.12 * height
            });

            ellipses.push({
                cx: 0.15 * width,
                cy: 0.55 * height,
                rx: 0.1 * height,
                ry: 0.1 * height
            });

            ellipses.push({
                cx: 0.5 * width,
                cy: 0.90 * height,
                rx: 0.08 * height,
                ry: 0.08 * height
            });

            ellipses.push({
                cx: 0.12 * width,
                cy: 0.25 * height,
                rx: 0.05 * height,
                ry: 0.05 * height
            });
        }

        return ellipses;
    }

    function draw (scope) {
        var meal = d3.select('#meal')
            .attr('height', scope.height);

        var mealTip = d3.tip().attr('class', 'meal-tip').html(function(d) {
                        return '<h4 class="meal-tip-ratio text-center"><small>' +
                        d.text + '</small> ' + parseFloat(d.value / d.recommended * 100).toFixed(2) + '%</h4>' +
                        '<h5 class="text-center"><small>Consumed </small><span>' +
                        d.value + d.unit + '</span></h5>' +
                        '<h5 class="text-center"><small>Recommended </small><span>' +
                        d.recommended + d.unit + '</span></h5>';
                    });
        mealTip.direction('e');
        meal.call(mealTip);

        var ellipse = meal.select('#ellipses')
            .selectAll('ellipse')
                .data(scope.selectedMeal.detailsList);

        // Update

        // Enter
        ellipse.enter().append('ellipse');

        // Enter + Update
        ellipse
            .attr('class', 'ellipse')
            .attr('id', function (d) {
                return d.id;
            })
            .style('opacity', 0.20)
            .transition()
                .delay(function() {
                    return Math.random() * 10 * 50;
                })
                .duration(500)
                .style('opacity', 1)
                .attr('cx', function (d, i) {
                    return scope.ellipses[i].cx;
                })
                .attr('cy', function (d, i) {
                    return scope.ellipses[i].cy;
                })
                .attr('rx', function (d, i) {
                    return scope.ellipses[i].rx;
                })
                .attr('ry', function (d, i) {
                    return scope.ellipses[i].ry;
                });

        // Exit
        ellipse.exit().remove();

        var text = meal.select('#texts')
            .selectAll('text')
                .data(scope.selectedMeal.detailsList);


        // Update

        // Enter
        text.enter().append('text');

        // Enter + Update
        text
            .attr('class', 'meal-item-text')
            .attr('id', function (d) {
                return d.id + '-text';
            })
            .style('opacity', 0.25)
            .text(function(d) {
                if (scope.selectedMeal.menuText !== 'Summary') {
                    return d.text;
                }

                var ratio = d.value / d.recommended;
                if ( ratio < 0.9 ) {
                    return d.text + ' \u25BC';
                } else if ( ratio > 1.1 ) {
                    return d.text + ' \u25B2';
                } else {
                    return d.text;
                }
            })
            .attr('text-anchor', 'middle')
            .transition()
                .delay(function() {
                    return Math.random() * 10 * 50;
                })
                .duration(500)
                .style('opacity', 1)
                .attr('x', function (d, i) {
                    return scope.ellipses[i].cx;
                })
                .attr('y', function (d, i) {
                    return scope.ellipses[i].cy + 5;
                });

        // Exit
        text.exit().remove();

        var ellipseClick = meal.select('#ellipses-click')
            .selectAll('ellipse')
                .data(scope.selectedMeal.detailsList);

        // Update

        // Enter
        ellipseClick.enter().append('ellipse');

        // Enter + Update
        ellipseClick
            .attr('class', 'ellipse-click')
            .style('opacity', 0)
            .on('mouseover', function (d) {
                mealTip.show(d);
                d3.selectAll('.ellipse')
                    .transition()
                        .duration(50)
                        .style('opacity', 0.20);

                d3.select('#' + d.id)
                    .transition()
                        .duration(50)
                        .style('opacity', 1);
            })
            .on('mouseout', function (d) {
                mealTip.hide(d);
                d3.selectAll('.ellipse')
                    .transition()
                        .delay(function() {
                            return Math.random() * 10 * 50;
                        })
                        .duration(500)
                        .style('opacity', 0.7);
            })
            .attr('cx', function (d, i) {
                return scope.ellipses[i].cx;
            })
            .attr('cy', function (d, i) {
                return scope.ellipses[i].cy;
            })
            .attr('rx', function (d, i) {
                return scope.ellipses[i].rx;
            })
            .attr('ry', function (d, i) {
                return scope.ellipses[i].ry;
            });

        // Exit
        ellipse.exit().remove();
    }

    function link (scope) {
        scope.rootElement = angular.element('#meal')[0];
        scope.width = scope.rootElement.clientWidth;
        scope.height = 420;
        scope.ellipses = makeEllipses(scope);

        angular.element($window).bind('resize', function() {
            var newWidth = scope.rootElement.clientWidth;
            if (newWidth !== scope.width) {
                scope.width = newWidth;
                scope.ellipses = makeEllipses(scope);
                scope.$apply();
                draw(scope);
            }
        });

        scope.isDatepickerOpen = false;
        scope.$watchCollection('dateList', function (newValue) {
            if (newValue !== undefined) {
                scope.dateTimeList = [];
                scope.dateList.forEach(function (element) {
                    scope.dateTimeList.push(new Date(
                        element.getFullYear(),
                        element.getMonth(),
                        element.getDate()
                    ).getTime());
                });
                scope.selectedDate = scope.dateList[0];
                scope.datepickerMax = scope.dateList[0];
                scope.datepickerMin = scope.dateList[scope.dateList.length - 1];
            }
        });
        scope.updateSelectedDate = function ($event) {
            $event.preventDefault();
            $event.stopPropagation();
            scope.isDatepickerOpen = true;
        };
        scope.isDateDisabled = function (date, mode) {
            if (scope.dateTimeList !== undefined) {
                return mode === 'day' && scope.dateTimeList.indexOf(
                    new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
                    ) < 0;
            } else {
                return false;
            }
        };
        scope.$watch('selectedDate', function (newValue, oldValue) {
            if (newValue === undefined) {
                return;
            }

            if (oldValue !== undefined && oldValue.getTime() === newValue.getTime()) {
                return;
            }

            scope.selectedDateIndex = scope.dateTimeList.indexOf(new Date(
                newValue.getFullYear(),
                newValue.getMonth(),
                newValue.getDate()
            ).getTime());
        });

        scope.$watchCollection('mealList', function(newValue) {
            if (newValue !== undefined) {
                scope.meals = mealJawboneService.transformMealListData(
                    scope.mealList
                );
                scope.isMealsEmpty = scope.meals.length === 0;
                scope.selectedMeal = scope.meals[0];
                draw(scope);
            }
        });

        scope.isMealMenuOpen = false;
        scope.updateSelectedMeal = function (selectedField) {
            scope.isMealMenuOpen = !scope.isMealMenuOpen;
            if (scope.selectedMeal !== selectedField) {
                scope.selectedMeal = selectedField;
                draw(scope);
            }
        };
    }

    return {
        restrict: 'E',
        templateUrl: 'views/meal-view.html',
        scope: {
            mealList: '=',
            dateList: '=',
            selectedDateIndex: '='
        },
        link: link
    };
}]);
