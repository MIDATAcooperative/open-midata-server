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
.controller('ContentEditorCtrl', ['$scope', '$state', 'views', 'session', 'formats', 'status', 'languages', '$q', '$timeout', function($scope, $state, views, session, formats, status, languages, $q, $timeout) {

	$scope.status = new status(true);
	$scope.lang = "en";
	$scope.security = ["MEDIUM", "HIGH"];
    $scope.nameToGroup = {};
    $scope.nameToContent = {};
    $scope.groupSystem = "v1";
    $scope.saving = false;
    $scope.languages = languages.array;
    $scope.resourceTypes = ["", "fhir/Observation", "fhir/DocumentReference"];
    $scope.subTypes = ["", "Quantity", "CodeableConcept", "String", "Range", "Ratio", "SampledData", "Attachment", "Time", "DateTime", "Period", "component"];
    $scope.categories = ["", "social-history", "vital-signs", "imaging", "laboratory", "procedure", "survey", "exam", "therapy", "fitness" ];
	
	$scope.init = function() {	
		  $scope.nameToGroup = {};
	      $scope.nameToContent = {};
		
		  $scope.status.doBusy(formats.listCodes())
		  .then(function(data) { $scope.codes = data.data;$scope.prepare(); });		  		  
		  
		  $scope.status.doBusy(formats.listContents())
		  .then(function(data) { $scope.contents = data.data;$scope.prepare(); });
		  
		  $scope.status.doBusy(formats.listGroups())
		  .then(function(data) { $scope.groups = data.data;$scope.prepare(); });
	};
	
	$scope.prepare = function() {
		if ($scope.codes && $scope.contents && $scope.groups) {
			var nameToCode = {};
			angular.forEach($scope.codes , function(code) {
				nameToCode[code.system+" "+code.code] = code;								
			});
			
			
			angular.forEach($scope.contents , function(cnt) {
				$scope.nameToContent[cnt.content] = cnt;
				var defCode = nameToCode[cnt.defaultCode];
				if (defCode == null) {
					var system = cnt.defaultCode.substr(0, cnt.defaultCode.indexOf(' '));
					var name = cnt.defaultCode.substr(cnt.defaultCode.indexOf(' ')+1);
					var newCode = { code : name, system : system, display : cnt.label[$scope.lang], content : cnt.content, action : "create" };
					$scope.codes.push(newCode);
				}
				
			});
			angular.forEach($scope.groups, function(grp) {
				$scope.nameToGroup[grp.name] = grp;
				grp.contentEntries = [];
				if (grp.contents) {
					angular.forEach(grp.contents, function(cnt) {
						var c = $scope.nameToContent[cnt];
					
						if (c != null) {
							grp.contentEntries.push(c);
							c.group = grp.name;
						}
					});				
				}
				$scope.checkGroupValid(grp);
			});
			
		}
	};
	
	$scope.checkGroupValid = function(grp) {
		var hasChildGroups = false;
		angular.forEach($scope.groups, function(g) { if (g.parent == grp.name) hasChildGroups = true; });
		/*if (hasChildGroups && grp.contentEntries && grp.contentEntries.length > 0) grp.problem = "double";
		else*/ if (!hasChildGroups && (grp.contentEntries.length === 0)) grp.problem = "empty";
		else grp.problem = null;
	};
	
	$scope.editCode = function(code) {				
		views.setView("editCode", { code : code }, "Code :"+code.code);				
	};	
	$scope.addCode = function(content) {
		var code = { code : "", display : content.label[$scope.lang], content : content.content, action:"create" };
		$scope.codes.push(code);
		views.setView("editCode", { code : code }, "Code: "+code.code);				
	};
	$scope.newCode = function() {
		var code = { code : "", display : "", content : "", action:"create" };
		$scope.codes.push(code);
		views.setView("editCode", { code : code }, "New Code");				
	};
	
	$scope.editContent = function(content) {				
		views.setView("editContent", { content : content }, "Content: "+content.content);				
	};
	
	$scope.editGroup = function(group) {				
		views.setView("editGroup", { group : group }, "Group: "+group.name);				
	};
	
	$scope.translate = function() {				
		views.setView("translate", {  }, "Translate");				
	};
	
	$scope.addGroup = function() {
		var newGroup = { name : "", label : {}, contents : [], contentEntries : [], system:$scope.groupSystem, action:"create" };
		newGroup.label[$scope.lang] = "";
		$scope.groups.push(newGroup);
		views.setView("editGroup", { group : newGroup }, "New Group");
	};
	
	$scope.isDefaultFor = function(code, content) {
		return content.defaultCode == code.system + " "+ code.code;
	};
	
	$scope.makeDefaultCode = function(code, content) {
		content.defaultCode = code.system + " " + code.code;
		content.action = content.action || "update";
	};
	
	$scope.getStyle = function(elem) {
		if (elem.action == "update") return { "color": "blue" };
		if (elem.action == "create") return { "color": "green" };
		if (elem.action == "delete") return { "color": "red" };
		return { "color" : "black" };
	};
	$scope.getParents = function(name) {
		if (!name) return "";
		var grp = $scope.nameToGroup[name];
		if (grp && grp.parent) return name+" > "+$scope.getParents(grp.parent);
		return name;
	};
	$scope.deleteGroup = function(grp) {
		grp.action = "delete";
	};
	$scope.deleteContent = function(cnt) {
		cnt.action = "delete";
	};
	$scope.deleteCode = function(code) {
		code.action = "delete";
	};
	
	$scope.save = function() {
		$scope.saving = true;
		var next = $q.when();
					
		angular.forEach($scope.codes, function(code) {
			if (code.action == "delete" && code._id != null) {
				next = next.then(function() {
				  code.action = null;				
				  return formats.deleteCode(code);
				});
			}
		});
		
		angular.forEach($scope.contents, function(content) {
			if (content.action == "create") {
				next = next.then(function() {
					content.action = null;
				    return formats.createContent(content);
				});
			} else if (content.action == "update") {
				next = next.then(function() {
					content.action = null;				
				    return formats.updateContent(content);
				});
			} 
		});
		
		angular.forEach($scope.codes, function(code) {
			if (code.action == "create") {
				next = next.then(function() {
					code.action = null;
				    return formats.createCode(code);
				});
			} else if (code.action == "update") {
				next = next.then(function() {
					code.action = null;				
				    return formats.updateCode(code);
				});
			} 
		});
						
		angular.forEach($scope.groups, function(group) {
			if (group.action == "create") {
				next = next.then(function() {
					group.action = null;				
				    return formats.createGroup(group);
				});
				
			} else if (group.action == "update") {
				next = next.then(function() {
					group.action = null;
				  return formats.updateGroup(group);
				});
			} else if (group.action == "delete" && group._id != null) {
				next = next.then(function() {
					group.action = null;				
				    return formats.deleteGroup(group);
				});
			}
		});
		
					
		
		angular.forEach($scope.contents, function(content) {
			if (content.action == "delete" && content._id != null) {
				next = next.then(function() {
					content.action = null;				
				    return formats.deleteContent(content);
				});
			}
		});
		
		
		next.then(function() { $timeout(function() { $scope.saving = false; $scope.init(); }, 2000); });
	};
	
	$scope.init();
}])
.controller('EditCodeCtrl', ['$scope', '$state', '$filter', 'views', 'session', 'formats', 'status', function($scope, $state, $filter, views, session, formats, status) {
	$scope.view = views.getView("editCode");
	
	$scope.reload = function() {
		if (!$scope.view.active) return;
		$scope.code = $scope.view.setup.code;
	};
	
	$scope.submit = function() {
		$scope.code.action = $scope.code.action || "update"; 
		
        var contentchk = $filter("filter")($scope.contents, function(x) { return (x.content == $scope.code.content); });
        
        views.disableView("editCode");
        if (contentchk.length === 0) {
        	var newcontent = { content : $scope.code.content, defaultCode : $scope.code.system+" "+$scope.code.code, security : "MEDIUM", label : { }, action : "create"};
        	newcontent.label[$scope.lang] = $scope.code.display;
        	$scope.contents.push(newcontent);
        	views.setView("editContent", { content : newcontent });
        }
		
		
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });
}])
.controller('EditContentCtrl', ['$scope', '$state', 'views', 'session', 'formats', 'status', function($scope, $state, views, session, formats, status) {
	$scope.view = views.getView("editContent");
	
	$scope.reload = function() {
		if (!$scope.view.active) return;
		$scope.contentEntry = $scope.view.setup.content;
		$scope.oldGroup = $scope.contentEntry.group;
		$scope.oldName = $scope.contentEntry.content;
	};
	
	$scope.submit = function() {
		views.disableView("editContent");
		$scope.contentEntry.action = $scope.contentEntry.action || "update";
		if ($scope.oldGroup != $scope.contentEntry.group || $scope.oldName != $scope.contentEntry.content) {
			var old = $scope.nameToGroup[$scope.oldGroup];
			var newGroup = $scope.nameToGroup[$scope.contentEntry.group];
			
			if (old) {
			  old.action = old.action || "update";
			  old.contents.splice(old.contents.indexOf($scope.oldName), 1);
			  old.contentEntries.splice(old.contentEntries.indexOf($scope.contentEntry), 1);
			  $scope.checkGroupValid(old);
			}
			newGroup.action = newGroup.action || "update";
			newGroup.contents.push($scope.contentEntry.content);
			newGroup.contentEntries.push($scope.contentEntry);			
			$scope.checkGroupValid(newGroup);
		}
		if ($scope.oldName != $scope.contentEntry.content) {
			angular.forEach($scope.codes, function(c) {
				if (c.content == $scope.oldName) {
					c.content = $scope.contentEntry.content;
					c.action = c.action || "update";
				}
			});
		}
	};
	
	$scope.addGroup = function() {
		var newGroup = { parent:$scope.contentEntry.group, name : $scope.contentEntry.content, label : {}, contents : [], contentEntries : [], system:$scope.groupSystem, action:"create" };
		newGroup.label[$scope.lang] = $scope.contentEntry.label[$scope.lang];
		$scope.groups.push(newGroup);
		views.setView("editGroup", { group : newGroup, content : $scope.contentEntry }, "New Group");
	};
		
	
	$scope.$watch('view.setup', function() { $scope.reload(); });
}])
.controller('EditGroupCtrl', ['$scope', '$state', 'views', 'session', 'formats', 'status', function($scope, $state, views, session, formats, status) {
	$scope.view = views.getView("editGroup");
	
	$scope.reload = function() {
		if (!$scope.view.active) return;
		$scope.group = $scope.view.setup.group;
	};
	
	$scope.submit = function() {
		if ($scope.view.setup.content) {
			$scope.view.setup.content.group = $scope.group.name;
		}
		$scope.group.action = $scope.group.action || "update";
		$scope.nameToGroup[$scope.group.name] = $scope.group;
		$scope.checkGroupValid($scope.group);
		views.disableView("editGroup");
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });
}])
.controller('TranslateCtrl', ['$scope', '$state', 'views', 'session', 'formats', 'status', function($scope, $state, views, session, formats, status) {
	$scope.view = views.getView("translate");
	
	$scope.reload = function() {
		if (!$scope.view.active) return;		
	};
	
	$scope.submit = function() {
		
		
		views.disableView("translate");
	};
	
	$scope.changed = function(itm) {
		itm.action = itm.action || "update";
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });
}]);
