angular.module('portal')
.controller('StudyDetailCtrl', ['$scope', '$state', 'server', 'views', 'session', 'users', 'studies', 'labels', '$window', '$translate', function($scope, $state, server, views, session, users, studies, labels, $window, $translate) {
	
	$scope.studyid = $state.params.studyId;
	$scope.study = {};
	$scope.participation = {};
	$scope.providers = [];
	$scope.labels = [];
	$scope.loading = true;
	$scope.error = null;
		
	views.link("shared_with_study", "record", "record");
	views.link("shared_with_study", "shareFrom", "share");
	views.link("share", "record", "record");
	
	$scope.reload = function() {
			
		server.get(jsRoutes.controllers.members.Studies.get($scope.studyid).url).
			success(function(data) { 				
				$scope.study = data.study;
				delete $scope.study.recordQuery["group-system"];
				$scope.participation = data.participation;
				$scope.research = data.research;
				$scope.loading = false;
				$scope.error = null;
				
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
				
				var sq = labels.simplifyQuery($scope.study.recordQuery);
				
				if (sq) {
					$scope.labels = [];
					if (sq.content) {
						angular.forEach(sq.content, function(r) {
						  labels.getContentLabel($translate.use(), r).then(function(lab) {
							 $scope.labels.push(lab); 
						  });
						});
					}
					if (sq.group) {
						angular.forEach(sq.group, function(r) {
							  labels.getGroupLabel($translate.use(), r).then(function(lab) {
								 $scope.labels.push(lab); 
							  });
						});
					}
				}
			}).
			error(function(err) {
				$scope.error = err;				
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
		   ($scope.participation == null && !$scope.locked && $scope.study.participantSearchStatus == 'SEARCHING');
	};
	
	$scope.mayDeclineParticipation = function() {
		return $scope.participation != null && ( $scope.participation.pstatus == "MATCH" || $scope.participation.pstatus == "CODE" || $scope.participation.pstatus == "REQUEST" );
	};
	
	$scope.mayRetreatParticipation = function() {
		return $scope.participation != null && $scope.participation.pstatus == "ACCEPTED";
	};
	
	$scope.requestParticipation = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.members.Studies.requestParticipation($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.noParticipation = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.members.Studies.noParticipation($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
		});
	};
	
	$scope.retreatParticipation = function() {
		$scope.error = null;
		
		server.post(jsRoutes.controllers.members.Studies.retreatParticipation($scope.studyid).url).
		success(function(data) { 				
		    $scope.reload();
		}).
		error(function(err) {
			$scope.error = err;			
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
	
}]);