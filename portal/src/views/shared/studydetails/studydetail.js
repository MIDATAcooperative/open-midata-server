angular.module('portal')
.controller('StudyDetailCtrl', ['$scope', '$state', 'server', 'views', 'session', 'users', 'studies', '$window', function($scope, $state, server, views, session, users, studies, $window) {
	
	$scope.studyid = $state.params.studyId;
	$scope.study = {};
	$scope.participation = {};
	$scope.providers = [];
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
						console.log(p);
						$scope.providers.push(session.resolve(p, function() { return users.getMembers({ "_id" : p },users.ALLPUBLIC ); }));
					});
				}
				
				if ($scope.participation && !($scope.participation.status == "CODE" || $scope.participation.status == "MATCH" )) {
				  views.setView("shared_with_study", { aps : $scope.participation._id.$oid, properties : { } , type:"participations", allowAdd : true, allowRemove : false, fields : [ "ownerName", "created", "id", "name" ]});
				} else {
				  views.disableView("shared_with_study");
				}
			}).
			error(function(err) {
				$scope.error = err;				
			});
	};
	
	$scope.addProvider = function() {
		var execAdd = function(prov) {
		   return studies.updateParticipation($scope.study._id.$oid, { add : { providers : [ prov._id.$oid ]}})
		          .then(function() {
		        	  $scope.reload();
		          });
		   
		};
		
		views.setView("providersearch", { callback : execAdd });
	};
	
	$scope.removeProvider = function(prov) {
		studies.updateParticipation($scope.study._id.$oid, { remove : { providers : [ prov._id.$oid ]}})
        .then(function() {
      	  $scope.reload();
        });
	};
		
	
	$scope.needs = function(what) {
		return $scope.study.requiredInformation && $scope.study.requiredInformation == what;
	};
	
	$scope.mayRequestParticipation = function() {
		return ($scope.participation != null && ( $scope.participation.status == "MATCH" || $scope.participation.status == "CODE" )) ||
		   ($scope.participation == null && $scope.study.participantSearchStatus == 'SEARCHING');
	};
	
	$scope.mayDeclineParticipation = function() {
		return $scope.participation != null && ( $scope.participation.status == "MATCH" || $scope.participation.status == "CODE" );
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
	
	$scope.reload();
	
	$scope.goBack = function() {
		$window.history.back();
	};
	
}]);