// The frontend controller
withings.controller('ImportController', ['$scope', '$translate', '$location', 'midataServer', 'midataPortal', 'importer',  
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
			importer.saveConfig(authToken);
			importer.importNow(authToken);
			$scope.status = "ok";
		};

		$scope.saveConfig = function(){
			importer.saveConfig(authToken);
		};		
						
		$scope.progress = function() {
			var r = $scope.importer.requested > 0 ? $scope.importer.requested : 1;
			return { 'width' : ($scope.importer.saved * 100 / r)+"%" };
		};	
	}
]);
withings.controller('PreviewCtrl', ['$scope', '$translate', '$location', 'midataServer', 'midataPortal', 'importer',  
	function($scope, $translate, $location, midataServer, midataPortal, importer) {
		$translate.use(midataPortal.language);
        $scope.importer = importer;	    
					
		// get authorization token
		var authToken = $location.search().authToken;

		$scope.importer.initForm(authToken);
		
	}
]);
