var
  chai = require('chai'),
  chaiHttp = require('chai-http'),
  basic = require('../managers/basic');
  _data = require('../files/configurations'),
  observation = require('../models/observation');

chai.use(chaiHttp);
var expect = chai.expect;


describe.skip('Scenario 4', function () {
  // researcher wants to share study with researcher 2
  var researcher1 = _data.researchers[0];
  var researcher2 = _data.researchers[1];

  // researcher dont have permission to see the study
  var researcher3 = _data.researchers[2];

  // user 1 and user 2
  var user1 = _data.users[0];
  var user2 = _data.users[1];

  // user3 don't have permission to see the shared data
  var user3 = _data.users[2];

  before('Authentication', async function () {
	await basic.login(researcher1);
	await basic.oauth2(_data.configs, researcher1); 
	await basic.login(researcher2);
	await basic.oauth2(_data.configs, researcher2);
	await basic.login(researcher3);
	await basic.oauth2(_data.configs, researcher3);	
	
    await basic.login(user1);
	await basic.oauth2(_data.configs, user1); 
	await basic.login(user2);
	await basic.oauth2(_data.configs, user2);
	await basic.login(user3);
	await basic.oauth2(_data.configs, user3);	 
  });

  describe('Records in study', function () {
    var study;
    it('Precondition: validated study exist', function (done) {
      basic.get('/api/research/studies/' + _data.configs.study_accepted_id)
        .portal(researcher1)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);
          expect(res.text).to.be.not.null;
          study = JSON.parse(res.text);

          expect(study.executionStatus).to.equal("RUNNING");
          expect(study.validationStatus).to.equal("VALIDATED");

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

    it('get information from researcher4', function (done) {
      basic.get('/api/shared/users/current')
        .portal(researcher4)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);
          expect(res.body).to.be.not.null;
          researcher4.id = res.body.user;
          researcher4.org = res.body.org;

          done();
        });
    });

    it('Precondition: researchers 1, 2 and 3 are in the same organization but not r4', function (done) {
      expect(researcher1.org).to.equal(researcher2.org);
      expect(researcher1.org).to.equal(researcher3.org);
      expect(researcher1.org).to.not.equal(researcher4.org);
      done();
    });
    
    it('Precondition: researchers 1 and 2 are in the study but not 3 and 4', function (done) {
      done();
    });
    
    it('Precondition: user 1 and 2 are in the study', function (done) {
      done();
    });
    
    it('Precondition: user 3 are not in the study', function (done) {
      done();
    });
  });
});