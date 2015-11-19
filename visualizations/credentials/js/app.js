var credentials = angular.module('credentials', [ 'midata' ]);
credentials.controller('CredentialsCtrl', ['$scope', '$http', '$location', 'midataServer', 'midataPortal',
	function($scope, $http, $location, midataServer, midataPortal) {
		
	    midataPortal.autoresize();
	    
		// init
		$scope.loading = true;
		$scope.error = null;
		$scope.empty = false;
		$scope.authorized = false;
		$scope.encrypted = {};
		$scope.decrypted = [];
		$scope.passphrase = null;
		$scope.current = null;

		// get authorization token
		var authToken = $location.path().split("/")[1];
		
		// get the ids of the records assigned to this space
		var data = {"authToken": authToken};
		$http.post("https://" + window.location.hostname +
			":9000/v1/plugin_api/records/ids", JSON.stringify(data)).
			success(function(recordIds) {
				getRecords(recordIds);
			}).
			error(function(err) {
				$scope.error = "Failed to load records: " + err;
				$scope.loading = false;
			});

		// get the records
		getRecords = function(recordIds) {
			data.properties = {"_id": recordIds};
			data.fields = ["data"];
			$http.post("https://" + window.location.hostname +
				":9000/v1/plugin_api/records/search", JSON.stringify(data)).
				success(function(records) {
					prepareRecords(records);
				}).
				error(function(err) {
					$scope.error = "Failed to load records: " + err;
					$scope.loading = false;
				});
		}
		
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
				$scope.error = "No records found. Be sure to assign records to this space" +
					" that have been created with the Credentials Manager app.";
				$scope.empty = true;
			}
			$scope.loading = false;
		}

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
				$scope.error = null;
				$scope.authorized = true;
			} else {
				$scope.error = "Passphrase did not match any passphrases of the assigned records.";
			}
		}

		// decrypt the respective ciphertext with the current passphrase
		decrypt = function(secret) {
			return CryptoJS.AES.decrypt(secret, $scope.passphrase).toString(CryptoJS.enc.Utf8);
		}

		// reset the passphrase
		$scope.reset = function() {
			$scope.decrypted = [];
			$scope.passphrase = null;
			$scope.current = null;
			$scope.authorized = false;
		}

	}
]);
