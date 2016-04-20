angular.module('portal')
.controller('ContentEditorCtrl', ['$scope', '$state', 'views', 'session', 'formats', 'status', function($scope, $state, views, session, formats, status) {

	$scope.status = new status(true);
	$scope.lang = "en";
	$scope.security = ["MEDIUM", "HIGH"];
    $scope.nameToGroup = {};
    $scope.nameToContent = {};
    $scope.groupSystem = "v1";
	
	$scope.init = function() {		
		  $scope.status.doBusy(formats.listCodes())
		  .then(function(data) { $scope.codes = data.data;$scope.prepare(); });		  		  
		  
		  $scope.status.doBusy(formats.listContents())
		  .then(function(data) { $scope.contents = data.data;$scope.prepare(); });
		  
		  $scope.status.doBusy(formats.listGroups())
		  .then(function(data) { $scope.groups = data.data;$scope.prepare(); });
	};
	
	$scope.prepare = function() {
		if ($scope.codes && $scope.contents && $scope.groups) {
			angular.forEach($scope.contents , function(cnt) {
				$scope.nameToContent[cnt.content] = cnt;
			});
			angular.forEach($scope.groups, function(grp) {
				$scope.nameToGroup[grp.name] = grp;
				grp.contentEntries = [];
				if (grp.contents) {
					angular.forEach(grp.contents, function(cnt) {
						var c = $scope.nameToContent[cnt];
						console.log(c);
						if (c != null) {
							grp.contentEntries.push(c);
							c.group = grp.name;
						}
					});				
				}
			});
			
		}
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
	
	$scope.save = function() {
		
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
		if ($scope.oldGroup != $scope.contentEntry.group) {
			var old = $scope.nameToGroup[$scope.oldGroup];
			var newGroup = $scope.nameToGroup[$scope.contentEntry.group];
			console.log($scope.contentEntry.group);
			console.log(newGroup);
			if (old) {
			  old.action = old.action || "update";
			  old.contents.splice(old.contents.indexOf($scope.oldName), 1);
			  old.contentEntries.splice(old.contentEntries.indexOf($scope.contentEntry), 1);
			}
			newGroup.action = newGroup.action || "update";
			newGroup.contents.push($scope.contentEntry.content);
			newGroup.contentEntries.push($scope.contentEntry);			
		}
	};
	
	$scope.addGroup = function() {
		var newGroup = { name : $scope.contentEntry.content, label : {}, contents : [], contentEntries : [], system:$scope.groupSystem, action:"create" };
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
		views.disableView("editGroup");
	};
	
	$scope.$watch('view.setup', function() { $scope.reload(); });
}]);
