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
	// string format: YEAR-MONTH-DAY (YYYY-MM-DD)
	// date format: JavaScript Date
	
	// get current timestamp
	var now = function() {
		return new Date();
	};
	
	// convert string to date
	var stringToDate = function(string) {
		var split = _.map(string.split("-"), function(num) { return Number(num); });
		return new Date(split[0], split[1] - 1, split[2]);
	};
	
	// convert date to string
	var dateToString = function(date) {
		var year = date.getFullYear();
		var month = ((date.getMonth() < 9) ? "0" : "") + (date.getMonth() + 1);
		var day = ((date.getDate() < 10) ? "0" : "") + date.getDate();
		return year + "-" + month + "-" + day;
	};
	
	var isValidDate = function(day,month,year) {
        if (day <= 0 || day > 31) return false;
        if (month <=0 || month > 12) return false;
        if (year <= 1900 || year > 3000) return false;
        month = month - 1; // Zero based in javascript
        var composedDate = new Date(year, month, day);
        console.log(composedDate);
        console.log(day);
        console.log(composedDate.getDate());
        return composedDate.getDate() == day &&
                composedDate.getMonth() == month &&
                composedDate.getFullYear() == year;
     }; 
	
	export default {
		now: now,
		toDate: stringToDate,
		toString: dateToString,
		isValidDate : isValidDate
	};
	