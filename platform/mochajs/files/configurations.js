var configs = {
    "configs": {
      "environment": "https://midata-frontend",
      "appName": "MidataTestRecords",
      "secret": "123456abcABC",
      "device": "1MochaTest",
      "url_auth_api": "/v1/auth",
      "url_auth_api_portal_member":"/api/members/login",
      "url_auth_api_portal_research":"/api/research/login",
      "url_consents":"/api/members/consents",
      "url_consents_confirm":"/api/members/consents/confirm",
      "url_records_delete": "/api/members/records/delete",
      "study_accepted_id": "5b16792179c7213e4ea991fa"
    },
    "users": [
      {
        "email": "midata.test.user1@chavez-arias.ch",
        "password": "123456abcABC!",
        "role": "MEMBER"
      },
      {
        "email": "midata.test.user2@chavez-arias.ch",
        "password": "123456abcABC!",
        "role": "MEMBER"
      },
      {
        "email": "midata.test.user3@chavez-arias.ch",
        "password": "123456abcABC!",
        "role": "MEMBER"
      }
    ],
    "researchers": [
      {
        "email": "midata.test.research1@chavez-arias.ch",
        "password": "123456abcABC!",
        "organizationName": "BFH MIDATA Test",
        "role": "RESEARCH"
      },
      {
        "email": "midata.test.research2@chavez-arias.ch",
        "password": "123456abcABC!",
        "organizationName": "BFH MIDATA Test",
        "role": "RESEARCH"
      },
      {
        "email": "midata.test.research3@chavez-arias.ch",
        "password": "123456abcABC!",
        "organizationName": "BFH MIDATA Test",
        "role": "RESEARCH"
      },
      {
        "email": "midata.test.research4@chavez-arias.ch",
        "password": "123456abcABC!",
        "organizationName": "BFH MIDATA Test 2",
        "role": "RESEARCH"
      }
    ]
  };

module.exports = configs;