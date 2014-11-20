/**
 * Created by Sam on 10/09/2014.
 */

var path = require('path')
    , rootPath = path.normalize(__dirname + '/..')
    , templatePath = path.normalize(__dirname + '/../app/mailer/templates')
    , notifier = {
        service: 'postmark',
        APN: false,
        email: false, // true
        actions: ['comment'],
        tplPath: templatePath,
        key: 'POSTMARK_KEY',
        parseAppId: 'PARSE_APP_ID',
        parseApiKey: 'PARSE_MASTER_KEY'
    };

// Export the project config vars
module.exports = {
    development: {
        username: 'user',
        password: 'pass',
        db: 'mongodb://user:pass@dbpath/db',
        root: rootPath,
        notifier: notifier,
        app: {
            name: 'Nodejs Express Mongoose Demo'
        }
    },
    test: {
        username: 'user',
        password: 'pass',
        db: 'mongodb://user:pass@dbpath/db',
        root: rootPath,
        notifier: notifier,
        app: {
            name: 'Nodejs Express Mongoose Demo'
        }
    },
    production: {}
};