angular.module('services')
.factory('crypto', ['$q', function($q) {
	var service = {};
	
	var forge = require('node-forge');		
	var rsa = forge.pki.rsa;
	var ssss = require('../../secrets.js');
	
	var recoveryPubKeys = require('../../../../conf/recoverykeys.json');
		
	service.generateKeys = function(password) {
							
		var def = $q.defer();
		
		rsa.generateKeyPair({bits: 2048, workers: 2}, function(err, keypair) {
					
			var result = {};
									
			result.priv_pw = forge.pki.encryptRsaPrivateKey(keypair.privateKey, password,
					{legacy: true, algorithm: 'aes128'}); 
			
			//var back = forge.pki.decryptRsaPrivateKey(ossh, password);
			//console.log(back);
			
			result.pub = forge.pki.publicKeyToPem(keypair.publicKey);
			
			var hash = forge.md.sha512.create();
			hash.update(password);		
			
			result.pw_hash = hash.digest().toHex();	
			result.recoverKey = forge.util.encode64(forge.random.getBytesSync(16));
			
			result.recovery = service.createRecoveryInfo(keypair.privateKey);
			def.resolve(result);
		});
		
		return def.promise;
	};
		
	
	service.encryptPK_AES = function(privkeyStr) {
		var key = forge.random.getBytesSync(16);
		var iv = forge.random.getBytesSync(16);
		var cipher = forge.cipher.createCipher('AES-CBC', key);
		cipher.start({iv: iv});
		cipher.update(forge.util.createBuffer(privkeyStr));
		cipher.finish();
		var encrypted = cipher.output;
		
		return { key : key, iv : iv, encrypted : encrypted };
	};
	
	service.decryptPK_AES = function(keyinfo) {
				
		var decipher = forge.cipher.createDecipher('AES-CBC', keyinfo.key);	
		decipher.start({iv: forge.util.decode64(keyinfo.iv) });
		decipher.update(forge.util.createBuffer(forge.util.decode64(keyinfo.encrypted)));	
		var result = decipher.finish(); // check 'result' for true/false
	
		// outputs decrypted hex
		return decipher.output.toString();
		
	};
	
	service.createRecoveryInfo = function(pk) {
		if (recoveryPubKeys && recoveryPubKeys.length && recoveryPubKeys[0].neededKeys && recoveryPubKeys[0].neededKeys > 0) {
			var pkstr = forge.pki.privateKeyToPem(pk);
			var enc = service.encryptPK_AES(pkstr);		
			var neededKeys = recoveryPubKeys[0].neededKeys;
			var pkhex = forge.util.binary.hex.encode(enc.key);	
			var shares = ssss.share(pkhex, recoveryPubKeys.length-1, neededKeys);
			//console.log(shares);
			var recovery = { "encrypted" : forge.util.encode64(enc.encrypted.getBytes()), "iv" : forge.util.encode64(enc.iv) };
			for (var idx = 0;idx < recoveryPubKeys.length-1; idx++) {
				var pubkey = forge.pki.publicKeyFromPem(recoveryPubKeys[idx+1].key);		
				var encoded = shares[idx]; //forge.util.binary.hex.encode(shares[idx]);			
							
				var encrypted = pubkey.encrypt(encoded); //service.encryptKEM(pubkey, encoded);
				recovery[recoveryPubKeys[idx+1].user] = forge.util.encode64(encrypted);
			}
			
			return recovery;		
		} else return null;
	};
	
	service.keysNeeded = function() {
	  return recoveryPubKeys[0].neededKeys;
	};
	
	service.dorecover = function(recovery, challenge) {
		var rec = [];
		angular.forEach(recovery, function(v,k) {
			if (k!="encrypted" && k!="iv") rec.push(v);
		});	
		var combined = ssss.combine(rec);
		recovery.key = forge.util.hexToBytes(combined);
		var pkstr = service.decryptPK_AES(recovery);
			
		
		var challenge = forge.util.decode64(challenge);	
		var pk = forge.pki.privateKeyFromPem(pkstr);	
		return forge.util.encode64(pk.decrypt(challenge, "RSA-OAEP", {
			  md: forge.md.sha256.create(),
			  mgf1: {
			    md: forge.md.sha1.create()
			  }
		}));
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
		var pk = forge.pki.decryptRsaPrivateKey(priv_pw, password);	
		return forge.util.encode64(pk.decrypt(challenge, "RSA-OAEP", {
			  md: forge.md.sha256.create(),
			  mgf1: {
			    md: forge.md.sha1.create()
			  }
		}));
	};
	
	service.checkLocalRecovery = function(username, recoverKey, priv_pw, password) {
		var hash = forge.md.sha256.create();
		hash.update(username);
		var key = hash.digest().toHex();
		// if (localStorage["recover_"+key]) return;
		
		var pk = forge.pki.decryptRsaPrivateKey(priv_pw, password);
		var pkstr = forge.pki.privateKeyToPem(pk);	
		var iv = forge.random.getBytesSync(16);
		var cipher = forge.cipher.createCipher('AES-CBC', forge.util.decode64(recoverKey));
		cipher.start({iv: iv});
		cipher.update(forge.util.createBuffer(pkstr));
		cipher.finish();
		var encrypted = cipher.output.getBytes();		
		   //console.log(encrypted);     	
		localStorage["recover_"+key] = JSON.stringify({ encrypted : forge.util.encode64(encrypted), iv : forge.util.encode64(iv) });
		
	};
	
	service.hasLocalRecovery = function(username) {
		var hash = forge.md.sha256.create();
		hash.update(username);
		var key = hash.digest().toHex();
		var rec = localStorage["recover_"+key];
		if (rec) {
			var parsed = JSON.parse(rec);
			if (parsed.encrypted && parsed.iv) return true;
		}
		return false;
	};
	
	service.keyChallengeLocal = function(username, recoverKey, challenge) {
		var hash = forge.md.sha256.create();
		hash.update(username);
		var key = hash.digest().toHex();
		var rec = localStorage["recover_"+key];
		if (rec) {
			var parsed = JSON.parse(rec);
			if (parsed.encrypted && parsed.iv) {
				var decipher = forge.cipher.createDecipher('AES-CBC', forge.util.decode64(recoverKey));	
				decipher.start({iv: forge.util.decode64(parsed.iv) });
				decipher.update(forge.util.createBuffer(forge.util.decode64(parsed.encrypted)));	
				var result = decipher.finish(); // check 'result' for true/false
				if (!result) return null;
			    var pkstr = decipher.output.toString();			   
				var pk = forge.pki.privateKeyFromPem(pkstr);
				var challenge = forge.util.decode64(challenge);	
				return forge.util.encode64(pk.decrypt(challenge, "RSA-OAEP", {
					  md: forge.md.sha256.create(),
					  mgf1: {
					    md: forge.md.sha1.create()
					  }
				}));				
			}
		}
		return null;
	};
	
	
	return service;
	
}]);