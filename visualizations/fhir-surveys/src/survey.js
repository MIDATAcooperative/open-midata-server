angular.module('surveys')
.factory('surveys', [ function() {
   var surveys = {};
   
   surveys.newSurvey = function() {
	 var newSurvey = {
	    resourceType : "Questionnaire",
	    version : "1.0",
	    status : "draft",
	    date : new Date(),
	    group : {}
	 };
	 return newSurvey;
   };
   
   surveys.newGroup = function(parent, idx) {
	 var newGroup = {
	    linkId : "",
	    title : "New Group",	    
		question:[]	 
	 };
	 if (!parent.group) parent.group = [];
	 if (idx == "last") idx = parent.group.length;
	 parent.group.splice(idx, 0, newGroup);
	 return newGroup;
   };
   
   surveys.newQuestion = function(parent, idx) {
	 if (!parent.question) parent.question = [];
	 if (idx == "last") idx = parent.question.length;
	 var newQuestion = {
		linkId : "",
		text : "",
		type : "text",		
	 };
	 parent.question.splice(idx, 0, newQuestion);
	 return newQuestion;
   };
   
   surveys.newResponse = function(survey) {
	 var newResponse = {
	    resourceType : "QuestionnaireResponse",
	    questionnaire : "Questionnaire/"+survey.id,
	    status : "in-progress",
	    authored : new Date(),
	    group : surveys.newResponseGroup(survey.group)
	 };
	 
	 return newResponse;
   };
   
   surveys.newResponseGroup = function(group) {
	 var newResponseGroup = {
		linkId : group.linkId,
		title : group.title,
		text : group.text	
	 };
	 if (group.group) {
		 newResponseGroup.group = [];
	 }
	 if (group.question) {
		 newResponseGroup.question = [];
	 }
	 return newResponseGroup;
   };
   
   surveys.newResponseQuestion = function(question) {
	  var newResponseQuestion = {
		 linkId : question.linkId,
		 text : question.text,
		 answer : [ {} ]
	  };
	  return newResponseQuestion;
   };
   
   surveys.deleteGroup = function(group, fromGroup) {
	  if (!fromGroup.group) return;
	  var idx = fromGroup.group.indexOf(group);
	  if (idx >= 0) {
		  fromGroup.group.splice(idx, 1);
		  if (fromGroup.group.length === 0) {
			  delete fromGroup.group; 
		  }
	  } else {
		  angular.forEach(fromGroup.group, function(grp) { surveys.deleteGroup(group, grp); });
	  }
   };
   
   surveys.deleteQuestion = function(question, fromGroup) {
		  if (fromGroup.question) {
		    var idx = fromGroup.question.indexOf(question);
		    if (idx >= 0) {
			  fromGroup.question.splice(idx, 1);
			  
			  if (fromGroup.question.length === 0) {
				  delete fromGroup.question;
			  }
		    }		   
		  } else {
			  angular.forEach(fromGroup.group, function(grp) { surveys.deleteQuestion(question, grp); });
		  }
   };
   
         
   return surveys;
}])
.factory('currentSurvey', [ 'surveys', function(surveys) {
	var result = {
	   activeSurvey : null,
	   activeGroup : null,
	   activeQuestion : null,
	   activeResponse : null,
	   pages : [],
	   activeStepIdx : 0,
	   activePage : null
	};
	result.setSurvey = function(survey) {
		result.activeSurvey = survey;
		result.activeGroup = survey.group;
		result.activeQuestion = null;		
	};
	result.setQuestion = function(question) {
		result.activeGroup = null;
		result.activeQuestion = question;
	};
	result.setGroup = function(group) {
		result.activeQuestion = null;
		result.activeGroup = group;
	};
	result.findByLinkId = function(id, items) {
		for (var i=0;i<items.length;i++) {
			if (items[i].linkId == id) return items[i];
		}
	};
	result.startAnswer = function(survey, response) {
		if (!survey) survey = result.activeSurvey;
		result.activeSurvey = survey;
		result.activeResponse = response || surveys.newResponse(survey);
		result.pages = [ result.createPage(survey.group, result.activeResponse.group) ];
		if (survey.group.group) {
			angular.forEach(survey.group.group, function(grp) {
				var response = result.findByLinkId(grp.linkId, result.activeResponse.group.group);
				if (!response) {
					response = surveys.newResponseGroup(grp);
					result.activeResponse.group.group.push(response);
				}
				var page = result.createPage(grp, response);
				result.pages.push(page);
			});
		}
		result.activeStepIdx = 0;
		result.activePage = result.pages[0];
		result.preparePage(result.activePage);
		console.log(result.pages);
	};	
	result.createPage = function(group, responseGroup) {
	    var page = {
	    	group : group,
	    	rGroup : responseGroup
	    };	    
	    return page;
	};
	result.preparePage = function(page) {
		if (page.group.question != null && !page.question) {
	    	page.question = [];
	    	if (!page.rGroup.question) {
	    		page.rGroup.question = [];
	    	}
	    	angular.forEach(page.group.question, function(q) {
	    		var rQuestion = result.findByLinkId(q.linkId, page.rGroup.question);
	    		if (rQuestion == null) {
	    			rQuestion = surveys.newResponseQuestion(q);
	    			page.rGroup.question.push(rQuestion);
	    		}
	    		var pageQuestion = {
	    		   question : q,
	    		   rQuestion : rQuestion
	    		};
	    		page.question.push(pageQuestion);
	    	});
	    }
	};
	result.next = function() {
		result.activeStepIdx++;
		if (result.activeStepIdx < result.pages.length) {
			result.activePage = result.pages[result.activeStepIdx];
			result.preparePage(result.activePage);
			console.log(result.activePage);
			return true;
		} else {
			return false;
		}		
	};
	result.prev = function() {
		result.activeStepIdx--;
		if (result.activeStepIdx < 0) {
			result.activePage = null;
			return false;
		} else {
			result.activePage = result.pages[result.activeStepIdx];
			result.preparePage(result.activePage);
			return true;
		}
	};
	result.reset = function() {
		result.activeSurvey = null;
		result.activeGroup = null;
		result.activeQuestion = null;
	};
	return result;
}]);