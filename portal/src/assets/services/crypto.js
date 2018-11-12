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
			
			service.createRecoveryInfo(keypair.privateKey);
			def.resolve(result);
		});
		
		return def.promise;
	};
	
	service.encryptKEM = function(pubkey, message) {
		// generate and encapsulate a 16-byte secret key
		var kdf1 = new forge.kem.kdf1(forge.md.sha1.create());
		var kem = forge.kem.rsa.create(kdf1);
		var result = kem.encrypt(pubkey, 16);
		// result has 'encapsulation' and 'key'

		// encrypt some bytes
		var iv = forge.random.getBytesSync(12);		
		var cipher = forge.cipher.createCipher('AES-GCM', result.key);
		cipher.start({iv: iv});
		cipher.update(forge.util.createBuffer(message));
		cipher.finish();
		var encrypted = cipher.output.getBytes();
		var tag = cipher.mode.tag.getBytes();
		console.log("ENC:");
		console.log(encrypted);
		console.log("TAG:");
		console.log(tag);
		return encrypted;
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
		
		console.log("A");		
		var decipher = forge.cipher.createDecipher('AES-CBC', keyinfo.key);
		console.log("B");
		decipher.start({iv: forge.util.decode64(keyinfo.iv) });
		console.log("C");
		decipher.update(forge.util.createBuffer(forge.util.decode64(keyinfo.encrypted)));
		console.log("D");
		var result = decipher.finish(); // check 'result' for true/false
		console.log(result);
		// outputs decrypted hex
		return decipher.output.toString();
		
	};
	
	service.createRecoveryInfo = function(pk) {
		var pkstr = forge.pki.privateKeyToPem(pk);
		var enc = service.encryptPK_AES(pkstr);
		
		console.log("PK:"+pkstr);
		console.log(enc);
		var pkhex = forge.util.binary.hex.encode(enc.key);
		console.log("HEX:"+pkhex);
		var shares = ssss.share(pkhex, recoveryPubKeys.length, 2);
		console.log("SHARES:");
		console.log(shares);
		var recovery = { "encrypted" : forge.util.encode64(enc.encrypted.getBytes()), "iv" : forge.util.encode64(enc.iv) };
		for (var idx = 0;idx < recoveryPubKeys.length; idx++) {
			var pubkey = forge.pki.publicKeyFromPem(recoveryPubKeys[idx].key);
			var encoded = forge.util.binary.hex.encode(shares[idx]);
			console.log(encoded);
			console.log("LEN:"+encoded.length);
						
			var encrypted = pubkey.encrypt(encoded); //service.encryptKEM(pubkey, encoded);
			recovery[recoveryPubKeys[idx].user] = forge.util.encode64(encrypted);
		}
		
		return recovery;
		/*
		recovery.key = forge.util.hexToBytes(pkhex);
		console.log(recovery);
		
		var pkcheck = service.decryptPK_AES(recovery);
		console.log(pkcheck);
		console.log(pkcheck == pkstr);
		*/
		/*
		var comb = ssss.combine( [ shares[0], shares[1] ] );

		// convert back to UTF string
		comb = ssss.hex2str(comb);
		
		console.log(comb);
		console.log(comb == pkstr);*/
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