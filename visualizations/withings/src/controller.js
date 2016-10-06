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
			importer.importNow(authToken);
			$scope.status = "ok";
		};

		$scope.saveConfig = function(){
			importer.saveConfig(authToken);
		};			
	}
]);
