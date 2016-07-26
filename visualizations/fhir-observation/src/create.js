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
		
		fhirinfo.loadLabels(midataPortal.language, measure);
		fhirinfo.getInfo(measure)
		.then(function(format) {
			  console.log(format);
			  $scope.format = format;
			  $scope.reset();
		});
				
		$scope.getLabel = fhirinfo.getLabel;
		$scope.reset = function() {
			$scope.newentry = { 
					resourceType : "Observation",
					status : "preliminary",
					category : {
						coding : [
				           {
				             "system": "http://hl7.org/fhir/observation-category",
				             "code": "fitness",
				             "display": "Fitness Data"
				           }
						],
/*						coding : [{
						  system : "http://hl7.org/fhir/observation-category",
						  code : "vital-signs",
						  display : "Vital Signs"
						}],*/
						text : "Fitness Data"
					},
					code : $scope.format.code,
					effectiveDateTime : new Date() /*$filter('date')(new Date(), "yyyy-MM-dd")*/
			}; 
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
				$timeout(function() { $scope.success = false;midataPortal.updateNotification();midataPortal.doneNotification(); }, 2000); 
			});			
		};
														
	}]);