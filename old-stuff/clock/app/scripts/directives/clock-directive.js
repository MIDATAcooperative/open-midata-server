'use strict';

angular.module('clockApp')
    /**
     * Clock directive that allows the visualization of events during the day,
     * the current implementation supports move and sleep events from
     * the Jawbone tracker app.
     */
    .directive('clockDirective', ['$window', 'clockUtilsService',
    'clockJawboneService', function ($window, clockUtilsService,
                                     clockJawboneService) {
        function updateScope(scope) {
            var oldRadius = scope.radius;
            scope.radius = (scope.rootElement.clientWidth *
                0.08333333333333333 * 7) / 2;
            scope.radius = scope.radius <= 210 ? scope.radius : 210;
            scope.height = scope.width = scope.radius * 2;
            scope.center = scope.radius;
            scope.clockInnerArc = clockUtilsService.makeArc(
                0,
                0.45 * scope.radius,
                0,
                2 * Math.PI
            );
            scope.clockOuterArc = clockUtilsService.makeArc(
                scope.clockInnerArc.outerRadius()(),
                1.0 * scope.radius,
                0,
                2 * Math.PI
            );

            return oldRadius !== scope.radius;
        }
        /**
        *  This function updates clock move event objects
        *
        *  A clock move event has the following fields:
        *      value
        *      cssClass
        *      time
        *      duration
        *      arc
        *      rotate
        *      type
        */
        function updateClockMoveEvents(scope) {
            if (scope.clockMoveData !== undefined) {
                var clockMoveData = scope.clockMoveData.slice(),
                    clockMoveEvents = [],
                    clockOuterArcWidth = scope.clockOuterArc.outerRadius()() -
                        scope.clockOuterArc.innerRadius()();

                if (clockMoveData.length > 0) {
                    clockMoveData.sort(function (a, b) {
                        return b[scope.selectedMoveField.value] -
                            a[scope.selectedMoveField.value];
                    });

                    var maxValue = clockMoveData[0]
                        [scope.selectedMoveField.value];

                    clockMoveData.forEach(function (element) {
                        var value = element[scope.selectedMoveField.value],
                            ratioToMaxValue = value / maxValue,
                            startDate = new Date(element.startTime * 1000),
                            endDate = new Date(element.endTime * 1000),
                            startAngle = clockUtilsService.degree2Rad(
                                clockUtilsService.time2Angle(
                                    startDate.getHours(),
                                    startDate.getMinutes())),
                            endAngle = clockUtilsService.degree2Rad(
                                clockUtilsService.time2Angle(
                                    endDate.getHours(), endDate.getMinutes())),
                            cssClass = 'clock-event clock-move-event-',
                            rotate = 0,
                            arc;

                        cssClass = cssClass + Math.ceil(
                            Math.ceil(ratioToMaxValue * 10) / 2);

                        if (startAngle > endAngle) {
                            rotate = 2 * Math.PI - startAngle;
                            endAngle = endAngle + Math.PI * 2 - startAngle;
                            startAngle = 0;
                            rotate = clockUtilsService.rad2Degree(-rotate);
                        }

                        arc = clockUtilsService.makeArc(
                                scope.clockInnerArc.outerRadius()(),
                                clockOuterArcWidth *
                                    Math.sqrt(ratioToMaxValue) +
                                    scope.clockInnerArc.outerRadius()(),
                                startAngle,
                                endAngle
                        );

                        clockMoveEvents.push({
                            value: value,
                            cssClass: cssClass,
                            time: startDate,
                            duration: element.endTime - element.startTime,
                            arc: arc,
                            rotate: rotate,
                            type: 'move'
                        });
                    });
                }

                clockMoveEvents.sort(function(a, b) {
                    return a.time - b.time;
                });

                scope.clockMoveEvents = clockMoveEvents;
            } else {
                scope.clockMoveEvents = [];
            }
            scope.isClockMoveEventsEmpty = scope.clockMoveEvents.length === 0;
        }

        /**
        *  This function update clock sleep event objects
        *
        *  A clock sleep event has the following fields:
        *      value
        *      description
        *      cssClass
        *      time
        *      duration
        *      arc
        *      rotate
        *      type
        */
        function updateClockSleepEvents(scope) {
            if (scope.clockSleepData !== undefined) {
                var clockSleepData = scope.clockSleepData.slice(),
                    clockSleepEvents = [],
                    clockOuterArcWidth = scope.clockOuterArc.outerRadius()() -
                        scope.clockOuterArc.innerRadius()();

                clockSleepData.forEach(function (element) {
                    var value = element.depth,
                        startDate = new Date(element.startTime * 1000),
                        endDate = new Date(element.endTime * 1000),
                        startAngle = clockUtilsService.degree2Rad(
                            clockUtilsService.time2Angle(
                                startDate.getHours(), startDate.getMinutes())),
                        endAngle = clockUtilsService.degree2Rad(
                            clockUtilsService.time2Angle(
                                endDate.getHours(), endDate.getMinutes())),
                        cssClass = 'clock-event clock-sleep-event-' + value,
                        rotate = 0,
                        arc;

                    if (startAngle > endAngle) {
                        rotate = 2 * Math.PI - startAngle;
                        endAngle = endAngle + Math.PI * 2 - startAngle;
                        startAngle = 0;
                        rotate = clockUtilsService.rad2Degree(-rotate);
                    }

                    arc = clockUtilsService.makeArc(
                            scope.clockInnerArc.outerRadius()(),
                            clockOuterArcWidth * value / 5 +
                                scope.clockInnerArc.outerRadius()(),
                            startAngle,
                            endAngle
                    );

                    clockSleepEvents.push({
                        value: value,
                        description: element.description,
                        cssClass: cssClass,
                        time: startDate,
                        duration: element.endTime - element.startTime,
                        arc: arc,
                        rotate: rotate,
                        type: 'sleep'
                    });
                });

                clockSleepEvents.sort(function(a, b) {
                    return a.time - b.time;
                });

                scope.clockSleepEvents = clockSleepEvents;
            } else {
                scope.clockSleepEvents = [];
            }

            scope.isClockSleepEventsEmpty = scope.clockSleepEvents.length === 0;
        }

        function drawClockMoveEvents(scope) {
            var clock = d3.select('#clock');

            var moveEvent = clock.select('#clock-move-events').selectAll('path')
                .data(scope.clockMoveEvents);

            var moveTip = d3.tip().attr('class', 'clock-tip').html(function(d) {
                return '<span class="clock-tip-value">' + d.value +
                        '</span> <span class="clock-tip-unit">' +
                         scope.selectedMoveField.unit + '</span>';
            });
            clock.call(moveTip);
            // Update


            // Enter
            moveEvent.enter().append('path');

            // Enter + Update
            moveEvent
                .style('opacity', 0)
                .attr('class', function(d) {
                    return d.cssClass;
                })
                .attr('d', function(d) {
                    return d.arc();
                })
                .attr('transform', function(d) {
                    return 'translate(' + scope.center + ',' + scope.center +
                        '), rotate(' + d.rotate + ')';
                })
                .on('mouseover', function(d) {
                    moveTip.show(d);
                    d3.selectAll('.clock-event')
                        .transition()
                            .duration(50)
                            .style('opacity', 0.25);

                    d3.select(this)
                        .transition()
                            .duration(50)
                            .style('opacity', 1);
                })
                .on('mouseout', function(d) {
                    moveTip.hide(d);
                    d3.selectAll('.clock-event')
                        .transition()
                            .delay(50)
                            .style('opacity', 1);
                })
                .transition()
                    .delay(function(d, i) {
                        return i * 25;
                    })
                    .duration(100)
                    .style('opacity', 1);

            // Exit
            moveEvent.exit().remove();
        }

        function drawClockSleepEvents(scope) {
            var clock = d3.select('#clock');

            if (scope.sleepDataRadio === 'hidden') {
                clock.selectAll('#clock-sleep-events path').remove();
                return;
            }

            var sleepEvent = clock.select('#clock-sleep-events')
                .selectAll('path')
                    .data(scope.clockSleepEvents);

            var sleepTip = d3.tip().attr('class', 'clock-tip').html(
                function(d) {
                    return '<span class="clock-tip-value">' + d.duration +
                        's</span> <span class="clock-tip-unit"> ' +
                        d.description + '</span>';
                });
            clock.call(sleepTip);

            // Update


            // Enter
            sleepEvent.enter().append('path');

            // Enter + Update
            sleepEvent
                .style('opacity', 0)
                .attr('class', function(d) {
                    return d.cssClass;
                })
                .attr('d', function(d) {
                    return d.arc();
                })
                .attr('transform', function(d) {
                    return 'translate(' + scope.center + ',' + scope.center +
                        '), rotate(' + d.rotate + ')';
                })
                .on('mouseover', function(d) {
                    sleepTip.show(d);
                    d3.selectAll('.clock-event')
                        .transition()
                            .duration(50)
                            .style('opacity', 0.25);

                    d3.select(this)
                        .transition()
                            .duration(50)
                            .style('opacity', 1);
                })
                .on('mouseout', function(d) {
                    sleepTip.hide(d);
                    d3.selectAll('.clock-event')
                        .transition()
                            .delay(50)
                            .style('opacity', 1);
                })
                .transition()
                    .delay(function(d, i) {
                        return i * 25;
                    })
                    .duration(100)
                    .style('opacity', 1);

            // Exit
            sleepEvent.exit().remove();
        }

        function draw(scope) {
            var clock = d3.select('#clock')
                    .attr('width', scope.width)
                    .attr('height', scope.height);

            clock.select('.clock-outer')
                .attr('d', scope.clockOuterArc)
                .attr('transform',
                    'translate(' + scope.center + ',' + scope.center + ')');

            clock.select('.clock-inner')
                .attr('d', scope.clockInnerArc)
                .attr('transform',
                    'translate(' + scope.center + ',' + scope.center + ')');

            drawClockSleepEvents(scope);
            drawClockMoveEvents(scope);
        }

        function link(scope, element) {
            scope.rootElement = element[0];

            scope.clockMoveEvents = [];
            scope.clockSleepEvents = [];
            updateScope(scope);
            draw(scope);
            scope.clockTicks = _.map(_.range(24),
                clockUtilsService.makeClockTick);

            angular.element($window).bind('resize', function() {
                if (updateScope(scope)) {
                    updateClockMoveEvents(scope);
                    updateClockSleepEvents(scope);
                    scope.$apply();
                    draw(scope);
                }
            });

            scope.$watchCollection('moveTicks', function (newValue) {
                if (newValue !== undefined) {
                    scope.moveTicks = newValue;
                    scope.clockMoveData = clockJawboneService
                        .transformMoveTicksData(
                            scope.moveTicks.data.items,
                            scope.selectedGranularity.value
                        );
                    updateClockMoveEvents(scope);
                    drawClockMoveEvents(scope);
                }
            });

            scope.$watchCollection('sleepTicks', function (newValue) {
                if (newValue !== undefined) {
                    scope.sleepTicks = newValue;
                    scope.sleepDataStatus.ticks = true;
                    if (scope.sleepDataStatus.list) {
                        scope.sleepDataStatus.list = false;
                        scope.clockSleepData = clockJawboneService
                            .transformSleepData(
                                scope.sleepList.data.items,
                                scope.sleepTicks.data.items
                            );
                        updateClockSleepEvents(scope);
                        drawClockSleepEvents(scope);
                    }
                }
            });

            scope.$watchCollection('sleepList', function (newValue) {
                if (newValue !== undefined) {
                    scope.sleepList = newValue;
                    scope.sleepDataStatus.list = true;
                    if (scope.sleepDataStatus.ticks) {
                        scope.sleepDataStatus.ticks = false;
                        scope.clockSleepData = clockJawboneService
                            .transformSleepData(
                                scope.sleepList.data.items,
                                scope.sleepTicks.data.items
                            );
                        updateClockSleepEvents(scope);
                        drawClockSleepEvents(scope);
                    }
                }
            });

            scope.sleepDataRadio = 'shown';
            scope.sleepDataStatus = {
                ticks: false,
                list: false
            };
            scope.$watch('sleepDataRadio', function () {
                drawClockSleepEvents(scope);
            });

            scope.isDatepickerOpen = false;
            scope.$watchCollection('dateList', function (newValue) {
                if (newValue !== undefined) {
                    scope.dateList = newValue;
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
                    scope.datepickerMin = scope
                        .dateList[scope.dateList.length - 1];
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
                        new Date(date.getFullYear(), date.getMonth(),
                                 date.getDate()).getTime()
                        ) < 0;
                } else {
                    return false;
                }
            };
            scope.$watch('selectedDate', function (newValue, oldValue) {
                if (newValue === undefined) {
                    return;
                }

                if (oldValue !== undefined &&
                        oldValue.getTime() === newValue.getTime()) {
                    return;
                }

                scope.selectedDateIndex = scope.dateTimeList.indexOf(new Date(
                    newValue.getFullYear(),
                    newValue.getMonth(),
                    newValue.getDate()
                ).getTime());
            });

            scope.moveField = clockJawboneService.makeMoveField();
            scope.selectedMoveField = scope.moveField[0];
            scope.isMoveFieldMenuOpen = false;
            scope.updateSelectedMoveField = function (selectedField) {
                scope.isMoveFieldMenuOpen = !scope.isMoveFieldMenuOpen;
                if (scope.selectedMoveField !== selectedField) {
                    scope.selectedMoveField = selectedField;
                    updateClockMoveEvents(scope);
                    drawClockMoveEvents(scope);
                }
            };

            scope.granularities = [
                {value: 600, text: 'Every 10 Minutes'},
                {value: 1200, text: 'Every 20 Minutes'},
                {value: 1800, text: 'Every 30 Minutes'}
            ];
            scope.selectedGranularity = scope.granularities[1];
            scope.granularityMenuIsOpen = false;
            scope.updateSelectedGranularity = function (selectedField) {
                scope.granularityMenuIsOpen = !scope.granularityMenuIsOpen;
                if (scope.selectedGranularity !== selectedField) {
                    scope.selectedGranularity = selectedField;
                    scope.clockMoveData = clockJawboneService
                        .transformMoveTicksData(
                            scope.moveTicks.data.items,
                            scope.selectedGranularity.value
                        );
                    updateClockMoveEvents(scope);
                    drawClockMoveEvents(scope);
                }
            };
        }

        return {
            restrict: 'E',
            templateUrl: function(elem, attr){
                return attr.preview ? 'views/clock-preview.html' : 'views/clock-view.html'
            },
            scope: {
                moveList: '=',
                moveTicks: '=',
                sleepList: '=',
                sleepTicks: '=',
                dateList: '=',
                selectedDateIndex: '='
            },
            link: link
        };
    }]);