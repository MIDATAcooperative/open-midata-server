var chai = require('chai'),
  chaiHttp = require('chai-http'),
  _data = require('../files/configurations'),
  base_manager = require('../managers/base_manager');

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
          });
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
      }).then(function () {
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
          });
      }).then(function () {
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
      }).then(function () {
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
      });;
  });

  describe('Records in study', function () {
    var study;
    it('Precondition: validated study exist', function (done) {
      chai
        .request(_data.configs.environment)
        .get('/api/research/studies/' + _data.configs.study_accepted_id)
        .set('X-Session-Token', researcher1.authPortal.sessionToken)
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
      chai
        .request(_data.configs.environment)
        .get('/api/shared/users/current')
        .set('X-Session-Token', researcher1.authPortal.sessionToken)
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
      chai
        .request(_data.configs.environment)
        .get('/api/shared/users/current')
        .set('X-Session-Token', researcher2.authPortal.sessionToken)
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
      chai
        .request(_data.configs.environment)
        .get('/api/shared/users/current')
        .set('X-Session-Token', researcher3.authPortal.sessionToken)
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
      chai
        .request(_data.configs.environment)
        .get('/api/shared/users/current')
        .set('X-Session-Token', researcher4.authPortal.sessionToken)
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