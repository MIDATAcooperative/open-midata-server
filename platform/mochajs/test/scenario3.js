var
  chai = require('chai'),
  chaiHttp = require('chai-http'),
  basic = require('../managers/basic');
  _data = require('../files/configurations'),
  observation = require('../models/observation');

chai.use(chaiHttp);
var expect = chai.expect;


describe('Scenario 3', function () {
  // researcher wants to share study with researcher 2
  var researcher1 = _data.researchers[0];
  var researcher2 = _data.researchers[1];

  // researcher dont have permission to see the study
  var researcher3 = _data.researchers[2];

  before('Auth Researcher 1', async function () {
	await basic.login(researcher1);
	await basic.oauth2(_data.configs, researcher1); 	
  });

  before('Auth Researcher 2', async function () {	
	await basic.login(researcher2);
	await basic.oauth2(_data.configs, researcher2);	
  });

  before('Auth Researcher 3', async function () {	
	await basic.login(researcher3);
	await basic.oauth2(_data.configs, researcher3);	
  });

  describe('create and remove study', function () {
    var study = {name:"test study " + new Date().toISOString(),description:"test study"};
    it('create study', function (done) {
      basic.post('/api/research/studies')
        .portal(researcher1)
        .send(study)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);
          expect(res.text).to.be.not.null;
          let objResponse = JSON.parse(res.text);
          study.id = objResponse._id;
          study.code = objResponse.code;
          study.owner = objResponse.owner;
          study.createdBy = objResponse.createdBy;

          done();
        });
    });

    it('get information from researcher1', function (done) {
      basic.get('/api/shared/users/current')
        .portal(researcher1)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);
          expect(res.body).to.be.not.null;
          researcher1.id = res.body.user;
          researcher1.org = res.body.org;
          
          done();
        });
    });

    it('get information from researcher2', function (done) {
      basic.get('/api/shared/users/current')
        .portal(researcher2)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);
          expect(res.body).to.be.not.null;
          researcher2.id = res.body.user;
          researcher2.org = res.body.org;
          
          done();
        });
    });

    it('get information from researcher3', function (done) {
      basic.get('/api/shared/users/current')
        .portal(researcher3)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);
          expect(res.body).to.be.not.null;
          researcher3.id = res.body.user;
          researcher3.org = res.body.org;
          
          done();
        });
    });

    it('add researcher', function (done) {
      var add_forscher_information = 
      {
        group:study.id,
        members:[researcher2.id],
        readData:true,
        writeData:false,
        pseudo:true,
        changeTeam:true,
        export:true,
        auditLog:true,
        participants:true,
        setup:true,
        roleName:"Sponsor",
        id:"SPONSOR",
        unpseudo:false
      };

      basic.post('/api/shared/usergroups/adduser')
        .portal(researcher1)
        .send(add_forscher_information)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);
          done();
        });
    });

    it('get researchers in study', function (done) {
      var group_information = { usergroup: study.id };

      basic.post('/api/shared/usergroups/members')
        .portal(researcher1)
        .send(group_information)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);
          expect(res.text).to.be.not.null;
          let objResponse = JSON.parse(res.text);

          var containsR1 = false, 
            containsR2 = false,
            containsR3 = false;

          for (let i = 0; i < objResponse.length; i++) {
            const element = objResponse[i];
            if (element.member == researcher1.id) {
              containsR1 = true;
            } else if (element.member == researcher2.id) {
              containsR2 = true;
            } else if (element.member == researcher3.id) {
              containsR3 = true;
            } 
          }

          expect(containsR1).to.be.true;
          expect(containsR2).to.be.true;
          expect(containsR3).to.be.false;

          done();
        });
    });
    
    it('remove study', function (done) {
      basic.post('/api/research/studies/' + study.id + '/status/delete')
        .portal(researcher1)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);

          done();
        });
    });
  });
});