angular.module('surveys')
.factory('questionnaire', [ 'midataServer', function(midataServer) {
	var questionnaire = {};
	
	var fhir = function(record) {
	       var result = record.data;
	       result.id = record._id;
	       result.meta = { version : record.version };
	       return result;
	};
	
	questionnaire.create = function() {
		return {			
			  "resourceType" : "Questionnaire",						  
			  "identifier" : [], 
			  "version" : "0.1",
			  "name" : "unnamed", 
			  "title" : "Unnamed",
			  "status" : "draft",				 
			  "date" : new Date(), 				
			  "subjectType" : ["Patient"], 
			  "item" : []			
		};
	};
	
	questionnaire.load = function(id) {
		return midataServer.fhirRead(midataServer.authToken, "Questionnaire", id)
		.then(function(results) {
		   return results.data;		      			   		  
		});	
	};
	
	questionnaire.save = function(survey) {
		console.log(survey);
		if (survey.id) {
		  return midataServer.fhirUpdate(midataServer.authToken, survey).then(function(result) {
			 console.log(result.data);
			 
		  });			
		} else {
		  return midataServer.fhirCreate(midataServer.authToken, survey).then(function(result) {
  		    console.log(result.data);
			survey.id = result.data.id;
			return survey;
		  });
		}
	};
	
	questionnaire.createItem = function() {
		return {			
			 "linkId" : "",			    
			 "code" : [], 
			 "prefix" : "", 
			 "text" : "", 
			 "type" : "display",
			 
			 "required" : true,
			 "repeats" : false,			 
		 };								
	};
	
	return questionnaire;
}])
.factory('response', [ '$q', 'midataServer', function($q, midataServer) {
	var response = {};
	
	var fhir = function(record) {
	       var result = record.data;
	       result.id = record._id;
	       result.meta = { version : record.version };
	       return result;
	};
	
	response.create = function(survey) {
		 return $q.when({
		    resourceType : "QuestionnaireResponse",
		    questionnaire : { reference : "Questionnaire/"+survey.id },
		    status : "in-progress",
		    authored : new Date(),
		    item : []
		 });		 		 
	};
	
	response.load = function(id) {
		return midataServer.fhirRead(midataServer.authToken, "QuestionnaireResponse", id)
		.then(function(results) {
		   return results.data;		      			   		  
		});	
	};
	
	response.searchByQuestionnaire = function(questionnaire) {
		return midataServer.fhirSearch(midataServer.authToken, "QuestionnaireResponse", { "questionnaire" : "Questionnaire/"+questionnaire })
		.then(function(results) {
		   var res = [];
		   angular.forEach(results.data.entry, function(r) { res.push(r.resource); });
		   return res;		      			   		  
		});	
	};
	
	response.save = function(questionnaireResponse) {		
		if (questionnaireResponse.id) {
		  return midataServer.fhirUpdate(midataServer.authToken, questionnaireResponse);			
		} else {
		  return midataServer.fhirCreate(midataServer.authToken, questionnaireResponse);
		}				
	};
	
	response.emptyItem = function(item) {
		var result = {};
		 
		if (item.linkId) result.linkId = item.linkId;
		if (item.definition) result.definition = item.definition;
	    if (item.text) result.text = item.text;
		result.answer = [];
			
		return result;
	};
	
	return response;	
}])
.factory('editor', [ '$q', 'questionnaire', function($q, questionnaire) {
	var editor = {};
	var currentQuestionnaire = {};
	var itemMap = {};
	var counter = 1;
	
	var buildItemMap = function(parent) {
		if (parent === currentQuestionnaire) itemMap = {};
		
		angular.forEach(parent.item, function(item) {
			item._parent = parent;
			if (!item._id) {
				item._id = item.linkId;
				if (!item._id || itemMap[item._id]) item._id = counter++;
			}
			itemMap[item._id] = item;
			
			if (item.item && item.item.length) buildItemMap(item);
		});
	};
	
	var cleanItemMap = function(parent) {
		angular.forEach(parent.item, function(item) {
			item._parent = undefined;			
			if (item._id) itemMap[item._id] = undefined;
			item._id = undefined;			
			if (item.item && item.item.length) cleanItemMap(item);
		});
	};
	
	editor.createQuestionnaire = function() {
		currentQuestionnaire = questionnaire.create();
		return $q.when(currentQuestionnaire);
	};
	
	editor.selectQuestionnaire = function(id) {
		if (currentQuestionnaire.id == id) return $q.when(currentQuestionnaire);
		
		return questionnaire.load(id).then(function(r) {
			currentQuestionnaire = r;
			buildItemMap(currentQuestionnaire);
			return currentQuestionnaire;
		});		
	};
	
	editor.saveQuestionnaire = function(survey) {				
		currentQuestionnaire = survey;	
		cleanItemMap(survey);
		return questionnaire.save(currentQuestionnaire).then(function() {
			buildItemMap(survey);
		});
	};
	
	editor.end = function() {
		currentQuestionnaire = {};
	};
	
	editor.getQuestionnaire = function() {
		return currentQuestionnaire;
	};
	
	editor.getItems = function() {
		return currentQuestionnaire.item;
	};
			
	editor.getItem = function(id, itemId) {
		return editor.selectQuestionnaire(id).then(function() {
		
			if (!itemId) return currentQuestionnaire;
			
			var item = itemMap[itemId];		
			return item;
		
		});
	};
	
	editor.emptyItem = function() {
		return questionnaire.createItem();
	};
	
	editor.saveItem = function(id, item, loc) {
		return editor.selectQuestionnaire(id).then(function() {
		
		if (loc) {			
			var parent;
			if (loc.parentId) {		
				console.log(itemMap);
				parent = itemMap[loc.parentId];
			} else {				
				parent = currentQuestionnaire;
			}
			
			console.log(loc);
			if (!parent.item) parent.item = [];
			if (loc.idx === undefined) loc.idx = parent.item.length;			
			parent.item.splice(loc.idx | 0, 0, item);			
			buildItemMap(parent);			
		} else {
			
		}
		
		});
		
	};
	
	editor.deleteItem = function(id, item) {
	   return editor.selectQuestionnaire(id).then(function() {	   
		   var parent = item._parent;
		   var idx = parent.item.indexOf(item);
		   
		   cleanItemMap(parent);
		   parent.item.splice(idx, 1);
		   buildItemMap(parent);
	   });	   	   
	};
	
	return editor;
	
}])
.factory('answer', [ '$q', 'questionnaire', 'response', function($q, questionnaire, response) {
	var answer = {};
	
	var currentQuestionnaire = {};
	var currentResponse = {};
	var pages = [];	
	
	var itemMap = {};
	
	var buildItemMap = function(parent) {				
		angular.forEach(parent.item, function(item) {		
			itemMap[item.linkId] = item;			
			if (item.item && item.item.length) buildItemMap(item);
		});
	};
	
	var buildItem = function(qitem, ritem) {
		var result = qitem;
		result._response = ritem;
		
		var first = function(ritem) {
			if (ritem && ritem.answer && ritem.answer[0]) return ritem.answer[0];
			return {};
		};
		
		switch (qitem.type) {
		case "group": 
		case "display":			
			break;
		case "boolean":
			qitem._answer = first(ritem).valueBoolean;
			break;
		case "decimal":
			qitem._answer = first(ritem).valueDecimal;			
			break;
		case "integer":
			qitem._answer = first(ritem).valueInteger;			
			break;
		case "date" :
			qitem._answer = first(ritem).valueDate;			
			break;
		case "dateTime" :
			qitem._answer = first(ritem).valueDateTime;			
			break;
		case "time" : 
			qitem._answer = first(ritem).valueTime;			
			break;
		case "string":
		case "text":
			qitem._answer = first(ritem).valueString;			
			break;
		case "url":
			qitem._answer = first(ritem).valueUri;			
			break;
		case "choice":
			if (qitem.repeats) {
				
			} else {
				var cod = first(ritem).valueCoding;
				if (cod) qitem._answer = cod;
			}
			break;
		case "open-choice":
			break;
		case "attachment":
			break;
		case "reference":
			break;
		case "quantity":
		}
		
					
		angular.forEach(qitem.item, function(cqitem) {
			var critem = cqitem.linkId ? (itemMap[cqitem.linkId] || response.emptyItem(cqitem)) : response.emptyItem(cqitem);
			buildItem(cqitem, critem);					
		});
		
		return result;
	};
	
	var parseResponse = function(qitem, ritem) {
		switch (qitem.type) {
		case "group": 
		case "display":
			ritem.answer = undefined;
			break;
		case "boolean":
			ritem.answer = [ { "valueBoolean" : (qitem._answer ? true : false) } ];
			break;
		case "decimal":
			ritem.answer = [ { "valueDecimal" : Number(qitem._answer) }];
			break;
		case "integer":
			ritem.answer = [ { "valueInteger" : parseInt(qitem._answer, 10) }];
			break;
		case "date" :
			ritem.answer = [ { "valueDate" : qitem._answer }];
			break;
		case "dateTime" :
			ritem.answer = [ { "valueDateTime" : new Date(qitem._answer) }];
			break;
		case "time" : 
			ritem.answer = [ { "valueTime" : qitem._answer }];
			break;
		case "string":
		case "text":
			ritem.answer = [ { "valueString" : qitem._answer }];
			break;
		case "url":
			ritem.answer = [ { "valueUri" : qitem._answer }];
			break;
		case "choice":
			ritem.answer = [ { "valueCoding" : qitem._answer }];
			break;
		case "open-choice":
			break;
		case "attachment":
			break;
		case "reference":
			break;
		case "quantity":
			break;
		}
		qitem._answer = undefined;
	};
	
	var bundleResponse = function(qitem) {
		console.log("BR");
		console.log(qitem);
		var ritem = qitem._response;
		parseResponse(qitem, ritem);
		
		ritem.item = undefined;
		if (qitem.item) {
			ritem.item = [];
		
		    angular.forEach(qitem.item, function(cqitem) {
			  var critem = bundleResponse(cqitem);
			  if (critem) ritem.item.push(critem);
		    });
		}
				
		return ritem;
	};
	
	answer.startQuestionnaire = function(id, rid) {
		return questionnaire.load(id).then(function(q) {
	
			currentQuestionnaire = q; 
			if (rid) {
				return response.load(rid).then(function(x) {
					currentResponse = x;	
				});
			} else {
				return response.create(currentQuestionnaire).then(function(x) {
					currentResponse = x;	
				});
			}		
		}).then(function() {
			buildItemMap(currentResponse);
			
			angular.forEach(currentQuestionnaire.item, function(qitem) {
				var ritem = qitem.linkId ? (itemMap[qitem.linkId] || response.emptyItem(qitem)) : response.emptyItem(qitem);
				var dat = buildItem(qitem, ritem);
				
				pages.push(dat);
			});
		});
	};
	
	answer.getPage = function(pageid) {
		return pages[pageid];
	};
	
	answer.submitPage = function(pageid) {
		
	};
	
	answer.next = function(currentPage) {
		currentPage++;
		return (currentPage < pages.length) ? currentPage : -1;
	};
	
	answer.prev = function(currentPage) {		
		currentPage--;
		return currentPage >= 0 ? currentPage : 0;
	};
	
	answer.save = function() {
		currentResponse.item = [];
		console.log(currentQuestionnaire);
		console.log(currentResponse);
		angular.forEach(currentQuestionnaire.item, function(itm) {
			var ritem = bundleResponse(itm);
			if (ritem) currentResponse.item.push(ritem);
		});
		
		return response.save(currentResponse);
	};
	
	answer.end = function() {
		currentQuestionnaire = null;
		currentResponse = null;
		pages = [];
		itemMap = {};
	};
	
	return answer;	
}])
.factory('processResponses', [ '$q', 'questionnaire', 'response', function($q, questionnaire, response) {
	var processResponses = {};
	
	processResponses.process = function(responses) {
		
		var results = [];
		var resultMap = {};
		var inc = function(w,k) {
			if (!w[k]) w[k] = 1; else w[k]++;
		};
		
		var mergeAnswers = function(linked, item) {
			angular.forEach(item.answer, function(answer) {
			   if (answer.valueBoolean !== undefined)	{
				   if (!linked.answer.valueBoolean) linked.answer.valueBoolean = { true:0 , false:0 };
				   inc(linked.answer.valueBoolean, answer.valueBoolean);
			   } else if (answer.valueDecimal !== undefined) {
				   if (!linked.answer.valueDecimal) linked.answer.valueDecimal = {  };
				   inc(linked.answer.valueDecimal, answer.valueDecimal);				   
			   } else if (answer.valueInteger !== undefined) {
				   if (!linked.answer.valueInteger) linked.answer.valueInteger = {  };
				   inc(linked.answer.valueInteger, answer.valueInteger);
				  
			   } else if (answer.valueDate !== undefined) {
				   if (!linked.answer.valueDate) linked.answer.valueDate = {  };
				   inc(linked.answer.valueDate, answer.valueDate);				
			   } else if (answer.valueDateTime !== undefined) {				   
				   if (!linked.answer.valueDateTime) linked.answer.valueDateTime = {  };
				   inc(linked.answer.valueDateTime, answer.valueDateTime);				 
			   } else if (answer.valueTime !== undefined) {
				   if (!linked.answer.valueTime) linked.answer.valueTime = {  };
				   inc(linked.answer.valueTime, answer.valueTime);				   
			   } else if (answer.valueString !== undefined) {
				   if (!linked.answer.valueString) linked.answer.valueString = {  };
				   inc(linked.answer.valueString, answer.valueString);				   
			   } else if (answer.valueUri !== undefined) {
				   if (!linked.answer.valueUri) linked.answer.valueUri = {  };
				   inc(linked.answer.valueUri, answer.valueUri);				   
			   } else if (answer.valueCoding !== undefined) {
				   if (!linked.answer.valueCoding) linked.answer.valueCoding = {  };
				   if (!linked.labelMap) linked.labelMap = {};
				   var k = answer.valueCoding.system+" "+answer.valueCoding.code;
				   if (!linked.labelMap[k]) linked.labelMap[k] = answer.valueCoding.display;
				   inc(linked.answer.valueCoding, k);				   
			   }			 
			});
			
			linked.answer.some = linked.answer.valueBoolean || linked.answer.valueDecimal || linked.answer.valueInteger  || linked.answer.valueDate  || linked.answer.valueDateTime  || linked.answer.valueTime || linked.answer.valueString || linked.answer.valueUri || linked.answer.valueCoding;
			   
		};
		
		var processItems = function(items) {
			angular.forEach(items, function(item) {
				if (item.linkId && item.answer) {
					var linked = resultMap[item.linkId];
					if (!linked) {
						linked = { linkId : item.linkId, text : item.text, count : 0, answer:{} };
						results.push(linked);
						resultMap[item.linkId] = linked;
					}
					if (item.answer && item.answer.length > 0) {
						linked.count++;
						mergeAnswers(linked, item);
					}
				}
				if (item.item) {
					processItems(item.item);
				}
			});
		};
		
		angular.forEach(responses, function(singleResponse) {
			processItems(singleResponse.item);
		});
		
		angular.forEach(results, function(result) {
			if (result.answer && result.answer.some) {
				result.labels = [];
				result.values = [];
				angular.forEach(result.answer.some, function(v,k) {
					if (result.labelMap && result.labelMap[k]) result.labels.push(result.labelMap[k]);
					else result.labels.push(k);
					result.values.push(v);
				});
				result.values = [ result.values ];
			}
		});
		
		return results;
	};
	
	return processResponses;
}]);