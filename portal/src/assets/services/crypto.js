angular.module('services')
.factory('crypto', ['$q', function($q) {
	var service = {};
	
	var forge = require('node-forge');		
	var rsa = forge.pki.rsa;
	var ssss = require('../../secrets.js');
	
	var recoveryPubKeys = [
		{ user : "ak", key : "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6zaIr1vG7n5hrEV859e1ToClenORABtVEWhoNpruYHZMmK7p0G8S1nHJvqz8RK+W0KAwZe4L1nPRLWdPyPUbHGEWUNsqtABCHg/fxoyISqWKjLxoXg8K1kgdvNOybK9X4oLYGf+qcjb0adbxUQrEXgJT3us8EMh2tzDsmLhF5OFjWvXHl98/OhbMTL0Kr1XQ2amY9niUHvbNP8A/SpOxSVB5EewHZbcuKMQfV5YVbRFaiXta9qTwxPLDKbjX8LgTPg9Zz7En8M1/DDJi6tgExlIl0TS0WvO/8bflDECXqzuD3g0vRnlXKGJFQWfuvbOipDyFI7ymUCf62K4grYCEuwIDAQAB-----END PUBLIC KEY-----" },
		{ user : "od", key : "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArldtZzvrGrxcIbFLVzngc4CIpnBin+igYsMh3PcZbzq87NsoVyEukkFs3rcFXrpEk1bcG6NjLfEHGcgihZIPEWjEA2k5CYsXy0eKdG46MQfHbuNNnQYGm7SqlxyHuXIKxokAYqgPBk3vWAOUfK+QmyeKC3F+NcvT62yKlm++U7wsPKG1lrPCY8NaPuCvcZeY+HAXnREHQdp5sN4AlMb66yjdKAMCObGfAAbbuZRsi3zO/34SDP+7/jmJmm5SzmDadOymKqo3QC58oV0sZvrd/vh1Lv6XNTPbSX+Bg5+S+2jX21HV2IOhFbHJQt+vSxaE+8ggKDt5chxIYQDcKRMcaQIDAQAB-----END PUBLIC KEY-----" }
	];
	
	
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
		var pkstr = forge.pki.privateKeyToPem(pk);
		var enc = service.encryptPK_AES(pkstr);
			
		var pkhex = forge.util.binary.hex.encode(enc.key);	
		var shares = ssss.share(pkhex, recoveryPubKeys.length, 2);
		console.log(shares);
		var recovery = { "encrypted" : forge.util.encode64(enc.encrypted.getBytes()), "iv" : forge.util.encode64(enc.iv) };
		for (var idx = 0;idx < recoveryPubKeys.length; idx++) {
			var pubkey = forge.pki.publicKeyFromPem(recoveryPubKeys[idx].key);		
			var encoded = shares[idx]; //forge.util.binary.hex.encode(shares[idx]);			
						
			var encrypted = pubkey.encrypt(encoded); //service.encryptKEM(pubkey, encoded);
			recovery[recoveryPubKeys[idx].user] = forge.util.encode64(encrypted);
		}
		
		return recovery;		
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
	
	
	return service;
	
}]);