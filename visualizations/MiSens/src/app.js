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
 * Die Zahlen wurden rounded zu Integer weil ein Wert kamm mit mehr decimalen als in der Tabelle (# isovaleric-acid, Tabelle: 7742.652614 = 3, Record:7742.652614000002)
 */
MiSens.factory('information', ['$http', '$translate', 'midataServer', '$q', function ($http, $translate, midataServer, $q) {
	var result = {};

	result.datasetGroup = "scientifica";
	result.records = [];
	result.getInformationFinish = false;

	result.DownloadRecordsFromPortal = function(authToken){
		var query = { "format" : "fhir/QuestionnaireResponse" };
		return midataServer.fhirSearch(authToken, "QuestionnaireResponse", null).then(function(results){

			if (results.data && results.data.entry && Array.isArray(results.data.entry)) {
				results.data.entry.sort(function (a, b) {
					var _a = new Date(a.resource.meta.lastUpdated);
					var _b = new Date(b.resource.meta.lastUpdated);
					return _a.getTime() - _b.getTime();
				});
	
				//for (var index = 0; index < results.data.entry.length; index++) {
				//	var element = results.data.entry[index];
				//	console.log(index + ": " + element.resource.meta.lastUpdated);
				//}

				result.records = results.data.entry;
			}
		}, function(err){
			console.log("Failed to download records: " + err.data);
		});
	};

	result.GetInformationForVisualization = function(){
		var toReturn = {};
		for (var i = 0; result.records && i < result.records.length; i++) {
			var record = result.records[i].resource;

			if (result.datasetGroup == "scientifica" && (new Date(record.meta.lastUpdated)).getTime() >= (new Date(2017,9-1,3,18,0,0,0)) ) {
				continue;
			}

			for (var j = 0; record.item && j < record.item.length; j++) {
				var _item1 = record.item[j];
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
							 ) {
								 if (_item2.answer && _item2.answer[0] && _item2.answer[0].valueDecimal != null) {
									toReturn[substanceFound] = _item2.answer[0].valueDecimal;	
									break; 
								 }
						}
					}

					break;
				}
			}
		}

		return toReturn;
	};

	return result;
}]);