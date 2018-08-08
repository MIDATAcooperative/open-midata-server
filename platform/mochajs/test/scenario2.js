var
  chai = require('chai'),
  chaiHttp = require('chai-http'),
  _data = require('../files/configurations'),
  observation = require('../models/observation'),
  base_manager = require('../managers/base_manager');

chai.use(chaiHttp);
var expect = chai.expect;

describe('Scenario 2', function () {
  // user1 wants to share data with user 2
  var user1 = _data.users[0];
  var user2 = _data.users[1];

  // user3 dont have permission to see the shared data
  var user3 = _data.users[2];

  before('Authentication', function () {
    return base_manager.authenticate(user1, function (err, res, body) {
          user1.authAPI = body;
        },
        function (err, res, body) {
          user1.authPortal = body;
        })
      .then(function () {
          if (user1.authAPI.status == "UNCONFIRMED") {
          return base_manager.confirm_consents(user1.authPortal.sessionToken);
        }
      })
      .then(function(){
          return base_manager.authenticate(user2, function (err, res, body) {
          user2.authAPI = body;
            },
            function (err, res, body) {
          user2.authPortal = body;
            })
          .then(function () {
          if (user2.authAPI.status == "UNCONFIRMED") {
          return base_manager.confirm_consents(user2.authPortal.sessionToken);
            }
          })
      }).then(function(){
          return base_manager.authenticate(user3, function (err, res, body) {
          user3.authAPI = body;
            },
            function (err, res, body) {
          user3.authPortal = body;
            })
          .then(function () {
          if (user3.authAPI.status == "UNCONFIRMED") {
          return base_manager.confirm_consents(user3.authPortal.sessionToken);
            }
          });
      });
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
      chai
        .request(_data.configs.environment)
        .post('/fhir/Observation')
        .set('Authorization', 'Bearer ' + user1.authAPI.authToken)
        .send(objObservation1)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.header.location).to.be.not.null;
          var startAt = res.header.location.indexOf('/fhir/Observation/') + '/fhir/Observation/'.length;
          var endedAt = res.header.location.indexOf('/_history');
          objObservation1.id = res.header.location.substring(startAt, endedAt);
          done();
        });
    });

    it('insert new observation 2', function (done) {
      chai
        .request(_data.configs.environment)
        .post('/fhir/Observation')
        .set('Authorization', 'Bearer ' + user1.authAPI.authToken)
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
      chai
        .request(_data.configs.environment)
        .post('/fhir/Observation')
        .set('Authorization', 'Bearer ' + user1.authAPI.authToken)
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
      chai
        .request(_data.configs.environment)
        .get('/api/members/users/search/' + encodeURIComponent(user2.email))
        .set('X-Session-Token', user1.authPortal.sessionToken)
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

      chai
        .request(_data.configs.environment)
        .post('/api/members/circles')
        .set('X-Session-Token', user1.authPortal.sessionToken)
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

      chai
        .request(_data.configs.environment)
        .post('/api/members/records/shared')
        .set('X-Session-Token', user1.authPortal.sessionToken)
        .send(share_data_to_circle)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);

          done();
        });
    });

    it('user2 read observations from circle', function (done) {
      chai
        .request(_data.configs.environment)
        .get('/api/members/records/shared/' + circle_shared._id)
        .set('X-Session-Token', user2.authPortal.sessionToken)
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
      chai
        .request(_data.configs.environment)
        .get('/api/members/records/shared/' + circle_shared._id)
        .set('X-Session-Token', user3.authPortal.sessionToken)
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