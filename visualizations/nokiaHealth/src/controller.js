// The frontend controller
nokiaHealth.controller('ImportController', ['$scope', '$translate', '$location', 'midataServer', 'midataPortal', 'importer',
	function($scope, $translate, $location, midataServer, midataPortal, importer) {

	    // Make layout fit into MIDATA page
	    midataPortal.autoresize();

	    // Use language from MIDATA portal
	    $translate.use(midataPortal.language);

		// get authorization token from portal
		var authToken = $location.search().authToken;

		$scope.importer = importer;
		importer.firstTime = false;

		importer.initForm(authToken);

		$scope.Import = function(){
			importer.firstTime = true;
			importer.saveConfig()
			.then(function(){
				return importer.initForm(authToken);
			})
			.then(function(){
				return importer.importNow();
			})
			.then(function(){
				return importer.initForm(authToken);
			});			
			
			$scope.status = "ok";
		};

		$scope.saveConfig = function(){
			importer.saveConfig()
			.then(function(){
            	importer.status = "changesSaved";
        	});
		};

		$scope.progress = function() {
			var r = $scope.importer.requested > 0 ? $scope.importer.requested : 1;
			return { 'width' : (($scope.importer.saved + $scope.importer.requestingDone) * 100 / (r+$scope.importer.requesting))+"%" };
		};
	}
]);
nokiaHealth.controller('PreviewCtrl', ['$scope', '$translate', '$location', 'midataServer', 'midataPortal', 'importer',
	function($scope, $translate, $location, midataServer, midataPortal, importer) {
		$translate.use(midataPortal.language);
        $scope.importer = importer;

		// get authorization token
		var authToken = $location.search().authToken;

		$scope.importer.initForm(authToken);

	}
]);
