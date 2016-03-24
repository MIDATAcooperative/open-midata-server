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
   
   surveys.newGroup = function(parent) {
	 var newGroup = {
	    linkId : "",
	    title : "New Group",	    
		question:[]	 
	 };
	 if (!parent.group) parent.group = [];
	 parent.group.push(newGroup);
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
	    group : {
	    	linkId : survey.group.linkId,
	    	title : survey.group.title,
	    	text : survey.group.text	    	
	    }
	 };
	 return newResponse;
   };
   
   surveys.newResponseGroup = function(group) {
	 var newResponseGroup = {
		linkId : group.linkId,
		title : group.title,
		text : group.text	
	 };
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
		result.activeGroup = null;
		result.activeQuestion = null;		
	};
	result.setQuestion = function(question) {		
		result.activeQuestion = question;
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
		if (survey.group.groups) {
			angular.forEach(survey.group.groups, function(grp) {
				var response = result.findByLinkId(grp.linkId, result.activeResponse.group.groups);
				if (!response) {
					response = survey.newResponseGroup(grp);
				}
				var page = result.createPage(grp, response);
				result.pages.push(page);
			});
		}
		result.activeStepIdx = 0;
		result.activePage = result.pages[0];
		result.preparePage(result.activePage);
		
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
	return result;
}]);