angular.module('portal')
.controller('StudyDetailCtrl', ['$scope', '$state', 'server', 'views', 'session', 'users', 'studies', 'labels', '$window', '$translate', 'spaces', 'status', 'apps', 'actions', function($scope, $state, server, views, session, users, studies, labels, $window, $translate, spaces, status, apps, actions) {
	
	$scope.studyid = $state.params.studyId;
	$scope.study = {};
	$scope.participation = {};
	$scope.providers = [];
	$scope.labels = [];
	$scope.loading = true;
	$scope.error = null;
	$scope.translate = $translate;
	$scope.status = new status(true);
	$scope.lang = $translate.use();	
	$scope.pleaseReview= ($state.params.action != null);
	
	views.link("shared_with_study", "record", "record");
	views.link("shared_with_study", "shareFrom", "share");
	views.link("share", "record", "record");
	
	$scope.view = views.getView("terms");
	
	$scope.reload = function() {
			
		server.get(jsRoutes.controllers.members.Studies.get($scope.studyid).url).
			then(function(data1) {
				var data = data1.data;
				$scope.study = data.study;
				delete $scope.study.recordQuery["group-system"];
				$scope.participation = data.participation;
				$scope.research = data.research;
				$scope.loading = false;
				$scope.error = null;
				
				
				server.get(jsRoutes.controllers.Market.getStudyAppLinks("study-use", $scope.study._id).url)
			    .then(function(data) {		    	
			        $scope.links = [];
			    	for (var l=0;l<data.data.length;l++) {
			    		var link = data.data[l];
			    		if (link.type.indexOf("RECOMMEND_A")>=0) {
			    			if (link.type.indexOf("REQUIRE_P")<0 || ($scope.participation && $scope.participation.pstatus=="ACCEPTED")) {
			    			  $scope.links.push(link);
			    			}
			    		}
			    	}	
			    	console.log("LINKS");
			    	console.log($scope.links);
				});	
				
				$scope.providers = [];
				if (data.participation && data.participation.providers) {
					angular.forEach(data.participation.providers, function(p) {
					
						$scope.providers.push(session.resolve(p, function() { return users.getMembers({ "_id" : p },users.ALLPUBLIC ); }));
					});
				}
				
				if ($scope.participation && !($scope.participation.status == "CODE" || $scope.participation.status == "MATCH" )) {
				  views.setView("shared_with_study", { aps : $scope.participation._id, properties : { } , type:"participations", allowAdd : true, allowRemove : false, fields : [ "ownerName", "created", "id", "name" ]});
				} else {
				  views.disableView("shared_with_study");
				}
				
				var sq = labels.simplifyQuery($scope.study.recordQuery,null,true);				
				if (sq) {
					$scope.labels = [];
					if (sq.content) {
						angular.forEach(sq.content, function(r) {
						  if (r === "Patient" || r === "Group" || r === "Person" || r === "Practitioner") return;
						  labels.getContentLabel($translate.use(), r).then(function(lab) {
							 if ($scope.labels.indexOf(lab)<0) $scope.labels.push(lab); 
						  });
						});
					}
					if (sq.group) {
						angular.forEach(sq.group, function(r) {
							  labels.getGroupLabel($translate.use(), r).then(function(lab) {
								  if ($scope.labels.indexOf(lab)<0) $scope.labels.push(lab); 
							  });
						});
					}
				}
			}, function(err) {
				$scope.error = err.data;				
			});
	};
	
	$scope.addProvider = function() {
		var execAdd = function(prov) {
		   return studies.updateParticipation($scope.study._id, { add : { providers : [ prov._id ]}})
		          .then(function() {
		        	  $scope.reload();
		          });
		   
		};
		
		views.setView("providersearch", { callback : execAdd });
	};
	
	$scope.removeProvider = function(prov) {
		studies.updateParticipation($scope.study._id, { remove : { providers : [ prov._id ]}})
        .then(function() {
      	  $scope.reload();
        });
	};
		
	
	$scope.needs = function(what) {
		return $scope.study.requiredInformation && $scope.study.requiredInformation == what;
	};
	
	$scope.mayRequestParticipation = function() {
		return ($scope.participation != null && ( $scope.participation.pstatus == "MATCH" || $scope.participation.pstatus == "CODE" )) ||
		   ($scope.participation == null && !$scope.locked && $scope.study.participantSearchStatus == 'SEARCHING' && $state.current.data.role == 'MEMBER' && (!$scope.study.joinMethods || $scope.study.joinMethods.indexOf("PORTAL")>=0 ));
	};
	
	$scope.mayDeclineParticipation = function() {
		return $scope.participation != null && ( $scope.participation.pstatus == "MATCH" || $scope.participation.pstatus == "CODE" || $scope.participation.pstatus == "REQUEST" );
	};
	
	$scope.mayRetreatParticipation = function() {
		return $scope.participation != null && $scope.participation.pstatus == "ACCEPTED";
	};
	
	$scope.maySkip = function() {
		return $state.params.action != null;
	};
	
	$scope.skip = function() {
		if (!actions.showAction($state)) {
		      $scope.reload();
		}
	};
	
	$scope.requestParticipation = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.members.Studies.requestParticipation($scope.studyid).url).
		then(function(data) { 	
			if (!actions.showAction($state)) {
		      $scope.reload();
			}
		}, function(err) {
			$scope.error = err.data;			
		});
	};
	
	$scope.noParticipation = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.members.Studies.noParticipation($scope.studyid).url).
		then(function(data) { 				
			if (!actions.showAction($state)) {
			      $scope.reload();
		    }
		}, function(err) {
			$scope.error = err.data;			
		});
	};
	
	$scope.retreatParticipation = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.members.Studies.retreatParticipation($scope.studyid).url).
		then(function(data) { 				
			if (!actions.showAction($state)) {
			      $scope.reload();
			}
		}, function(err) {
			$scope.error = err.data;			
		});
	};
	
	session.currentUser.then(function(myUserId) {
	  /*if (session.user.subroles.indexOf("MEMBEROFCOOPERATIVE") < 0 && session.user.role === "MEMBER") {
		  $scope.locked = true;
	  } else $scope.locked = false;*/
	  $scope.reload();
	});
	
	$scope.goBack = function() {
		$window.history.back();
	};
	
	/*$scope.terms = function(terms) {
		$state.go("^.terms", { which : terms });
	};*/
	
	$scope.terms = function(def) {
		console.log("TERMS");
		views.setView("terms", { which : def }, "Terms");
	};
	
	$scope.installApp = function(app) {
		
		
		  spaces.get({ "owner": $scope.userId, "visualization" : app._id }, ["_id", "type"])
		  .then(function(spaceresult) {
			 if (spaceresult.data.length > 0) {
				 var target = spaceresult.data[0];
				 if (target.type === "oauth1" || target.type === "oauth2") {
					 $state.go("^.importrecords", { "spaceId" : target._id, params : $state.params.params });
				 } else { 
				     $state.go("^.spaces", { spaceId : target._id, params : $state.params.params });
				 }
			 } else {	  				
				$scope.status.doAction("install", apps.installPlugin(app._id, { applyRules : true }))
				.then(function(result) {				
					//session.login();
					if (result.data && result.data._id) {
					  if (app.type === "oauth1" || app.type === "oauth2") {
						 $state.go("^.importrecords", { "spaceId" : result.data._id });
					  } else { 
					     $state.go('^.spaces', { spaceId : result.data._id });
					  }
					} else {
					  $state.go('^.dashboard', { dashId : $scope.options.context });
					}
				});
			 }
		  });
	};
	
}]);