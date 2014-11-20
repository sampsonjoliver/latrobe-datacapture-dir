var express = require('express');
var utils = require('../lib/utils');
var response = require('../lib/response');

var router = express.Router();

// Include the session model
var SessionModel = require('../models/session.js');

/**
 * Route to GET session listing in json.
 */
router.get('/session', function(req, res) {
    // Get a list of all sessions, retrieving only their metadata fields
    return SessionModel.findOnlyMetadata(function (err, sessions) {
        // Return any errors that occur to the client
        if (err)
            return response.Failure(res, err);
        // Return the result
        return res.send(sessions);
    });
});

/**
 * Route to GET user session listing in json.
 */
router.get('/user/:uid', function(req, res) {
    // Get a list of all sessions belonging to the user,
    // retrieving only their metadata fields
    return SessionModel.findOnlyMetadata(function(err, sessions) {
        // Return any errors that occur to the client
        if (err)
            return response.Failure(res, err);

        // Filter the result to remove duplicate session documents
        utils.FilterUniqueSessionID(sessions);
        // Sort the sessions by id
        sessions.sort();
        // Return the result
        return res.send(sessions);
    }, {user: req.params.uid});
});

/**
 * Route to GET session document in json.
 */
router.get('/session/:id', function(req, res) {
    // Get the complete document for a given session id,
    // sorted bytes date
    return SessionModel.find({sid: req.params.id})
        .sort({part: "asc"})
        .exec(function(err, sessions) {
            // Return any errors that occur to the client
            if (err)
                return response.Failure(res, err);

            // Return the result
            return res.send(sessions);
        });
});

/**
 * Route to GET a single part of a session series in json.
 */
router.get('/session/:id/:part', function(req, res) {
    // Get a single document out of a series of session documents
    return SessionModel.find({
        sid: req.params.id,
        part: req.params.part
    }, function(err, session) {
        // Return any errors that occur to the client
        if (err)
            return response.Failure(res, err);

        // Return the result
        return res.send(session);
    });
});

/**
 * Route to POST a single part of a session series in json.
 */
router.post('/session/:id', function(req, res) {
    // Parse the session data from the request body
    var session = new SessionModel({
        sid: req.params.id,
        user: req.body.user,
        event: req.body.event,
        datapoint: req.body.datapoint,
        datetime: req.body.datapoint[0].timestamp
        //datetime: req.body.datetime
    });


    // Add the part to the session series in-order
    session.saveInSeries(function(err) {
        // Return any errors that occur to the client
        if (err)
            return response.Failure(res, err);

        // Return a result indicating success
        return res.json({status: "success", message: "Session data saved"});
    });
});

/**
 * Route to DELETE a complete session series in json.
 */
router.delete('/session/:id', function(req, res) {
    // Remove all documents with matching sid from
    // the request params
    return SessionModel.remove({sid: req.params.id}, function (err) {
        // Return any errors that occur to the client
        if (err)
            return response.Failure(res, err);

        // Return a result indicating success
        return res.json({status: "success", message: "Session removed"});
    });
});

/**
 * Route to GET a json packet including data about the server in json.
 */
router.get('/proclaim/', function(req, res) {
    // Create the json packet of server vars
    var jsResponse = {
        serverName: "DIR Server 3000",
        version: 0.1,
        handles: ["physio"]
    };

    // Return the result
    return res.json(jsResponse);
});

// Export the module
module.exports = router;
