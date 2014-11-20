var express = require('express');
var router = express.Router();

/**
 * Route to GET the index page as html.
 */
router.get('/', function(req, res) {
  res.render('index', { title: 'Dashboard' });
});

/**
 * Route to GET a session page as html.
 */
router.get('/session/:uid/:id', function(req, res) {
    res.render('session', { title: 'Session', sessionid: req.params.id, userid: req.params.uid });
});

module.exports = router;
