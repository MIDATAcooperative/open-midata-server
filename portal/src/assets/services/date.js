// service to handle dates
var date = angular.module('date', []);
date.factory('dateService', function() {
	// string format: YEAR-MONTH-DAY (YYYY-MM-DD)
	// date format: JavaScript Date
	
	// get current timestamp
	now = function() {
		return new Date();
	};
	
	// convert string to date
	stringToDate = function(string) {
		var split = _.map(string.split("-"), function(num) { return Number(num); });
		return new Date(split[0], split[1] - 1, split[2]);
	};
	
	// convert date to string
	dateToString = function(date) {
		var year = date.getFullYear();
		var month = ((date.getMonth() < 9) ? "0" : "") + (date.getMonth() + 1);
		var day = ((date.getDate() < 10) ? "0" : "") + date.getDate();
		return year + "-" + month + "-" + day;
	};
	
	return {
		now: now,
		toDate: stringToDate,
		toString: dateToString
	};
	
});