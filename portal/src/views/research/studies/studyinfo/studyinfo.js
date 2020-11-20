/*
 * This file is part of the Open MIDATA Server.
 *
 * The Open MIDATA Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * The Open MIDATA Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Open MIDATA Server.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('portal')
.controller('StudyInfoCtrl', ['$scope', '$state', 'server', 'status', 'languages', function($scope, $state, server, status, languages) {
  
   $scope.studyid = $state.params.studyId;
   $scope.status = new status(true);
   $scope.languages = languages.all;
   $scope.sections = ["SUMMARY", "ONBOARDING", "DESCRIPTION", "HOMEPAGE", "CONTACT", "INSTRUCTIONS", "PURPOSE", "AUDIENCE", "LOCATION", "PHASE", "SPONSOR", "SITE", "DEVICES", "COMMENT"];
   
   $scope.allsections = {
		   "ALL" : ["SUMMARY", "ONBOARDING", "DESCRIPTION", "HOMEPAGE", "CONTACT", "INSTRUCTIONS", "PURPOSE", "AUDIENCE", "LOCATION", "PHASE", "SPONSOR", "SITE", "DEVICES", "COMMENT"],
		   "PARTICIPANTS" : ["CONTACT", "INSTRUCTIONS", "DEVICES", "COMMENT"],
		   "INTERNAL" : ["PURPOSE", "PHASE", "COMMENT"]
   }
   $scope.visibilities = ["ALL", "PARTICIPANTS", "INTERNAL"];
   $scope.selection = { langs : ["int"], visibility : "ALL" };
   
   $scope.reload = function() {
	   	
	   $scope.status.doBusy(server.get(jsRoutes.controllers.research.Studies.get($scope.studyid).url))
	    .then(function(data) { 				
			$scope.study = data.data;
			$scope.study.recordQuery = undefined;
						
			$scope.generate();
		});
   };
   
   $scope.generate = function() {
	  var result = [];
	  var byKey = {};
	  if ($scope.study.infos) {
		  angular.forEach($scope.study.infos, function(info) {
			  info.visibility = "ALL";
			  byKey[info.type+"_"+info.visibility] = info;
		  });
	  }
	  if ($scope.study.infosPart) {
		  angular.forEach($scope.study.infosPart, function(info) {
			  info.visibility = "PARTICIPANTS";
			  byKey[info.type+"_"+info.visibility] = info;
		  });
      }
	  if ($scope.study.infosInternal) {
		  angular.forEach($scope.study.infosInternal, function(info) {
			  info.visibility = "INTERNAL";
			  byKey[info.type+"_"+info.visibility] = info;
		  });
	  }
	  angular.forEach($scope.visibilities, function(v) {
		 angular.forEach($scope.allsections[v], function(s) {
			var inf = byKey[s+"_"+v] || { type : s, visibility : v, value : {} };
			result.push(inf);
		 }); 
	  });
	  $scope.infos = result;
   };
   
  
  $scope.changevisible = function() {
	$scope.sections = $scope.allsections[$scope.selection.visibility];  
	console.log("DONE");
  };
   
  $scope.submit = function() {
	 
   	   
   	   /*try{
	     $scope.study.recordQuery = JSON.parse($scope.study.recordQueryStr);
   	   } catch (e) { console.log(e); $scope.error = e.message;return; }
   	   */
	   	$scope.submitted = true;	
	   	/*
		if ($scope.error && $scope.error.field && $scope.error.type) $scope.myform[$scope.error.field].$setValidity($scope.error.type, true);
		$scope.error = null;
		
		if (! $scope.myform.$valid) {
			var elem = $document[0].querySelector('input.ng-invalid');
			if (elem && elem.focus) elem.focus();
			return;
		}*/
   	   	   
		var result = { ALL : [], PARTICIPANTS : [], INTERNAL : [] };
		
		angular.forEach($scope.infos, function(inf) {
		  var used = false;
		  if (inf.value && inf.value.int && inf.value.int.trim().length > 0) used = true;
		  else angular.forEach(languages.all, function(l) {
			 if (inf.value && inf.value[l.value] && inf.value[l.value].trim().length > 0) used = true; 
		  });
		  if (used) {
			  result[inf.visibility].push(inf);
		  }
		
		});
	   var data = { infos : result.ALL, infosPart : result.PARTICIPANTS, infosInternal : result.INTERNAL };
	   $scope.status.doAction("update", server.post(jsRoutes.controllers.research.Studies.updateNonSetup($scope.studyid).url, JSON.stringify(data)))
	  .then(function(data) { 				
		    $scope.reload();
		    $scope.saveOk = true;
	   }); 
   };
   
   $scope.studyLocked = function() {
		 return (!$scope.study) || /*($scope.study.validationStatus !== "DRAFT" && $scope.study.validationStatus !== "REJECTED") ||*/ !$scope.study.myRole.setup;
   };
   
   $scope.toggle = function(array,itm) {
		console.log(array);
		var pos = array.indexOf(itm);
		if (pos < 0) array.push(itm); else array.splice(pos, 1);
  };
   
   $scope.reload();
}]);
