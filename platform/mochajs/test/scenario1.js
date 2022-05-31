var
  chai = require('chai'),
  chaiHttp = require('chai-http'),
  basic = require('../managers/basic');
  _data = require('../files/configurations'),
  observation = require('../models/observation');

chai.use(chaiHttp);
var expect = chai.expect;

describe('Scenario 1', function () {
  // use promises because done() function can only be called one time. And it is necessary to make two requests
  // one to authenticate in the api
  // an the other one to authenticate in the portal
  var user = _data.users[0];

  before('Authentication', async function () {	
	await basic.login(user);
	await basic.oauth2(_data.configs, user); 
  });

  describe('create and read an observation', function () {
    var objObservation = new observation();
    it('insert new observation', function (done) {
      basic.post('/fhir/Observation')
        .user(user)                  
        .send(objObservation)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.header.location).to.be.not.null;
        
          var startAt = res.header.location.indexOf('/fhir/Observation/') + '/fhir/Observation/'.length;
          var endedAt = res.header.location.indexOf('/_history');
          objObservation.id = res.header.location.substring(startAt, endedAt);
          done();       
      });
    });

    it('read inserted observation', function (done) {
      basic.get('/fhir/Observation/' + objObservation.id)
           .user(user)
        //.query({date: objObservation.effectiveDateTime})        
        .end(function (err, res) {
          var response_observation = res.body;//.entry[0].resource;
          expect(err).to.be.null;
          expect(res).to.have.status(200);
          expect(response_observation.code.coding[0].code).to.equal(objObservation.code.coding[0].code);
          expect(response_observation.status).to.equal(objObservation.status);
          expect(response_observation.valueQuantity.value).to.equal(objObservation.valueQuantity.value);
          expect(response_observation.meta.versionId).to.be.not.null;
          done();
        });
    });
  });


  describe('update observation', function () {
    var objObservation = new observation();
    var id_observation,
      updated_observation,
      ETag;
    it('insert new observation', function (done) {
      basic
        .post('/fhir/Observation')
        .user(user)        
        .send(objObservation)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);
          var startAt = res.header.location.indexOf('/fhir/Observation/') + '/fhir/Observation/'.length;
          var endedAt = res.header.location.indexOf('/_history');
          id_observation = res.header.location.substring(startAt, endedAt);
          ETag = res.header.etag;
          done();
        });
    });

    it('get inserted observation', function (done) {
      basic.get('/fhir/Observation/' + id_observation)
        .user(user)        
        .end(function (err, res) {
          var versionId = res.body.meta.versionId;
          updated_observation = new observation({
            id: id_observation,
            versionId: versionId
          });
          done();
        });
    });

    it('update inserted observation', function (done) {
      basic.put('/fhir/Observation/' + id_observation)
        .user(user)
        .set({        
          'If-Match': ETag
        })
        .send(updated_observation)
        .end(function (err, res) {
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);
          expect(err).to.be.null;
          done();
        });
    });

    it('get updated observation', function (done) {
      basic.get('/fhir/Observation/' + id_observation)
        .user(user)        
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res).to.have.status(200);
          expect(res.body.code.coding[0].code).to.equal(updated_observation.code.coding[0].code);
          expect(res.body.status).to.equal(updated_observation.status);
          expect(res.body.valueQuantity.value).to.equal(updated_observation.valueQuantity.value);
          expect(res.body.meta.versionId).to.be.not.null;
          done();
        });
    });
  });


  describe('remove observation', function () {
    var objObservation = new observation();
    objObservation.v_value = 100;
    var id_observation;
    it('insert new observation', function (done) {
      basic.post('/fhir/Observation')
        .user(user)
        .send(objObservation)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);
          var startAt = res.header.location.indexOf('/fhir/Observation/') + '/fhir/Observation/'.length;
          var endedAt = res.header.location.indexOf('/_history');
          id_observation = res.header.location.substring(startAt, endedAt);
          done();
        });
    });

    it('delete inserted observation', function (done) {
      basic.post(_data.configs.url_records_delete)      
        .portal(user)
        .send({
          _id: id_observation + '.' + user.owner
        })
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);
          done();
        });
    });

    it('should not get removed observation', function (done) {
      basic.get('/fhir/Observation/' + id_observation)
        .user(user)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(399);
          expect(res.statusCode).to.have.lessThan(500);
          done();
        });
    });

    it('should get history of removed observation', function (done) {
      basic.get('/fhir/Observation/' + id_observation + '/_history/0')
        .user(user)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);
          done();
        });
    });
  });
});