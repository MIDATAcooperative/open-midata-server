angular.module('services')
.factory('crypto', ['$q', function($q) {
	var service = {};
	
	var forge = require('node-forge');		
	var rsa = forge.pki.rsa;
	
	service.generateKeys = function(password) {
		console.log(forge);
						
		var def = $q.defer();
		
		rsa.generateKeyPair({bits: 2048, workers: 2}, function(err, keypair) {
					
			var result = {};
			
			console.log(keypair.privateKey);
			console.log(keypair.publicKey);
			
			result.priv_pw = forge.pki.encryptRsaPrivateKey(keypair.privateKey, password,
					{legacy: true, algorithm: 'aes128'}); 
			
			//var back = forge.pki.decryptRsaPrivateKey(ossh, password);
			//console.log(back);
			
			result.pub = forge.pki.publicKeyToPem(keypair.publicKey);
			
			var hash = forge.md.sha512.create();
			hash.update(password);		
			
			result.pw_hash = hash.digest().toHex();
			console.log(result);
			def.resolve(result);
		});
		
		return def.promise;
	};
	
	service.getHash = function(password) {
		var hash = forge.md.sha512.create();
		hash.update(password);				
		return hash.digest().toHex();
	};
	
	service.makeChallenge = function(pub, input) {
		var pubkey = forge.pki.publicKeyFromPem(pub);
		var inp = pubkey.encrypt(input, 'RSA-OAEP', {
			  md: forge.md.sha256.create(),
			  mgf1: {
			    md: forge.md.sha1.create()
			  }
			});
		var r = forge.util.encode64(inp);		
		return r;
	};
		
	service.keyChallenge = function(priv_pw, password, challenge) {
		var challenge = forge.util.decode64(challenge);
		console.log("A:"+challenge);
		var pk = forge.pki.decryptRsaPrivateKey(priv_pw, password);
		console.log("B:"+pk);
		console.log(challenge.length);
		return forge.util.encode64(pk.decrypt(challenge, "RSA-OAEP", {
			  md: forge.md.sha256.create(),
			  mgf1: {
			    md: forge.md.sha1.create()
			  }
		}));
	};
	
	
	return service;
	
}]);