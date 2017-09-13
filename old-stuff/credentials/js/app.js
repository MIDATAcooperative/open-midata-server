var credentials = angular.module('credentials', [ 'midata' ]);
credentials.controller('CredentialsCtrl', ['$scope', '$http', '$location', 'midataServer', 'midataPortal',
	function($scope, $http, $location, midataServer, midataPortal) {
		
	    midataPortal.autoresize();
	    
		
		
		// get authorization token
		var authToken = $location.path().split("/")[1];
		
		$scope.init = function() {
			$scope.loading = true;
			$scope.error = null;
			$scope.empty = false;
			$scope.authorized = false;
			$scope.encrypted = {};
			$scope.decrypted = [];
			$scope.passphrase = null;
			$scope.current = null;
			$scope.mode = "view";
			
			// get the ids of the records assigned to this space				
			midataServer.getRecords(authToken, { format : "credentials-manager", content : "account/credentials" }, [ "name", "data" ])
			.then(function(results) {
				prepareRecords(results.data);
			}, function(err) {
					$scope.error = "Failed to load records: " + err.data;
					$scope.loading = false;
			});
		};
				
		// prepare records (index by passphrase)
		prepareRecords = function(records) {
			_.each(records, function(record) {
				if (record.data.passphrase && record.data.credentials) {
					if (!$scope.encrypted[record.data.passphrase]) {
						$scope.encrypted[record.data.passphrase] = [];
					}
					$scope.encrypted[record.data.passphrase] = _.union($scope.encrypted[record.data.passphrase], record.data.credentials);
				}
			});
			if (!_.size($scope.encrypted)) {				
				$scope.empty = true;
				$scope.newDatabase();
			}
			$scope.loading = false;
		};

		// check whether entered passphrase matches any of the indexed passphrases
		$scope.checkPassphrase = function() {
			var hashedPassphrase = CryptoJS.SHA512($scope.passphrase).toString();
			if (_.has($scope.encrypted, hashedPassphrase)) {
				$scope.decrypted = _.map($scope.encrypted[hashedPassphrase], function(credentials) {
					return {
						"site": decrypt(credentials.site),
						"username": decrypt(credentials.username),
						"password": decrypt(credentials.password)
					};
				});
				$scope.credentials = $scope.decrypted;
				$scope.error = null;
				$scope.authorized = true;
			} else {
				$scope.error = "Passphrase did not match any passphrases of the assigned records.";
			}
		};

		// decrypt the respective ciphertext with the current passphrase
		decrypt = function(secret) {
			return CryptoJS.AES.decrypt(secret, $scope.passphrase).toString(CryptoJS.enc.Utf8);
		};

		// reset the passphrase
		$scope.reset = function() {
			$scope.decrypted = [];
			$scope.passphrase = null;
			$scope.current = null;
			$scope.authorized = false;
		};
		
		// init
		$scope.errors = {};
		$scope.credentials = [{ // add one set of initial credentials
			"site": null,
			"username": null,
			"password": null,
			"error": false
		}];
		
		// controller functions
		$scope.addCredentials = function() {
			$scope.credentials.push({
				"site": null,
				"username": null,
				"password": null,
				"error": false
			});
		};

		$scope.removeCredentials = function(credentials) {
			$scope.credentials.splice($scope.credentials.indexOf(credentials), 1);
		};

		$scope.validate = function() {
			$scope.loading = true;
			$scope.errors = {};
			$scope.validateTitle();
			$scope.validatePassphrase();
			var credentialsComplete = $scope.validateCredentials();
			if(!$scope.errors.title && !$scope.errors.passphrase && credentialsComplete) {
				$scope.submit()
			} else {
				$scope.loading = false;
			}
		};
		
		$scope.validateTitle = function() {
			$scope.errors.title = null;
			if (!$scope.title) {
				$scope.errors.title = "Please provide a title for your record.";
			} else if ($scope.title.length > 50) {
				$scope.errors.title = "Please provide a title with fewer than 50 characters.";
			}
		};

		$scope.validatePassphrase = function() {
			$scope.errors.passphrase = null;
			if (!$scope.passphrase) {
				$scope.errors.passphrase = "Please provide a passphrase to encrypt your credentials.";
			} else if (!$scope.passphrase2) {
				$scope.errors.passphrase = "Please re-enter the passphrase.";
			} else if ($scope.passphrase !== $scope.passphrase2) {
				$scope.errors.passphrase = "Passphrases entered do not match.";
			} else if (8 > $scope.passphrase.length || $scope.passphrase.length > 100) {
				$scope.errors.passphrase = "Please provide a passphrase between 8 and 100 characters.";
			}
		};

		$scope.validateCredentials = function() {
			// only proceed if there are any credentials
			if ($scope.credentials.length === 0) {
				$scope.errors.global = "Please provide at least one set of credentials.";
				$scope.errors.credentials = true;
				return false;
			}

			// check whether all fields are filled in (no further checks for length or content)
			var complete = true;
			for (var i = 0; i < $scope.credentials.length; i++) {
				var cur = $scope.credentials[i];
				if (!cur.site || !cur.username || !cur.password) {
					complete = false;
					cur.error = true;
				} else {
					cur.error = false;
				}
			}
			return complete;
		};
		
		$scope.submit = function() {
			// format date in the form "YYYY-MM-dd"
			var now = new Date();
			var formattedDate = now.getFullYear() + "-" + 
				("0" + (now.getMonth() + 1)).slice(-2) + "-" + 
				("0" + now.getDate()).slice(-2);

			// hash passphrase for checking in visualization
			var record = {};
			record.passphrase = CryptoJS.SHA512($scope.passphrase).toString();

			// encrypt credentials
			record.credentials = [];
			for (var i = 0; i < $scope.credentials.length; i++) {
				var cur = $scope.credentials[i];
				record.credentials.push({
					"site": CryptoJS.AES.encrypt(cur.site, $scope.passphrase).toString(),
					"username": CryptoJS.AES.encrypt(cur.username, $scope.passphrase).toString(),
					"password": CryptoJS.AES.encrypt(cur.password, $scope.passphrase).toString()
				});
			}

			midataServer.createRecord(authToken, { "name" : $scope.title, "description" : $scope.title + " created with the credentials app on " + formattedDate, "content" : "account/credentials", "format" : "credentials-manager" }, record)
			.then(function() {					
					$scope.title = null;
					$scope.description = null;
					$scope.data = null;
					$scope.loading = false;
					$scope.init();
				}, function(err) {
					$scope.success = null;
					$scope.errors.server = err.data;
					$scope.loading = false;
			});
		};

		$scope.newDatabase = function() {
			$scope.mode = "edit";
			$scope.errors = {};
			$scope.credentials = [{ // add one set of initial credentials
				"site": null,
				"username": null,
				"password": null,
				"error": false
			}];
			$scope.passphrase = "";
		};
		
		$scope.cancel = function() {
		   $scope.mode = "view";
		   $scope.passphrase = "";
		};
		
		$scope.init();

	}
]);
