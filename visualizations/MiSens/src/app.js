var MiSens = angular.module('MiSens', ['midata', 'pascalprecht.translate', 'MiSensi18n']);
// Configuration
MiSens.config(['$translateProvider', 'i18nc', function ($translateProvider, i18nc) {

	$translateProvider
		.useSanitizeValueStrategy('escape')
		.registerAvailableLanguageKeys(['en', 'de', 'it', 'fr'], {
			'en_*': 'en',
			'de_*': 'de',
			'fr_*': 'fr',
			'it_*': 'it',
		})
		.translations('en', i18nc.en)
		.translations('de', i18nc.de)
		.translations('it', i18nc.it)
		.translations('fr', i18nc.fr)
		.fallbackLanguage('de');

}]);

/**
 * Ich gehe davon aus dass
 * - jede einzelne substance nur ein mal pro user vor kommt (ex. prop kommt nicht 2 mal)
 * 
 * Ich habe extra die Zahlen rounded zu Integer weil ein Wert kamm mit mehr decimalen als in der Tabelle (# isovaleric-acid, Tabelle: 7742.652614 = 3, Record:7742.652614000002)
 */
MiSens.factory('information', ['$http', '$translate', 'midataServer', '$q', function ($http, $translate, midataServer, $q) {
	var result = {};
	result.getInformation = function(authToken){
		var toReturn = {};
		var query = { "format" : "fhir/QuestionnaireResponse" };
		///return midataServer.getRecords(authToken, query, ["name", "created", "content", "data", "owner", "ownerName", "version"]).then(function(results){
		return midataServer.fhirSearch(authToken, "QuestionnaireResponse", null).then(function(results){
			
			//for (var i = 0; i < results.data.length; i++) {
			//var _data = results.data[i];
			for (var i = 0; i < results.data.entry.length; i++) {
				var _data = results.data.entry[i].resource;
	
				for (var j = 0; _data.item && j < _data.item.length; j++) {
					var _item1 = _data.item[j];
					var substanceFound = null;
					var k, _item2;
					if (_item1.item) {
						for (k = 0; k < _item1.item.length; k++) {
							_item2 = _item1.item[k];
							
							if (_item2.linkId == 'substance' && _item2.answer) {
								substanceFound = _item2.answer[0].valueString;
								break;
							}
						}
					}
	
					if (substanceFound) {
						for (k = 0; k < _item1.item.length; k++) {
							_item2 = _item1.item[k];
							// if information found, save it in results
							if ((_item2.linkId.toLowerCase() == 'threshold' || _item2.linkId.toLowerCase() == 'intensity')
									// && 
									//(substanceFound == 'beta-ionone' || substanceFound == 'heptanone' || substanceFound == 'isobuteryl-aldehyde' || substanceFound == 'isovaleci-acid' || substanceFound == 'rotundone' || substanceFound == 'sucrose' || substanceFound == 'nacl' || substanceFound == 'prop')
								 ) {
									 if (_item2.answer && _item2.answer[0] && _item.answer[0].valueDecimal) {
										toReturn[substanceFound] = _item2.answer[0].valueDecimal;	
										break; 
									 }
							}
						}
	
						// information of current record saved
						// next record
						break;
					}
				}
			}
			return toReturn;
		}, function(err){
			console.log("Failed to load records: " + err.data);
		});
	};

	return result;
}]);