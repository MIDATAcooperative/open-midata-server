angular.module('portal')
.controller('StudyRulesCtrl', ['$scope', '$state', 'server', 'status', 'terms', 'apps', 'labels', '$translate', 'formats', function($scope, $state, server, status, terms, apps, labels, $translate, formats) {
   
   $scope.studyid = $state.params.studyId;
   $scope.status = new status(false, $scope);
   $scope.error = null;
   $scope.requirements = apps.userfeatures;
   $scope.datePickers = {  };
   $scope.dateOptions = {
	  	 formatYear: 'yy',
	  	 startingDay: 1
   };
   $scope.query = {};
   $scope.codesystems = formats.codesystems;
   
   $scope.reload = function() {
	   	  
	   $scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
	    .then(function(data) { 				
			$scope.study = data.data;	
			if (!$scope.study.requirements) $scope.study.requirements = [];
			$scope.study.recordQueryStr = JSON.stringify($scope.study.recordQuery);
			$scope.updateQuery();
		});
   };
   
   $scope.submit = function() {
	   $scope.error = null;
   	   
   	   try{
	     $scope.study.recordQuery = JSON.parse($scope.study.recordQueryStr);
   	   } catch (e) { console.log(e); $scope.error = e.message;return; }
	  
	   var data = { recordQuery : $scope.study.recordQuery, termsOfUse : $scope.study.termsOfUse, requirements: $scope.study.requirements, startDate : $scope.study.startDate, endDate : $scope.study.endDate, dataCreatedBefore : $scope.study.dataCreatedBefore };
	   $scope.status.doAction("update", server.put(jsRoutes.controllers.research.Studies.update($scope.studyid).url, JSON.stringify(data)))
	  .then(function(data) { 				
		    $scope.reload();
	   }); 
   };
   
   $scope.studyLocked = function() {
	 return (!$scope.study) || ($scope.study.validationStatus !== "DRAFT" && $scope.study.validationStatus !== "REJECTED") || !$scope.study.myRole.setup;    
   };
   
   $scope.toggle = function(array,itm) {
		console.log(array);
		var pos = array.indexOf(itm);
		if (pos < 0) array.push(itm); else array.splice(pos, 1);
   };
         
   terms.search({}, ["name", "version", "language", "title"])
	.then(function(result) {
		$scope.terms = result.data;
	});
   
   $scope.updateQuery = function() {
		try {
		  $scope.study.recordQuery = JSON.parse($scope.study.recordQueryStr);
		
		var is = function(f, v) {
			return f && (f == v ||( f.length > 0 && f.indexOf(v) >= 0));
		};
		
		$scope.labels = [];
		
		var q = labels.simplifyQuery($scope.study.recordQuery);
		
		if (q.content) {
			angular.forEach(q.content, function(r) {
			  labels.getContentLabel($translate.use(), r).then(function(lab) {
				 $scope.labels.push({ type : "content", field : r, label : lab, selected : true }); 
			  });
			});
		}
		if (q.group) {
			angular.forEach(q.group, function(r) {
				  labels.getGroupLabel($translate.use(), r).then(function(lab) {
					 $scope.labels.push({ type : "group", field : r, label : lab, selected : true }); 
				  });
			});
		}
		} catch (e) {}	
	};
	
	$scope.patchQuery = function(field, val) {
		
		if (field == "app") {
			if ($scope.query.ownapp) $scope.study.recordQuery.app = $scope.app.filename; 
			else $scope.study.recordQuery.app = undefined;
		}
		if (field == "group") {
			if ($scope.query.all) {
				$scope.study.recordQuery.group = ["all"]; 
				$scope.study.recordQuery["group-system"] = "v1";
			}
			else {
				$scope.study.recordQuery.group = undefined;
				$scope.study.recordQuery["group-system"] = undefined;
			}
		}
		
		if (field == "content") {
			if (!$scope.study.recordQuery.content) $scope.study.recordQuery.content = [];
			if (val.selected) {
				$scope.study.recordQuery.content.push(val.field);
			} else {
				$scope.study.recordQuery.content.splice($scope.study.recordQuery.content.indexOf(val.field), 1);
			}						
		}
		
		$scope.study.recordQueryStr = JSON.stringify($scope.study.recordQuery);
	};
	
	
	$scope.addCode = function() {
		if ($scope.query.system.type && $scope.query.system.type == "app") {
			apps.getApps({ filename : $scope.query.code}, ["defaultQuery"])
			.then(function(r) {
				if (r.data && r.data.length == 1) {
					var q = r.data[0].defaultQuery;
					if (q.content) {
						angular.forEach(q.content, function(name) {
							if (!$scope.study.recordQuery.content) $scope.study.recordQuery.content = [];
							if ($scope.study.recordQuery.content.indexOf(name) == -1) {
								$scope.study.recordQuery.content.push(name);
							}
						});
					}
				} else {
					{
						$scope.codeerror = "error.unknown.code";
						$scope.myform.queryadd.$invalid = true;
					}
				}
			});
		} else {
		
			formats.searchCodes({ system : $scope.query.system.system, code : $scope.query.code }, ["content"])
			.then(function(r) {
				if (r.data && r.data.length == 1) {
					$scope.patchQuery("content", { field : r.data[0].content, selected : true });
					$scope.updateQuery();
				} else {
					$scope.codeerror = "error.unknown.code";
					$scope.myform.queryadd.$invalid = true;
				}
			});
		}
		
	};
		
   
   $scope.reload();
}]);
