var filters = angular.module('filters', ['date']);
filters.factory('filterData', function() {
	// service to share data between the record filter and the filter service
	var context = null;
	var filters = {};
	var circles = {};
	
	initData = function(_context, _serviceId) {
		this.context = _context;
		filters[_serviceId] = {};
		filters[_serviceId].current = [];
	}
	
	return {
		context: context,
		filters: filters,
		circles: circles,
		initData: initData
	}
	
});
filters.filter('recordFilter', ['filterData', function(filterData) {
	// custom filter for AngularJS
	return function(records, serviceId) {
		if (!filterData.filters[serviceId]) {
			return [];
		} else {
			return _.filter(records, function(record) {
				return _.every(filterData.filters[serviceId].current, function(filter) {
					if (!filter.property) {
						return true;
					} else if (filter.property.type === "point" && filter.value) {
						if (filter.operator.value === "is") { // equality
							return record[filter.property.name].$oid === filter.value._id.$oid;
						} else if (filter.operator.value === "isnt") { // inequality
							return record[filter.property.name].$oid !== filter.value._id.$oid;
						} else if (filter.operator.value === "incircle") { // circle membership
							if (filterData.circles[filter.value._id.$oid]) {
								return _.some(filterData.circles[filter.value._id.$oid].members, 
										function(member) { return record[filter.property.name].$oid === member.$oid; });
							}
						} else if (filter.operator.value === "notincircle") { // exclusion from circle
							if (filterData.circles[filter.value._id.$oid]) {
								return !_.some(filterData.circles[filter.value._id.$oid].members, 
										function(member) { return record[filter.property.name].$oid === member.$oid; });
							}
						}
					} else if (filter.sliderReady && filter.property.type === "range") {
						return filter.from.value <= record[filter.property.name].value && record[filter.property.name].value <= filter.to.value;
					}
					return true;
				});
			});
		}
	}
}]);
filters.factory('filterService', ['$rootScope', '$http', '$q', '$timeout', 'dateService', 'filterData', 
                                  function($rootScope, $http, $q, $timeout, dateService, filterData) {
	// main service with a generic record filter
	
	// init
	var error = null;
	var records = [];
	var userId = null;
	var onChangeFunction = null;
	var circles = [];
	
	// initialize the service
	initService = function(_context, _serviceId, _records, _userId, _onChangeFunction) {
		filterData.initData(_context, _serviceId);
		records = _records;
		userId = _userId;
		onChangeFunction = _onChangeFunction;
	}
	
	// initialize filters once
	initFilters = function(serviceId) {
		if (!filterData.filters[serviceId].properties) {
			// point operators
			filterData.filters[serviceId].operators = {
					"is": {"value": "is", "name": "is"},
					"isnt": {"value": "isnt", "name": "isn't"},
					"incircle": {"value": "incircle", "name": "is in circle"},
					"notincircle": {"value": "notincircle", "name": "isn't in circle"}
			};
			var operators = filterData.filters[serviceId].operators;
			
			// promises
			filterData.filters[serviceId].promises = {
					"app": getPropertyValues("app", jsRoutes.controllers.Apps.get().url),
					"owner": getPropertyValues("owner", jsRoutes.controllers.Users.get().url),
					"creator": getPropertyValues("creator", jsRoutes.controllers.Users.get().url),
					"circle": getCircles()
			};
			var promises = filterData.filters[serviceId].promises;
			
			// initialize properties; expose promises, so other components can also depend on their resolution
			filterData.filters[serviceId].properties = {
					"app": {"name": "app", "type": "point", "operators": [operators.is, operators.isnt], values: {}},
					"owner": {"name": "owner", "type": "point", "operators": [operators.is, operators.isnt, operators.incircle, operators.notincircle], values: {}},
					"creator": {"name": "creator", "type": "point", "operators": [operators.is, operators.isnt, operators.incircle, operators.notincircle], values: {}},
					"created": {"name": "created", "type": "range", "from": getMin("created"), "to": getMax("created")}
			};
			var properties = filterData.filters[serviceId].properties;
			
			// subscriptions for promises to resolve
			var subscriptions = {
					"app": [{"property": properties.app, "operators": [operators.is, operators.isnt]}],
					"owner": [{"property": properties.owner, "operators": [operators.is, operators.isnt]}],
					"creator": [{"property": properties.creator, "operators": [operators.is, operators.isnt]}],
					"circle": [{"property": properties.owner, "operators": [operators.incircle, operators.notincircle]},
					           {"property": properties.creator, "operators": [operators.incircle, operators.notincircle]}]
			};
			
			// resolves promises by setting the values for the operators for each subscription
			var resolvePromise = function(type, values) {
				_.each(subscriptions[type], function(subscription) { 
					_.each(subscription.operators, function(operator) {
						subscription.property.values[operator.value] = values;
					});
				});
			}
			
			// actually resolve promises
			promises.app.then(function(apps) { resolvePromise("app", apps); }, function(err) { error = err; });
			promises.owner.then(function(owners) { resolvePromise("owner", renameCurUser(owners)); }, function(err) { error = err; });
			promises.creator.then(function(creators) { resolvePromise("creator", renameCurUser(creators)); }, function(err) { error = err; });
			promises.circle.then(function(circles) {
				resolvePromise("circle", circles);
				_.each(circles, function(circle) { filterData.circles[circle._id.$oid] = circle; });
			}, function(err) { error = err; });
		}
	}
	
	// get the min of all values of a property with a 'value' field
	getMin = function(property) {
		return _.min(records, function(record) { return record[property].value; })[property];
	}
	
	// get the max of all values of a property with a 'value' field
	getMax = function(property) {
		return _.max(records, function(record) { return record[property].value; })[property];
	}
	
	// get the values of a property, fetch the name from the given url and sort by it
	getPropertyValues = function(property, url) {
		var deferred = $q.defer();
		var ids = _.uniq(_.pluck(records, property), false, function(value) { return value.$oid; });
		var data = {"properties": {"_id": ids}, "fields": ["name"]};
		$http.post(url, JSON.stringify(data)).
			success(function(values) { deferred.resolve(_.sortBy(values, "name")); }).
			error(function(err) { deferred.reject("Failed to load filter values for property '" + property + "': " + err); });
		return deferred.promise;
	}
	
	// get the user's circles and sort them by name
	getCircles = function() {
		var deferred = $q.defer();
		var data = {"owner": userId.$oid};
		$http.post(jsRoutes.controllers.Circles.get().url, JSON.stringify(data)).
			success(function(circles) { deferred.resolve(_.sortBy(circles, "name")); }).
			error(function(err) { deferred.reject("Failed to load user's circles: " + err); });
		return deferred.promise;
	};
	
	// get current user (if present), rename as "myself" and put on top of the list
	renameCurUser = function(users) {
		var curUser = _.find(users, function(user) { return user._id.$oid === userId.$oid; });
		if (curUser) {
			curUser.name = "myself";
			users = _.union([curUser], _.without(users, curUser));
		}
		return users;
	}
	
	// add a new filter
	addFilter = function(serviceId) {
		initFilters(serviceId);
		var newFilter = {"id": _.uniqueId("filter"), "context": filterData.context, "serviceId": serviceId};
		filterData.filters[serviceId].current.push(newFilter);
		// call init slider after view has been updated (i.e., after $digest cycle finished)
		$timeout(function() { initSlider(newFilter); });
	}
	
	// initialize slider (cannot be done in 'addFilter' since id of HTML slider element must be updated first)
	initSlider = function(filter) {
		if (!filter.sliderReady) {
			// hard code this metric and property for now
			var day = 1000 * 60 * 60 * 24;
			var property = filterData.filters[filter.serviceId].properties.created;
			filter.from = property.from;
			filter.to = property.to;
			$("#" + filter.context + "-" + filter.serviceId).find("#" + filter.id).slider({range: true, min: filter.from.value.getTime(),
				max: filter.to.value.getTime(), step: day, values: [filter.from.value.getTime(), filter.to.value.getTime()],
				slide: function(event, ui){ sliderChanged(event, ui, filter); }});
			filter.sliderReady = true;
		}
	}
	
	// slider value changed
	sliderChanged = function(event, ui, filter) {
		$rootScope.$apply(function(){
			filter.from = {"name": dateService.toString(new Date(ui.values[0])), "value": new Date(ui.values[0])};
			filter.to = {"name": dateService.toString(new Date(ui.values[1])), "value": new Date(ui.values[1])};
		});
		onChangeFunction(filter.serviceId);
	}
	
	// set slider to given value
	setSlider = function(filter, from, to) {
		$timeout(function() {
			// if slider is not ready yet, timeout again
			if (!filter.sliderReady) {
				setSlider(filter, from, to);
			} else {
				$rootScope.$apply(function() {
					filter.from = from;
					filter.to = to;
				});
				$("#" + filter.context + "-" + filter.serviceId).find("#" + filter.id).slider("option", "values", [from.value, to.value]);
			}
		});
	}
	
	// remove a filter
	removeFilter = function(filter) {
		filterData.filters[filter.serviceId].current = _.without(filterData.filters[filter.serviceId].current, filter);
		onChangeFunction(filter.serviceId);
	}
	
	// api of the filter service
	return {
		// data structures
		filters: filterData.filters,
		error: error,
		// functions
		init: initService,
		addFilter: addFilter,
		removeFilter: removeFilter,
		setSlider: setSlider
	}
}]);