var
  forge = require('node-forge'),
  chai = require('chai'),
  chaiHttp = require('chai-http'),
  config = require('../files/configurations');
  chai.use(chaiHttp);
  const should = chai.should();

let service = {
	getHash(password) {
		var hash = forge.md.sha512.create();
		hash.update(password);				
		return hash.digest().toHex();
    },

    keyChallenge(priv_pw, password, challenge) {
		var challenge = forge.util.decode64(challenge);	
		var pk = forge.pki.decryptRsaPrivateKey(priv_pw, password);	
		return forge.util.encode64(pk.decrypt(challenge, "RSA-OAEP", {
			  md: forge.md.sha256.create(),
			  mgf1: {
			    md: forge.md.sha1.create()
			  }
		}));
	},
	
	request() {
		return chai.request(config.configs.environment);		
	},
	
	more(p) {
		p.portal = function(user) {
			return p			 
		     .set("X-Session-Token", user.sessionToken)		     
		    ;
		}
		p.user = function(user) {
			return p
			 .set("Authorization","Bearer "+user.token)		   
		     .set("Prefer", "return=representation")
		    ;
		}
		return p;
	}, 
		
	post(path) {
		return service.more(service.request().post(path));				
	},
	
	get(path) {
		return service.more(service.request().get(path));		
	},
	
	put(path) {
		return service.more(service.request().put(path));
	},
	
	async login(user) {
	  let data = {
		"email": user.email, 
		"password": service.getHash(user.password), 
		"role" : user.role  
	  };

      let first = await service.request()
      .post("/api/members/login")
      .send(data);      
	  first.should.have.status(200);
      first.should.be.json;
      first.body.should.have.property("challenge");
      first.body.should.have.property("keyEncrypted");
      first.body.should.have.property("sessionToken");

      data.loginToken = first.body.sessionToken;
      data.sessionToken = service.keyChallenge(first.body.keyEncrypted, user.password, first.body.challenge);
   
      let second = await service.request()
      .post("/api/members/login")
      .send(data);            
      second.should.have.status(200);
      second.should.be.json;
      second.body.should.have.property("sessionToken");
      
      user.sessionToken = second.body.sessionToken;
    },

    async oauth2(app, user) {
	   let data = {
		"appname": app.appName,
		"redirectUri": config.configs.environment+"/#/admin/appdebug",
		"state":"none",
		"device":"debug",
		"username": user.email,		 
		"password": service.getHash(user.password), 
		"role" : user.role,
		"confirm": true,
		"confirmStudy": true  
	  };

      let first = await service.request()
      .post("/v1/authorize")
      .send(data);
      
	  first.should.have.status(200);
      first.should.be.json;
      first.body.should.have.property("challenge");
      first.body.should.have.property("keyEncrypted");
      first.body.should.have.property("sessionToken");    
      data.loginToken = first.body.sessionToken;
      data.sessionToken = service.keyChallenge(first.body.keyEncrypted, user.password, first.body.challenge);
   
      let second = await service.request()
      .post("/v1/authorize")
      .send(data);
            
      second.should.have.status(200);
      second.should.be.json;
      second.body.should.have.property("code");
           
      let code = second.body.code;
      let req = "grant_type=authorization_code&redirect_uri=x&client_id="+app.appName+"&code="+code;      
      let third = await service.request()
      .post("/v1/token")
      .send(req);
      console.log(third.body);
      third.should.have.status(200);
      third.should.be.json;
      third.body.should.have.property("access_token");
      third.body.should.have.property("patient");

      user.token = third.body.access_token;	  
      user.owner = third.body.patient; 
    }
			    		    
}

exports.login = service.login;
exports.post = service.post;
exports.get = service.get;
exports.put = service.put;
exports.oauth2 = service.oauth2;
