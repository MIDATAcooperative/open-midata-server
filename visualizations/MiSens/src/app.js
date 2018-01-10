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
/*
			//results.data = {};
			results.data.entry = [/** /{
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac5caafc8d45945ef3065e/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac5caafc8d45945ef3065e",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59abd51efc8d45945ef2fbf7",
										"display": "Part. LSIM-EH3A"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2018-09-13T21:48:58.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59abd51efc8d45945ef2fbf7",
							"display": "Part. LSIM-EH3A"
						},
						"authored": "2017-09-03T19:48:57.346Z",
						"source": {
							"reference": "Patient/59abd51efc8d45945ef2fbf3"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "isobuteryl-aldehyde"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 0
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?"
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac5caafc8d45945ef3065e/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac5caafc8d45945ef3065e",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59abd51efc8d45945ef2fbf7",
										"display": "Part. LSIM-EH3A"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2018-09-23T21:48:58.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59abd51efc8d45945ef2fbf7",
							"display": "Part. LSIM-EH3A"
						},
						"authored": "2017-09-03T19:48:57.346Z",
						"source": {
							"reference": "Patient/59abd51efc8d45945ef2fbf3"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "isobuteryl-aldehyde"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 7742.652614
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?"
							}]
						}]
					}
				},{
					"fullUrl": "https://ch.midata.coop/fhir/Observation/59ac3973fc8d45945ef30652/_history/0",
					"resource": {
						"resourceType": "Observation",
						"id": "59ac3973fc8d45945ef30652",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59abf9c1fc8d45945ef3004a",
										"display": "Part. CVUG-DOPK"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T19:18:43.000+02:00"
						},
						"status": "preliminary",
						"category": [{
							"coding": [{
								"system": "http://hl7.org/fhir/medication-statement-category",
								"code": "patientspecified",
								"display": "Patient Specified"
							}],
							"text": "Patient Specified"
						}],
						"code": {
							"coding": [{
								"system": "http://loinc.org",
								"code": "63582-1",
								"display": "loinc/63582-1"
							}]
						},
						"subject": {
							"reference": "Patient/59abf9c1fc8d45945ef3004a",
							"display": "Part. CVUG-DOPK"
						},
						"effectiveDateTime": "2017-09-03T17:18:43.725Z",
						"valueString": "Not at all"
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac3050fc8d45945ef30651/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac3050fc8d45945ef30651",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59abf0a6fc8d45945ef2fedd",
										"display": "Part. VB9M-A865"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:39:44.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensQuestionnaire",
								"display": "MiSens General Questionnaire"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59abf0a6fc8d45945ef2fedd",
							"display": "Part. VB9M-A865"
						},
						"authored": "2017-09-03T16:39:43.633Z",
						"source": {
							"reference": "Patient/59abf0a6fc8d45945ef2fed9"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "general-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Questions about your dental health: Past dental treatments may influence your taste perception.",
							"item": [{
								"linkId": "teeth_canal_theraphy",
								"text": "Number of teeth with root canal treatment.",
								"answer": [{
									"valueDecimal": 0
								}]
							}, {
								"linkId": "removed_teeth",
								"text": "Number of extracted teeth (e.g. wisdom teeth)",
								"answer": [{
									"valueDecimal": 12
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Questions about your attitudes to certain foods: Please use the 9-point scales to indicate your agreement with the following statements.",
							"item": [{
								"linkId": "broccoli_taste",
								"text": "I enjoy and frequently eat broccoli ",
								"answer": [{
									"valueInteger": 7
								}]
							}, {
								"linkId": "broccoli_bitter",
								"text": "Broccoli tastes very bitter to me",
								"answer": [{
									"valueInteger": 1
								}]
							}, {
								"linkId": "brussels_taste",
								"text": "I enjoy and frequently eat brussel sprouts",
								"answer": [{
									"valueInteger": 5
								}]
							}, {
								"linkId": "brussels_bitter",
								"text": "Brussel sprouts taste very bitter to me",
								"answer": [{
									"valueInteger": 1
								}]
							}]
						}, {
							"linkId": "3",
							"text": "Unpleasant foods: Are there any other foods or drinks that taste very bitter to you and that you like/dislike for this reason?",
							"item": [{
								"linkId": "foodstuff_1",
								"text": "Food"
							}, {
								"linkId": "feeling_1",
								"text": "taste"
							}, {
								"linkId": "foodstuff_2",
								"text": "Food"
							}, {
								"linkId": "feeling_2",
								"text": "taste"
							}, {
								"linkId": "foodstuff_3",
								"text": "Food"
							}, {
								"linkId": "feeling_3",
								"text": "taste"
							}, {
								"linkId": "foodstuff_4",
								"text": "Food"
							}, {
								"linkId": "feeling_4",
								"text": "taste"
							}, {
								"linkId": "foodstuff_5",
								"text": "Food"
							}, {
								"linkId": "feeling_5",
								"text": "taste"
							}, {
								"linkId": "foodstuff_6",
								"text": "Food"
							}, {
								"linkId": "feeling_6",
								"text": "taste"
							}]
						}, {
							"linkId": "4",
							"text": "Personal aroma perception: Are there certain tastes or smells, which you percieve as much more or less intense than other people.",
							"item": [{
								"linkId": "foodstuff_7",
								"text": "Food"
							}, {
								"linkId": "feeling_7",
								"text": "taste"
							}, {
								"linkId": "foodstuff_8",
								"text": "Food"
							}, {
								"linkId": "feeling_8",
								"text": "taste"
							}, {
								"linkId": "foodstuff_9",
								"text": "Food"
							}, {
								"linkId": "feeling_9",
								"text": "taste"
							}, {
								"linkId": "foodstuff_10",
								"text": "Food"
							}, {
								"linkId": "feeling_10",
								"text": "taste"
							}, {
								"linkId": "foodstuff_11",
								"text": "Food"
							}, {
								"linkId": "feeling_11",
								"text": "taste"
							}, {
								"linkId": "foodstuff_12",
								"text": "Food"
							}, {
								"linkId": "feeling_12",
								"text": "taste"
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac2fa5fc8d45945ef30650/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac2fa5fc8d45945ef30650",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac2e3afc8d45945ef30640",
										"display": "Part. 69G6-V4H4"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:36:53.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac2e3afc8d45945ef30640",
							"display": "Part. 69G6-V4H4"
						},
						"authored": "2017-09-03T16:36:53.859Z",
						"source": {
							"reference": "Patient/59ac2e3afc8d45945ef3063c"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 6.842275515895148
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 483
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPhone9,3 (iOS, 10.3.3)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "sucrose"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 26.884057971014485
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "sweet"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac2f98fc8d45945ef3064f/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac2f98fc8d45945ef3064f",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac2e3afc8d45945ef30640",
										"display": "Part. 69G6-V4H4"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:36:40.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac2e3afc8d45945ef30640",
							"display": "Part. 69G6-V4H4"
						},
						"authored": "2017-09-03T16:36:40.127Z",
						"source": {
							"reference": "Patient/59ac2e3afc8d45945ef3063c"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 6.842275515895148
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 483
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPhone9,3 (iOS, 10.3.3)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "nacl"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 15.49689440993788
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "salty"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac2f82fc8d45945ef3064c/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac2f82fc8d45945ef3064c",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac2e3afc8d45945ef30640",
										"display": "Part. 69G6-V4H4"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2018-09-29T18:36:18.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac2e3afc8d45945ef30640",
							"display": "Part. 69G6-V4H4"
						},
						"authored": "2017-09-03T16:36:18.324Z",
						"source": {
							"reference": "Patient/59ac2e3afc8d45945ef3063c"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 6.842275515895148
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 483
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPhone9,3 (iOS, 10.3.3)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "prop"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 0
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "bitter"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac2f82fc8d45945ef3064c/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac2f82fc8d45945ef3064c",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac2e3afc8d45945ef30640",
										"display": "Part. 69G6-V4H4"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2018-09-13T18:36:18.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac2e3afc8d45945ef30640",
							"display": "Part. 69G6-V4H4"
						},
						"authored": "2017-09-03T16:36:18.324Z",
						"source": {
							"reference": "Patient/59ac2e3afc8d45945ef3063c"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 6.842275515895148
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 483
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPhone9,3 (iOS, 10.3.3)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "prop"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 100
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "bitter"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac2f64fc8d45945ef3064b/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac2f64fc8d45945ef3064b",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac2e3afc8d45945ef30640",
										"display": "Part. 69G6-V4H4"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:35:48.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac2e3afc8d45945ef30640",
							"display": "Part. 69G6-V4H4"
						},
						"authored": "2017-09-03T16:35:48.838Z",
						"source": {
							"reference": "Patient/59ac2e3afc8d45945ef3063c"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "rotundone"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 2.154441279
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "woody"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac2f4ffc8d45945ef3064a/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac2f4ffc8d45945ef3064a",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac2e3afc8d45945ef30640",
										"display": "Part. 69G6-V4H4"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:35:27.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac2e3afc8d45945ef30640",
							"display": "Part. 69G6-V4H4"
						},
						"authored": "2018-09-03T16:35:27.537Z",
						"source": {
							"reference": "Patient/59ac2e3afc8d45945ef3063c"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "beta-ionone"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 2.156348848
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "flowery"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac2f40fc8d45945ef30649/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac2f40fc8d45945ef30649",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac2e3afc8d45945ef30640",
										"display": "Part. 69G6-V4H4"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:35:12.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac2e3afc8d45945ef30640",
							"display": "Part. 69G6-V4H4"
						},
						"authored": "2017-09-03T16:35:12.867Z",
						"source": {
							"reference": "Patient/59ac2e3afc8d45945ef3063c"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "isovaleci-acid"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 599.486695
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "decay"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac2f32fc8d45945ef30648/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac2f32fc8d45945ef30648",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac2e3afc8d45945ef30640",
										"display": "Part. 69G6-V4H4"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:34:58.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac2e3afc8d45945ef30640",
							"display": "Part. 69G6-V4H4"
						},
						"authored": "2017-09-03T16:34:58.068Z",
						"source": {
							"reference": "Patient/59ac2e3afc8d45945ef3063c"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "heptanone"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 599.486695
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "decay"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac2f10fc8d45945ef30645/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac2f10fc8d45945ef30645",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac2e3afc8d45945ef30640",
										"display": "Part. 69G6-V4H4"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:34:24.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac2e3afc8d45945ef30640",
							"display": "Part. 69G6-V4H4"
						},
						"authored": "2017-09-03T16:34:24.700Z",
						"source": {
							"reference": "Patient/59ac2e3afc8d45945ef3063c"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "isobuteryl-aldehyde"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 7742.652614
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "chemical"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/Patient/59ac2e3afc8d45945ef30642/_history/0",
					"resource": {
						"resourceType": "Patient",
						"id": "59ac2e3afc8d45945ef30640",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "portal",
										"display": "Midata Portal"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac2e3afc8d45945ef30640",
										"display": "Part. 69G6-V4H4"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:30:50.000+02:00"
						},
						"text": {
							"status": "generated",
							"div": "<div xmlns=\"http://www.w3.org/1999/xhtml\"><div class=\"hapiHeaderText\"/><table class=\"hapiPropertyTable\"><tbody><tr><td>Date of birth</td><td><span>01 Januar 1969</span></td></tr></tbody></table></div>"
						},
						"name": [{
							"text": "Part. 69G6-V4H4"
						}],
						"gender": "male",
						"birthDate": "1969-01-01"
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac29c3fc8d45945ef30634/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac29c3fc8d45945ef30634",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59aa8519fc8d45945ef2f10a",
										"display": "Part. CR02-CNKO"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:11:47.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59aa8519fc8d45945ef2f10a",
							"display": "Part. CR02-CNKO"
						},
						"authored": "2017-09-03T16:11:46.194Z",
						"source": {
							"reference": "Patient/59aa8518fc8d45945ef2f106"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 6.770571589043463
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 407
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPhone7,2 (iOS, 10.3.3)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "sucrose"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 24.459459459459453
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "Anderes"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac29c3fc8d45945ef30633/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac29c3fc8d45945ef30633",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59aa8519fc8d45945ef2f10a",
										"display": "Part. CR02-CNKO"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:11:47.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59aa8519fc8d45945ef2f10a",
							"display": "Part. CR02-CNKO"
						},
						"authored": "2017-09-03T16:11:44.127Z",
						"source": {
							"reference": "Patient/59aa8518fc8d45945ef2f106"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 6.770571589043463
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 407
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPhone7,2 (iOS, 10.3.3)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "sucrose"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 24.459459459459453
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "Anderes"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac29c3fc8d45945ef30632/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac29c3fc8d45945ef30632",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59aa8519fc8d45945ef2f10a",
										"display": "Part. CR02-CNKO"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:11:47.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59aa8519fc8d45945ef2f10a",
							"display": "Part. CR02-CNKO"
						},
						"authored": "2017-09-03T16:11:42.610Z",
						"source": {
							"reference": "Patient/59aa8518fc8d45945ef2f106"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 6.770571589043463
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 407
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPhone7,2 (iOS, 10.3.3)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "sucrose"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 24.459459459459453
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "Anderes"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac29c3fc8d45945ef30631/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac29c3fc8d45945ef30631",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59aa8519fc8d45945ef2f10a",
										"display": "Part. CR02-CNKO"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:11:47.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59aa8519fc8d45945ef2f10a",
							"display": "Part. CR02-CNKO"
						},
						"authored": "2017-09-03T16:11:41.322Z",
						"source": {
							"reference": "Patient/59aa8518fc8d45945ef2f106"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 6.770571589043463
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 407
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPhone7,2 (iOS, 10.3.3)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "sucrose"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 24.459459459459453
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "Anderes"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac29b3fc8d45945ef3062e/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac29b3fc8d45945ef3062e",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59aa8519fc8d45945ef2f10a",
										"display": "Part. CR02-CNKO"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:11:31.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59aa8519fc8d45945ef2f10a",
							"display": "Part. CR02-CNKO"
						},
						"authored": "2017-09-03T16:11:31.359Z",
						"source": {
							"reference": "Patient/59aa8518fc8d45945ef2f106"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 6.770571589043463
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 407
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPhone7,2 (iOS, 10.3.3)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "sucrose"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 24.459459459459453
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?"
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac29aefc8d45945ef3062d/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac29aefc8d45945ef3062d",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59aa8519fc8d45945ef2f10a",
										"display": "Part. CR02-CNKO"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:11:26.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59aa8519fc8d45945ef2f10a",
							"display": "Part. CR02-CNKO"
						},
						"authored": "2017-09-03T16:11:26.339Z",
						"source": {
							"reference": "Patient/59aa8518fc8d45945ef2f106"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 6.770571589043463
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 407
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPhone7,2 (iOS, 10.3.3)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "nacl"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 13.108108108108112
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "Anderes"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac29a1fc8d45945ef3062c/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac29a1fc8d45945ef3062c",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59aa8519fc8d45945ef2f10a",
										"display": "Part. CR02-CNKO"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:11:13.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59aa8519fc8d45945ef2f10a",
							"display": "Part. CR02-CNKO"
						},
						"authored": "2017-09-03T16:11:13.461Z",
						"source": {
							"reference": "Patient/59aa8518fc8d45945ef2f106"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 6.770571589043463
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 407
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPhone7,2 (iOS, 10.3.3)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "nacl"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 13.108108108108112
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?"
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac299afc8d45945ef30629/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac299afc8d45945ef30629",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59aa8519fc8d45945ef2f10a",
										"display": "Part. CR02-CNKO"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:11:06.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59aa8519fc8d45945ef2f10a",
							"display": "Part. CR02-CNKO"
						},
						"authored": "2017-09-03T16:11:05.580Z",
						"source": {
							"reference": "Patient/59aa8518fc8d45945ef2f106"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 6.770571589043463
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 407
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPhone7,2 (iOS, 10.3.3)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "prop"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 24.459459459459453
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "Anderes"
								}]
							}]
						}]
					}
				}, /** /{
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac2984fc8d45945ef30628/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac2984fc8d45945ef30628",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59aa8519fc8d45945ef2f10a",
										"display": "Part. CR02-CNKO"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:10:44.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59aa8519fc8d45945ef2f10a",
							"display": "Part. CR02-CNKO"
						},
						"authored": "2017-09-03T16:10:44.004Z",
						"source": {
							"reference": "Patient/59aa8518fc8d45945ef2f106"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "rotundone"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 0
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "Anderes"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac2977fc8d45945ef30627/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac2977fc8d45945ef30627",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59aa8519fc8d45945ef2f10a",
										"display": "Part. CR02-CNKO"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:10:31.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59aa8519fc8d45945ef2f10a",
							"display": "Part. CR02-CNKO"
						},
						"authored": "2017-09-03T16:10:30.941Z",
						"source": {
							"reference": "Patient/59aa8518fc8d45945ef2f106"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "beta-ionone"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 0
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "Anderes"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac296afc8d45945ef30626/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac296afc8d45945ef30626",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59aa8519fc8d45945ef2f10a",
										"display": "Part. CR02-CNKO"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:10:18.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59aa8519fc8d45945ef2f10a",
							"display": "Part. CR02-CNKO"
						},
						"authored": "2017-09-03T16:10:18.733Z",
						"source": {
							"reference": "Patient/59aa8518fc8d45945ef2f106"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "isovaleci-acid"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 0
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "Anderes"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac2961fc8d45945ef30625/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac2961fc8d45945ef30625",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59aa8519fc8d45945ef2f10a",
										"display": "Part. CR02-CNKO"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:10:09.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59aa8519fc8d45945ef2f10a",
							"display": "Part. CR02-CNKO"
						},
						"authored": "2017-09-03T16:10:09.712Z",
						"source": {
							"reference": "Patient/59aa8518fc8d45945ef2f106"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "isovaleci-acid"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 0
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?"
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac2957fc8d45945ef30624/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac2957fc8d45945ef30624",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59aa8519fc8d45945ef2f10a",
										"display": "Part. CR02-CNKO"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:09:59.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59aa8519fc8d45945ef2f10a",
							"display": "Part. CR02-CNKO"
						},
						"authored": "2017-09-03T16:09:58.671Z",
						"source": {
							"reference": "Patient/59aa8518fc8d45945ef2f106"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "heptanone"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 7742.652614
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "Anderes"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac293ffc8d45945ef30621/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac293ffc8d45945ef30621",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59aa8519fc8d45945ef2f10a",
										"display": "Part. CR02-CNKO"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T18:09:35.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59aa8519fc8d45945ef2f10a",
							"display": "Part. CR02-CNKO"
						},
						"authored": "2017-09-03T16:09:34.955Z",
						"source": {
							"reference": "Patient/59aa8518fc8d45945ef2f106"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "isobuteryl-aldehyde"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 27825.62239
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "chemical"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac2357fc8d45945ef3061e/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac2357fc8d45945ef3061e",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ab09d7fc8d45945ef2fa7a",
										"display": "Part. O86A-FJBM"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:44:23.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensQuestionnaire",
								"display": "MiSens General Questionnaire"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ab09d7fc8d45945ef2fa7a",
							"display": "Part. O86A-FJBM"
						},
						"authored": "2017-09-03T15:44:22.469Z",
						"source": {
							"reference": "Patient/59ab09d7fc8d45945ef2fa76"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "general-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Questions about your dental health: Past dental treatments may influence your taste perception.",
							"item": [{
								"linkId": "teeth_canal_theraphy",
								"text": "Number of teeth with root canal treatment.",
								"answer": [{
									"valueDecimal": 0
								}]
							}, {
								"linkId": "removed_teeth",
								"text": "Number of extracted teeth (e.g. wisdom teeth)",
								"answer": [{
									"valueDecimal": 4
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Questions about your attitudes to certain foods: Please use the 9-point scales to indicate your agreement with the following statements.",
							"item": [{
								"linkId": "broccoli_taste",
								"text": "I enjoy and frequently eat broccoli ",
								"answer": [{
									"valueInteger": 7
								}]
							}, {
								"linkId": "broccoli_bitter",
								"text": "Broccoli tastes very bitter to me",
								"answer": [{
									"valueInteger": 1
								}]
							}, {
								"linkId": "brussels_taste",
								"text": "I enjoy and frequently eat brussel sprouts",
								"answer": [{
									"valueInteger": 1
								}]
							}, {
								"linkId": "brussels_bitter",
								"text": "Brussel sprouts taste very bitter to me",
								"answer": [{
									"valueInteger": 1
								}]
							}]
						}, {
							"linkId": "3",
							"text": "Unpleasant foods: Are there any other foods or drinks that taste very bitter to you and that you like/dislike for this reason?",
							"item": [{
								"linkId": "foodstuff_1",
								"text": "Food",
								"answer": [{
									"valueString": "Grapefruit "
								}]
							}, {
								"linkId": "feeling_1",
								"text": "taste",
								"answer": [{
									"valueString": "tasty"
								}]
							}, {
								"linkId": "foodstuff_2",
								"text": "Food",
								"answer": [{
									"valueString": "Tonic water"
								}]
							}, {
								"linkId": "feeling_2",
								"text": "taste",
								"answer": [{
									"valueString": "tasty"
								}]
							}, {
								"linkId": "foodstuff_3",
								"text": "Food",
								"answer": [{
									"valueString": "Ciccorino rosso salat"
								}]
							}, {
								"linkId": "feeling_3",
								"text": "taste",
								"answer": [{
									"valueString": "nasty"
								}]
							}, {
								"linkId": "foodstuff_4",
								"text": "Food",
								"answer": [{
									"valueString": "Hopfen"
								}]
							}, {
								"linkId": "feeling_4",
								"text": "taste",
								"answer": [{
									"valueString": "nasty"
								}]
							}, {
								"linkId": "foodstuff_5",
								"text": "Food"
							}, {
								"linkId": "feeling_5",
								"text": "taste"
							}, {
								"linkId": "foodstuff_6",
								"text": "Food"
							}, {
								"linkId": "feeling_6",
								"text": "taste"
							}]
						}, {
							"linkId": "4",
							"text": "Personal aroma perception: Are there certain tastes or smells, which you percieve as much more or less intense than other people.",
							"item": [{
								"linkId": "foodstuff_7",
								"text": "Food"
							}, {
								"linkId": "feeling_7",
								"text": "taste"
							}, {
								"linkId": "foodstuff_8",
								"text": "Food"
							}, {
								"linkId": "feeling_8",
								"text": "taste"
							}, {
								"linkId": "foodstuff_9",
								"text": "Food"
							}, {
								"linkId": "feeling_9",
								"text": "taste"
							}, {
								"linkId": "foodstuff_10",
								"text": "Food"
							}, {
								"linkId": "feeling_10",
								"text": "taste"
							}, {
								"linkId": "foodstuff_11",
								"text": "Food"
							}, {
								"linkId": "feeling_11",
								"text": "taste"
							}, {
								"linkId": "foodstuff_12",
								"text": "Food"
							}, {
								"linkId": "feeling_12",
								"text": "taste"
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/Patient/59ac21fffc8d45945ef3061b/_history/0",
					"resource": {
						"resourceType": "Patient",
						"id": "59ac21fffc8d45945ef30619",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "portal",
										"display": "Midata Portal"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac21fffc8d45945ef30619",
										"display": "Part. 154B-OQEC"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:38:39.000+02:00"
						},
						"text": {
							"status": "generated",
							"div": "<div xmlns=\"http://www.w3.org/1999/xhtml\"><div class=\"hapiHeaderText\"/><table class=\"hapiPropertyTable\"><tbody><tr><td>Date of birth</td><td><span>01 Januar 1970</span></td></tr></tbody></table></div>"
						},
						"name": [{
							"text": "Part. 154B-OQEC"
						}],
						"gender": "female",
						"birthDate": "1970-01-01"
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac1febfc8d45945ef30613/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac1febfc8d45945ef30613",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac0cc0fc8d45945ef30340",
										"display": "Part. 2326-S70T"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:29:47.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac0cc0fc8d45945ef30340",
							"display": "Part. 2326-S70T"
						},
						"authored": "2017-09-03T15:29:47.617Z",
						"source": {
							"reference": "Patient/59ac0cc0fc8d45945ef3033c"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 0
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 407
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPhone8,4 (iOS, 10.1.1)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "sucrose"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 48.51351351351351
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?"
							}]
						}]
					}
				}, 
				{
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac1fc1fc8d45945ef30612/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac1fc1fc8d45945ef30612",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac0cc0fc8d45945ef30340",
										"display": "Part. 2326-S70T"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:29:05.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac0cc0fc8d45945ef30340",
							"display": "Part. 2326-S70T"
						},
						"authored": "2017-09-03T15:29:05.780Z",
						"source": {
							"reference": "Patient/59ac0cc0fc8d45945ef3033c"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 0
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 407
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPhone8,4 (iOS, 10.1.1)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "nacl"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 35.81081081081081
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?"
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac1fb8fc8d45945ef30611/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac1fb8fc8d45945ef30611",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac0ca4fc8d45945ef3031d",
										"display": "Part. B0O5-2OMT"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:28:56.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac0ca4fc8d45945ef3031d",
							"display": "Part. B0O5-2OMT"
						},
						"authored": "2017-09-03T15:28:56.537Z",
						"source": {
							"reference": "Patient/59ac0ca4fc8d45945ef30319"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 0
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 1023
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPad3,4 (iOS, 10.3.3)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "nacl"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 48.65591397849463
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?"
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac1f95fc8d45945ef3060e/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac1f95fc8d45945ef3060e",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac0cc0fc8d45945ef30340",
										"display": "Part. 2326-S70T"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:28:21.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac0cc0fc8d45945ef30340",
							"display": "Part. 2326-S70T"
						},
						"authored": "2017-09-03T15:28:21.121Z",
						"source": {
							"reference": "Patient/59ac0cc0fc8d45945ef3033c"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 0
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 407
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPhone8,4 (iOS, 10.1.1)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "sucrose"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 33.648648648648646
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?"
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac1f6efc8d45945ef3060b/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac1f6efc8d45945ef3060b",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac0ca4fc8d45945ef3031d",
										"display": "Part. B0O5-2OMT"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:27:42.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensTasteIntensity",
								"display": "Taste intensity on a GLMS (General Labeled Magnitude Scale) of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac0ca4fc8d45945ef3031d",
							"display": "Part. B0O5-2OMT"
						},
						"authored": "2017-09-03T15:27:42.476Z",
						"source": {
							"reference": "Patient/59ac0ca4fc8d45945ef30319"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "taste-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "General Information",
							"item": [{
								"linkId": "scale",
								"text": "What type of scala was used?",
								"answer": [{
									"valueString": "gLMS"
								}]
							}, {
								"linkId": "scale-size",
								"text": "How big was your scale? (in cm)",
								"answer": [{
									"valueDecimal": 0
								}]
							}, {
								"linkId": "scale-pixel",
								"text": "How big was your scale? (in px)",
								"answer": [{
									"valueDecimal": 1023
								}]
							}, {
								"linkId": "device",
								"text": "What device did you use?",
								"answer": [{
									"valueString": "Apple: iPad3,4 (iOS, 10.3.3)"
								}]
							}, {
								"linkId": "instructions",
								"text": "What inscructions did you follow?",
								"answer": [{
									"valueString": "As part of this study you will rate the intensity of your sensory perception on a scale of all possible sensory perceptions. The scale uses everyday expressions such as “weak” and “strong”. The upper end of the scale is the most intense sensory experience of any kind that you can imagine. When you make your rating, please use the expressions in the same way you would use them in everyday life. When you set your mark, you may use the spaces between the labels to best reflect the actual intensity you perceive."
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Taste Perception",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "sucrose"
								}]
							}, {
								"linkId": "intensity",
								"text": "What was your taste intensity on the scale? (in %, 0% no sensation, 100% strongest imaginable sensation)",
								"answer": [{
									"valueDecimal": 33.602150537634415
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?"
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac1f49fc8d45945ef3060a/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac1f49fc8d45945ef3060a",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac0cc0fc8d45945ef30340",
										"display": "Part. 2326-S70T"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:27:05.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac0cc0fc8d45945ef30340",
							"display": "Part. 2326-S70T"
						},
						"authored": "2017-09-03T15:27:05.061Z",
						"source": {
							"reference": "Patient/59ac0cc0fc8d45945ef3033c"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "isovaleci-acid"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 599.486695
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "pungent"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac1f30fc8d45945ef30609/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac1f30fc8d45945ef30609",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac0d63fc8d45945ef30386",
										"display": "Part. 8MOT-LTFN"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:26:40.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac0d63fc8d45945ef30386",
							"display": "Part. 8MOT-LTFN"
						},
						"authored": "2017-09-03T15:26:36.187Z",
						"source": {
							"reference": "Patient/59ac0d63fc8d45945ef30382"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "rotundone"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 0.599486695
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "Anderes"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac1ef2fc8d45945ef30608/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac1ef2fc8d45945ef30608",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac147afc8d45945ef303f7",
										"display": "Part. 6S96-HTRV"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:25:38.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensQuestionnaire",
								"display": "MiSens General Questionnaire"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac147afc8d45945ef303f7",
							"display": "Part. 6S96-HTRV"
						},
						"authored": "2017-09-03T15:25:35.256Z",
						"source": {
							"reference": "Patient/59ac147afc8d45945ef303f3"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "general-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Questions about your dental health: Past dental treatments may influence your taste perception.",
							"item": [{
								"linkId": "teeth_canal_theraphy",
								"text": "Number of teeth with root canal treatment.",
								"answer": [{
									"valueDecimal": 2
								}]
							}, {
								"linkId": "removed_teeth",
								"text": "Number of extracted teeth (e.g. wisdom teeth)",
								"answer": [{
									"valueDecimal": 4
								}]
							}]
						}, {
							"linkId": "2",
							"text": "Questions about your attitudes to certain foods: Please use the 9-point scales to indicate your agreement with the following statements.",
							"item": [{
								"linkId": "broccoli_taste",
								"text": "I enjoy and frequently eat broccoli ",
								"answer": [{
									"valueInteger": 8
								}]
							}, {
								"linkId": "broccoli_bitter",
								"text": "Broccoli tastes very bitter to me",
								"answer": [{
									"valueInteger": 2
								}]
							}, {
								"linkId": "brussels_taste",
								"text": "I enjoy and frequently eat brussel sprouts",
								"answer": [{
									"valueInteger": 8
								}]
							}, {
								"linkId": "brussels_bitter",
								"text": "Brussel sprouts taste very bitter to me",
								"answer": [{
									"valueInteger": 2
								}]
							}]
						}, {
							"linkId": "3",
							"text": "Unpleasant foods: Are there any other foods or drinks that taste very bitter to you and that you like/dislike for this reason?",
							"item": [{
								"linkId": "foodstuff_1",
								"text": "Food"
							}, {
								"linkId": "feeling_1",
								"text": "taste"
							}, {
								"linkId": "foodstuff_2",
								"text": "Food"
							}, {
								"linkId": "feeling_2",
								"text": "taste"
							}, {
								"linkId": "foodstuff_3",
								"text": "Food"
							}, {
								"linkId": "feeling_3",
								"text": "taste"
							}, {
								"linkId": "foodstuff_4",
								"text": "Food"
							}, {
								"linkId": "feeling_4",
								"text": "taste"
							}, {
								"linkId": "foodstuff_5",
								"text": "Food"
							}, {
								"linkId": "feeling_5",
								"text": "taste"
							}, {
								"linkId": "foodstuff_6",
								"text": "Food"
							}, {
								"linkId": "feeling_6",
								"text": "taste"
							}]
						}, {
							"linkId": "4",
							"text": "Personal aroma perception: Are there certain tastes or smells, which you percieve as much more or less intense than other people.",
							"item": [{
								"linkId": "foodstuff_7",
								"text": "Food"
							}, {
								"linkId": "feeling_7",
								"text": "taste"
							}, {
								"linkId": "foodstuff_8",
								"text": "Food"
							}, {
								"linkId": "feeling_8",
								"text": "taste"
							}, {
								"linkId": "foodstuff_9",
								"text": "Food"
							}, {
								"linkId": "feeling_9",
								"text": "taste"
							}, {
								"linkId": "foodstuff_10",
								"text": "Food"
							}, {
								"linkId": "feeling_10",
								"text": "taste"
							}, {
								"linkId": "foodstuff_11",
								"text": "Food"
							}, {
								"linkId": "feeling_11",
								"text": "taste"
							}, {
								"linkId": "foodstuff_12",
								"text": "Food"
							}, {
								"linkId": "feeling_12",
								"text": "taste"
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac1eb8fc8d45945ef30607/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac1eb8fc8d45945ef30607",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac147afc8d45945ef303f7",
										"display": "Part. 6S96-HTRV"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:24:40.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac147afc8d45945ef303f7",
							"display": "Part. 6S96-HTRV"
						},
						"authored": "2017-09-03T15:24:39.716Z",
						"source": {
							"reference": "Patient/59ac147afc8d45945ef303f3"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "beta-ionone"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 0
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "Anderes"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac1eb7fc8d45945ef30606/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac1eb7fc8d45945ef30606",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac147afc8d45945ef303f7",
										"display": "Part. 6S96-HTRV"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:24:39.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac147afc8d45945ef303f7",
							"display": "Part. 6S96-HTRV"
						},
						"authored": "2017-09-03T15:24:37.818Z",
						"source": {
							"reference": "Patient/59ac147afc8d45945ef303f3"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "beta-ionone"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 0
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "Anderes"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac1ea5fc8d45945ef30605/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac1ea5fc8d45945ef30605",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac0bf7fc8d45945ef302cd",
										"display": "Part. 4HET-BV25"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:24:21.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac0bf7fc8d45945ef302cd",
							"display": "Part. 4HET-BV25"
						},
						"authored": "2017-09-03T15:24:16.312Z",
						"source": {
							"reference": "Patient/59ac0bf7fc8d45945ef302c9"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "isobuteryl-aldehyde"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 0
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?"
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac1e91fc8d45945ef30604/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac1e91fc8d45945ef30604",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac0e1efc8d45945ef303c2",
										"display": "Part. 83GH-5SSI"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:24:01.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac0e1efc8d45945ef303c2",
							"display": "Part. 83GH-5SSI"
						},
						"authored": "2017-09-03T15:24:00.787Z",
						"source": {
							"reference": "Patient/59ac0e1efc8d45945ef303be"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "heptanone"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 599.486695
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "fruity"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac1e8bfc8d45945ef30603/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac1e8bfc8d45945ef30603",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac0bf7fc8d45945ef302cd",
										"display": "Part. 4HET-BV25"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:23:55.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac0bf7fc8d45945ef302cd",
							"display": "Part. 4HET-BV25"
						},
						"authored": "2017-09-03T15:23:50.483Z",
						"source": {
							"reference": "Patient/59ac0bf7fc8d45945ef302c9"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "isovaleci-acid"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 27825.62239
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "Anderes"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac1e7cfc8d45945ef30602/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac1e7cfc8d45945ef30602",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac0ca4fc8d45945ef3031d",
										"display": "Part. B0O5-2OMT"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:23:40.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac0ca4fc8d45945ef3031d",
							"display": "Part. B0O5-2OMT"
						},
						"authored": "2017-09-03T15:23:40.151Z",
						"source": {
							"reference": "Patient/59ac0ca4fc8d45945ef30319"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "isovaleci-acid"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 166.810904
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "pungent"
								}]
							}]
						}]
					}
				}, {
					"fullUrl": "https://ch.midata.coop/fhir/QuestionnaireResponse/59ac1e6afc8d45945ef30601/_history/0",
					"resource": {
						"resourceType": "QuestionnaireResponse",
						"id": "59ac1e6afc8d45945ef30601",
						"meta": {
							"extension": [{
								"url": "http://midata.coop/extensions/metadata",
								"extension": [{
									"url": "app",
									"valueCoding": {
										"system": "http://midata.coop/codesystems/app",
										"code": "misense",
										"display": "MiSens"
									}
								}, {
									"url": "creator",
									"valueReference": {
										"reference": "Patient/59ac147afc8d45945ef303f7",
										"display": "Part. 6S96-HTRV"
									}
								}]
							}],
							"versionId": "0",
							"lastUpdated": "2017-09-03T17:23:22.000+02:00"
						},
						"extension": [{
							"url": "http://midata.coop/extensions/response-code",
							"valueCoding": {
								"system": "http://midata.coop",
								"code": "MiSensSmellThreshold",
								"display": "Smell concentration threshold of a specific substance"
							}
						}],
						"status": "completed",
						"subject": {
							"reference": "Patient/59ac147afc8d45945ef303f7",
							"display": "Part. 6S96-HTRV"
						},
						"authored": "2017-09-03T15:23:19.936Z",
						"source": {
							"reference": "Patient/59ac147afc8d45945ef303f3"
						},
						"item": [{
							"linkId": "type",
							"text": "What type of questionaire is this?",
							"answer": [{
								"valueString": "smell-questionaire"
							}]
						}, {
							"linkId": "1",
							"text": "Smell Threshold",
							"item": [{
								"linkId": "substance",
								"text": "What was the name of the substance?",
								"answer": [{
									"valueString": "heptanone"
								}]
							}, {
								"linkId": "threshold",
								"text": "What was the first concentration you could barely smell? (in ppb, 0 if not smelled)",
								"answer": [{
									"valueDecimal": 599.486695
								}]
							}, {
								"linkId": "quality",
								"text": "How would you describe the taste quality?",
								"answer": [{
									"valueString": "fruity"
								}]
							}]
						}]
					}
				}];*/








			if (results.data && results.data.entry && Array.isArray(results.data.entry)) {
				results.data.entry.sort(function (a, b) {
					var _a = new Date(a.resource.meta.lastUpdated);
					var _b = new Date(b.resource.meta.lastUpdated);
					return _a.getTime() - _b.getTime();
				});
	
				

			for (var index = 0; index < results.data.entry.length; index++) {
				var element = results.data.entry[index];
				
				 console.log(index + ": " + element.resource.meta.lastUpdated);
			}

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
								// && 
								//(substanceFound == 'beta-ionone' || substanceFound == 'heptanone' || substanceFound == 'isobuteryl-aldehyde' || substanceFound == 'isovaleci-acid' || substanceFound == 'rotundone' || substanceFound == 'sucrose' || substanceFound == 'nacl' || substanceFound == 'prop')
							 ) {
								 if (_item2.answer && _item2.answer[0] && _item2.answer[0].valueDecimal != null) {
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
	};

	result.getInformation = function(authToken){
		var toReturn = {};
		var query = { "format" : "fhir/QuestionnaireResponse" };
		
		return midataServer.fhirSearch(authToken, "QuestionnaireResponse", null).then(function(results){

			results.data.entry.sort(function (a, b) {
				var _a = new Date(a.resource.meta.lastUpdated);
				var _b = new Date(b.resource.meta.lastUpdated);
				return _a.getTime() - _b.getTime();
			});

			result.records = results.data.entry;

			for (var i = 0; results.data.entry && i < results.data.entry.length; i++) {
				var _data = results.data.entry[i].resource;
	
				if (result.dataset == "scientifica" && (new Date(_data.meta.lastUpdated)).getTime() >= (new Date(2017,9-1,3,18,0,0,0)) ) {
					continue;
				}

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
									 if (_item2.answer && _item2.answer[0] && _item2.answer[0].valueDecimal) {
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