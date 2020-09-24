'use strict';

const seclib = require('/envision/security-lib.xqy');

const users = seclib.getUsers();

users.map(u => ({username: u, stagingDocs: 0}))
