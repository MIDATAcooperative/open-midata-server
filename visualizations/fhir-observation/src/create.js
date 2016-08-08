angular.module('fhirObservation')
.controller('CreateCtrl', ['$scope', '$timeout', '$filter', '$state', 'midataServer', 'midataPortal', 'fhirinfo', 'configuration', 'data',
	function($scope, $timeout, $filter, $state, midataServer, midataPortal, fhirinfo, configuration, data) {
		
		// init
		$scope.errors = null;
		$scope.datePickers = {};
	    $scope.dateOptions = {
	       formatYear: 'yy',
	       startingDay: 1,	    
	    };
	    $scope.datePopupOptions = {	 	       
	 	   popupPlacement : "auto top-right"
	 	};
											
		$scope.isValid = true;
		$scope.isBusy = false;
		$scope.success = false;
		
		$scope.getCodeableConcept = data.getCodeableConcept;
		
		var measure = $scope.measure = $state.params.measure;
		
		//fhirinfo.loadLabels(midataPortal.language, measure);
		fhirinfo.getInfos(midataPortal.language, measure)
		.then(function(format) {
			  console.log(format);
			  $scope.format = format[0];
			  $scope.reset();
		});
				
		$scope.getLabel = fhirinfo.getLabel;
		$scope.reset = function() {
			$scope.newentry = { 
					resourceType : "Observation",
					status : "preliminary",					
					code : $scope.format.code,
					effectiveDateTime : new Date()
			}; 
			if ($scope.format.category) $scope.newentry.category = $scope.format.category;
			switch ($scope.format.type) {
			  case "Quantity" : $scope.newentry.valueQuantity = $scope.format.valueQuantity;break;
			  case "component" :  $scope.newentry.component = $scope.format.component;break;
			} 
			console.log($scope.newentry);
		};
		
		
		$scope.add = function() {
			$scope.success = false;
			$scope.error = $scope.errorValue = null;
			var theDate = new Date($scope.newentry.effectiveDateTime);
			if (isNaN(theDate)) {
				$scope.error = "Please enter a valid date! (YYYY-MM-DD)";
				return;
			}			
																					
			$scope.isBusy = true;
			midataServer.createRecord(midataServer.authToken, { "name" : $scope.format.label, "content" : $scope.format.content, format : "fhir/Observation", subformat : $scope.format.type }, $scope.newentry )
			.then(function() { 
				$scope.success = true; 
				$scope.isBusy = false; 
				$scope.reset(); 							
				$timeout(function() { 
					$scope.success = false;
					midataPortal.updateNotification();
					midataPortal.doneNotification();
					if ($scope.init) $scope.init();
			   }, 2000); 
			});			
		};
														
	}]);