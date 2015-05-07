'use strict';

angular.module('mealApp')
    .factory('mealJawboneService', function () {
        // Service logic
        var instance = {};

        /**
         * Preprocess jawbone records and filter out other records.
         * 
         * Adds a date field as well as a type field to a record.
         * The date is given in the granularity of days (without time).
         * The type is 'mealList'.
         * Finally, the data is a top-level node.
         */
        instance.preprocessRecord = function(record) {
            var data = {},
            type = null,
            date = null;

            // remove the meta information and bring the data node to the top level
            if (_.has(record, 'data') && _.has(record.data, 'data') ) {
                data = record.data.data;
            }

            // set the type of the record
            if (_.has(data, 'items')) {
                if (_.isArray(data.items) && data.items.length > 0) {
                    if (_.has(data.items[0], 'details') && _.has(data.items[0].details, 'carbohydrate')) {
                        type = 'mealList';
                    }
                }
            }

            // set the date of the record (without time)
            if (type === 'mealList') {
                if (_.has(data.items[0], 'time_created')) {
                    date = new Date(data.items[0].time_created * 1000);
                    // round to midnight
                    date = new Date(date.getFullYear(), date.getMonth(), date.getDate());
                }
            }

            // If preprocessing was successful, return the record, otherwise null
            if (_.isEmpty(data) || !type || !date) {
                return null;
            } else {
                record.data = data;
                record.type = type;
                record.date = date;
                return record;
            }
        }

        /**
         * This function transforms the meal data into the format to be used
         * by the visualization widget.
         */
        instance.transformMealListData = function(mealList) {
            var meals = [],
                mealItems = mealList.data.items;

            if (mealItems.length < 1) {
                return meals;
            }

            var mealSummary = {
                menuText: 'Summary',
                time: mealItems[0].time_created, //jshint ignore:line
                calories: 0,
                numDrinks: 0,
                numFoods: 0,
                note: 'Summary',
                details: {
                    calcium: 0,
                    carbohydrate: 0,
                    cholesterol: 0,
                    fiber: 0,
                    protein: 0,
                    saturatedFat: 0,
                    sodium: 0,
                    sugar: 0,
                    unsaturatedFat: 0
                }
            };

            _.each(mealItems, function (element, index) {
                var meal = {
                    menuText: 'Meal #' + (index + 1),
                    time: element.time_created, //jshint ignore:line
                    calories: parseFloat(element.details.calories).toFixed(2),
                    numDrinks: element.details.num_drinks, //jshint ignore:line
                    numFoods: element.details.num_foods, //jshint ignore:line
                    note: element.note,
                    details: {
                        calcium: element.details.calcium,
                        carbohydrate: element.details.carbohydrate,
                        cholesterol: element.details.cholesterol,
                        fiber: element.details.fiber,
                        protein: element.details.protein,
                        saturatedFat: element.details.saturated_fat, //jshint ignore:line
                        sodium: element.details.sodium,
                        sugar: element.details.sugar,
                        unsaturatedFat: element.details.unsaturated_fat //jshint ignore:line
                    },
                };

                meals.push(meal);

                mealSummary.calories += meal.calories;
                mealSummary.numDrinks += meal.numDrinks;
                mealSummary.numFoods += meal.numFoods;
                _.each(mealSummary.details, function (value, key) {
                    mealSummary.details[key] += meal.details[key];
                });
            });

            mealSummary.calories = parseFloat(mealSummary.calories).toFixed(2);
            meals.push(mealSummary);

            return  _.map(meals, function (value) {
                value.calories = {
                    value: value.calories,
                    text: 'Calories',
                    id: 'calories'
                };
                value.numDrinks = {
                    value: value.numDrinks,
                    text: 'Number of Drinks',
                    id: 'num-drinks'
                };
                value.numFoods = {
                    value: value.numFoods,
                    text: 'Number of Foods',
                    id: 'num-foods'
                };

                var details = value.details,
                    detailsList = [
                        {
                            value: details.calcium.toFixed(2),
                            unit: 'mg',
                            text: 'Calcium',
                            recommended: parseFloat(800).toFixed(2),
                            id: 'calcium'
                        },
                        {
                            value: details.carbohydrate.toFixed(2),
                            unit: 'g',
                            text: 'Carbohydrate',
                            recommended: parseFloat(100).toFixed(2),
                            id: 'carbohydrate'
                        },
                        {
                            value: details.cholesterol.toFixed(2),
                            unit: 'mg',
                            text: 'Cholesterol',
                            recommended: parseFloat(300).toFixed(2),
                            id: 'cholesterol'
                        },
                        {
                            value: details.fiber.toFixed(2),
                            unit: 'g',
                            text: 'Fiber',
                            recommended: parseFloat(40).toFixed(2),
                            id: 'fiber'
                        },
                        {
                            value: details.protein.toFixed(2),
                            unit: 'g',
                            text: 'Protein',
                            recommended: parseFloat(70).toFixed(2),
                            id: 'protein'
                        },
                        {
                            value: details.saturatedFat.toFixed(2),
                            unit: 'g',
                            text: 'Saturated Fat',
                            recommended: parseFloat(20).toFixed(2),
                            id: 'saturated-fat'
                        },
                        {
                            value: details.sodium.toFixed(2),
                            unit: 'mg',
                            text: 'Sodium',
                            recommended: parseFloat(2000).toFixed(2),
                            id: 'sodium'
                        },
                        {
                            value: details.sugar.toFixed(2),
                            unit: 'g',
                            text: 'Sugar',
                            recommended: parseFloat(85).toFixed(2),
                            id: 'sugar'
                        },
                        {
                            value: details.unsaturatedFat.toFixed(2),
                            unit: 'g',
                            text: 'Unsaturated Fat',
                            recommended: parseFloat(30).toFixed(2),
                            id: 'unsaturated-fat'
                        }
                    ];

                detailsList = _.sortBy(detailsList, function (element) {
                    return element.value / element.recommended;
                }).reverse();

                _.each(detailsList, function (value, index) {
                    value.index = index;
                });

                value.detailsList = detailsList;

                return value;
            });
        };

        return instance;
    });
