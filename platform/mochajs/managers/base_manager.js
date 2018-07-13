var request_promise = require('request-promise'),
  _data = require('../files/configurations');

/**
 * This function authenticate a user
 * @param {object} user User to authenticate
 * @param {function} resAPI The response to the API authentication.
 * @param {function} resPortal The response to the Portal authentication
 */
exports.authenticate = function (user, resAPI, resPortal) {
  return request_promise.post({
        uri: _data.configs.environment + _data.configs.url_auth_api,
        headers: {
          'Content-Type': 'application/json'
        },
        json: {
          appname: _data.configs.appName,
          device: _data.configs.device,
          secret: _data.configs.secret,
          username: user.email,
          password: user.password,
          role: user.role
        }
      },
      function (error, response, body) {
        resAPI(error, response, body); // body = { authToken, refreshToken, status, owner }
      }
    ).on('error', function (err) {
      // throw an exception and break the process if there is an error
      throw err;
    })
    .then(function () {
      var _uri = _data.configs.environment;
      if (user.role == "MEMBER") {
        _uri += _data.configs.url_auth_api_portal_member;
      } else if (user.role == "RESEARCH") {
        _uri += _data.configs.url_auth_api_portal_research;
      }

      return request_promise.post({
          uri: _uri,
          headers: {
            'Content-Type': 'application/json'
          },
          json: {
            email: user.email,
            password: user.password
          }
        },
        function (error, response, body) {
          resPortal(error, response, body); // body { keyType, sessionToken, lastLogin, role, subroles }
        }).on('error', function (err) {
        throw err;
      });
    });
};

exports.confirm_consents = function (sessionToken) {
  var requested_consents;
  return request_promise.post({
        uri: _data.configs.environment + _data.configs.url_consents,
        headers: {
          'Content-Type': 'application/json',
          'X-Session-Token': sessionToken
        },
        json: {
          properties: {
            type: "EXTERNALSERVICE"
          },
          fields: ["name", "authorized", "type", "status"]
        }
      },
      function (error, response, body) {
        // body is an array [] with objects { _id, name, authorized, type, status }
        requested_consents = body;
      }
    ).on('error', function (err) {
      throw err;
    })
    .then(function () {
      var _promises = [];
      requested_consents.forEach(consent => {
        if (consent.status == "UNCONFIRMED" && consent.name.indexOf(_data.configs.appName) != -1) {
          _promises.push(request_promise.post({
            uri: _data.configs.environment + _data.configs.url_consents_confirm,
            headers: {
              'Content-Type': 'application/json',
              'X-Session-Token': sessionToken
            },
            json: {
              consent: consent._id
            }
          }).on('error', function (error) {
            throw error;
          }));
        }
      });

      return Promise.all(_promises);
    });
};