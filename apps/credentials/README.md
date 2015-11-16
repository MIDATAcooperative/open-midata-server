credentials
===========

MIDATA app for storing username/password combinations, encrypted with a master passphrase.

The app uses [crypto-js](https://code.google.com/p/crypto-js/) to encrypt a triple of (site, username, password) with the [AES Cipher](https://code.google.com/p/crypto-js/#AES). The passphrase is hashed with [SHA-512](https://code.google.com/p/crypto-js/#SHA-2) so that it can be checked against in the visualization.