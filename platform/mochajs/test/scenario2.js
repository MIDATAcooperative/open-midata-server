var
  chai = require('chai'),
  chaiHttp = require('chai-http'),
  basic = require('../managers/basic');
  _data = require('../files/configurations'),
  observation = require('../models/observation');

chai.use(chaiHttp);
var expect = chai.expect;


describe('Scenario 2', function () {
  // user1 wants to share data with user 2
  var user1 = _data.users[0];
  var user2 = _data.users[1];

  // user3 dont have permission to see the shared data
  var user3 = _data.users[2];

  before('Auth User 1', async function () {
	await basic.login(user1);
	await basic.oauth2(_data.configs, user1); 		   
  });

  before('Auth User 2', async function () {	
	await basic.login(user2);
	await basic.oauth2(_data.configs, user2);		   
  });

  before('Auth User 3', async function () {	
	await basic.login(user3);
	await basic.oauth2(_data.configs, user3);	    
  });

  describe('share data', function () {
    // to be added in the circle
    var objObservation1 = new observation();
    var objObservation2 = new observation();

    // inserts only in user1 but not in the circle
    var objObservation3 = new observation();
    var user_to_share_data,
        circle_shared;

    it('insert new observation 1', function (done) {
      basic.post('/fhir/Observation')
        .user(user1)        
        .send(objObservation1)
        .end(function (err, res) {
	       console.log(res);
           console.log(res.header);
           console.log(res.body);
          expect(err).to.be.null;
          expect(res.header.location).to.be.not.null;
          var startAt = res.header.location.indexOf('/fhir/Observation/') + '/fhir/Observation/'.length;
          var endedAt = res.header.location.indexOf('/_history');
          objObservation1.id = res.header.location.substring(startAt, endedAt);
          done();
        });
    });

    it('insert new observation 2', function (done) {
      basic.post('/fhir/Observation')
        .user(user1)        
        .send(objObservation2)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.header.location).to.be.not.null;
          var startAt = res.header.location.indexOf('/fhir/Observation/') + '/fhir/Observation/'.length;
          var endedAt = res.header.location.indexOf('/_history');
          objObservation2.id = res.header.location.substring(startAt, endedAt);
          done();
        });
    });
    
    it('insert new observation 3', function (done) {
       basic.post('/fhir/Observation')
        .user(user1)        
        .send(objObservation3)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.header.location).to.be.not.null;
          var startAt = res.header.location.indexOf('/fhir/Observation/') + '/fhir/Observation/'.length;
          var endedAt = res.header.location.indexOf('/_history');
          objObservation3.id = res.header.location.substring(startAt, endedAt);
          done();
        });
    });

    it('search user 2 to share data', function (done) {
      basic.get('/api/members/users/search/' + encodeURIComponent(user2.email))
        .portal(user1)        
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);
          expect(res.text).to.be.not.null;
          user_to_share_data = JSON.parse(res.text);
          done();
        });
    });

    it('share data with user 2', function (done) {

      var circle_to_share = 
      {
        type:"CIRCLE",
        status:"ACTIVE",
        authorized: [user_to_share_data[0]._id],
        writes: "NONE",
        owner: user1.id,
        name: "test share information with user2 - " + new Date().toISOString()
      };

      basic.post('/api/members/circles')
        .portal(user1)        
        .send(circle_to_share)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);
          expect(res.body).to.be.not.null;
          circle_shared = res.body;
          done();
        });
    });

    it('insert records to circle', function (done) {
      var share_data_to_circle = 
      {
        records: [objObservation1.id, objObservation2.id],
        started: [circle_shared._id],
        stopped:[],
        type:"circles",
        query:null
      };

      basic.post('/api/members/records/shared')
        .portal(user1)
        .send(share_data_to_circle)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);

          done();
        });
    });

    it('user2 read observations from circle', function (done) {
      basic.get('/api/members/records/shared/' + circle_shared._id)
        .portal(user2)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);

          expect(res.body.records.includes(objObservation1.id)).to.be.true;
          expect(res.body.records.includes(objObservation2.id)).to.be.true;
          expect(res.body.records.includes(objObservation3.id)).to.be.false;

          done();
        });
    });

    it.skip('user3 can not read observations from circle', function (done) {
      basic.get('/api/members/records/shared/' + circle_shared._id)
        .portal(user3)
        .end(function (err, res) {
          expect(err).to.null;
          // at the moment returns http-500 and thats ok! (in the future maybe 400)
          expect(res.statusCode).to.have.greaterThan(399);
          expect(res.statusCode).to.have.lessThan(600);

          done();
        });
    });
  });
});