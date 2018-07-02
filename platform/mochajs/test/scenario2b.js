var chai = require('chai'),
  chaiHttp = require('chai-http'),
  _data = require('../files/configurations'),
  observation = require('../models/observation'),
  base_manager = require('../managers/base_manager');

chai.use(chaiHttp);
var expect = chai.expect;

describe.skip('Scenario 2 with researcher accounts', function () {
  // researcher wants to share study with researcher 2
  var researcher1 = _data.researchers[0];
  var researcher2 = _data.researchers[1];

  // researcher dont have permission to see the study
  var researcher3 = _data.researchers[2];

  before('Authentication', function () {
    return base_manager.authenticate(researcher1, function (err, res, body) {
          researcher1.authAPI = body;
        },
        function (err, res, body) {
          researcher1.authPortal = body;
        })
      .then(function () {
        if (researcher1.authAPI.status == "UNCONFIRMED") {
          return base_manager.confirm_consents(researcher1.authPortal.sessionToken);
        }
      })
      .then(function () {
        return base_manager.authenticate(researcher2, function (err, res, body) {
          researcher2.authAPI = body;
            },
            function (err, res, body) {
              researcher2.authPortal = body;
            })
          .then(function () {
            if (researcher2.authAPI.status == "UNCONFIRMED") {
              return base_manager.confirm_consents(researcher2.authPortal.sessionToken);
            }
          })
      }).then(function () {
        return base_manager.authenticate(researcher3, function (err, res, body) {
          researcher3.authAPI = body;
            },
            function (err, res, body) {
              researcher3.authPortal = body;
            })
          .then(function () {
            if (researcher3.authAPI.status == "UNCONFIRMED") {
              return base_manager.confirm_consents(researcher3.authPortal.sessionToken);
            }
          });
      });
  });

  describe('share data', function () {
    // to be added in the circle
    var objObservation1 = new observation();
    var objObservation2 = new observation();

    // inserts only in researcher1 but not in the circle
    var objObservation3 = new observation();
    var user_to_share_data,
      circle_shared;

    it('insert new observation 1', function (done) {
      chai
        .request(_data.configs.environment)
        .post('/fhir/Observation')
        .set('Authorization', 'Bearer ' + researcher1.authAPI.authToken)
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
        .set('Authorization', 'Bearer ' + researcher1.authAPI.authToken)
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
        .set('Authorization', 'Bearer ' + researcher1.authAPI.authToken)
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

    it('search researcher 2 to share data', function (done) {
      chai
        .request(_data.configs.environment)
        .get('/api/members/users/search/' + encodeURIComponent(researcher2.email))
        .set('X-Session-Token', researcher1.authPortal.sessionToken)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);
          expect(res.text).to.be.not.null;
          user_to_share_data = JSON.parse(res.text);
          done();
        });
    });

    it('share data with researcher 2', function (done) {

      var circle_to_share = {
        type: "CIRCLE",
        status: "ACTIVE",
        authorized: [user_to_share_data[0]._id],
        writes: "NONE",
        owner: researcher1.id,
        name: "test share information with researcher2 - " + new Date().toISOString()
      };

      chai
        .request(_data.configs.environment)
        .post('/api/members/circles')
        .set('X-Session-Token', researcher1.authPortal.sessionToken)
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
      var share_data_to_circle = {
        records: [objObservation1.id, objObservation2.id],
        started: [circle_shared._id],
        stopped: [],
        type: "circles",
        query: null
      };

      chai
        .request(_data.configs.environment)
        .post('/api/members/records/shared')
        .set('X-Session-Token', researcher1.authPortal.sessionToken)
        .send(share_data_to_circle)
        .end(function (err, res) {
          expect(err).to.be.null;
          expect(res.statusCode).to.have.greaterThan(199);
          expect(res.statusCode).to.have.lessThan(300);

          done();
        });
    });

    it('researcher2 read observations from circle', function (done) {
      chai
        .request(_data.configs.environment)
        .get('/api/members/records/shared/' + circle_shared._id)
        .set('X-Session-Token', researcher2.authPortal.sessionToken)
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

    it('researcher3 can not read observations from circle', function (done) {
      chai
        .request(_data.configs.environment)
        .get('/api/members/records/shared/' + circle_shared._id)
        .set('X-Session-Token', researcher3.authPortal.sessionToken)
        .end(function (err, res) {
          expect(err).to.null;
          expect(res.statusCode).to.have.greaterThan(399);
          expect(res.statusCode).to.have.lessThan(500);

          done();
        });
    });
  });
});