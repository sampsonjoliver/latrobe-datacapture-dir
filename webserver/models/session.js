/**
 * Created by Sam on 10/09/2014.
 */
var mongoose = require('mongoose');

var Schema = mongoose.Schema;

// Define the schema for Critical Events
var EventSchema = new Schema({
    dataField: String,
    label: String,
    constraintType: String,
    constraintValue: Number,
    severity: Number
}, {_id: false});

// Define the schema for timeseries data
var SeriesDataSchema = new Schema({
    timestamp: Date,
    calcDiffOrientationX: Number,
    calcDiffOrientationY: Number,
    calcDiffOrientationZ: Number,
    calcDiffOrientationW: Number,
    calcOrientationDistance: Number,
    calcFlexionAngle: Number,
    calcVarusAngle: Number,
    calcLateralAngle: Number,
    masterRotDataX: Number,
    masterRotDataY: Number,
    masterRotDataZ: Number,
    masterRotDataW: Number,
    slaveRotDataX: Number,
    slaveRotDataY: Number,
    slaveRotDataZ: Number,
    slaveRotDataW: Number,
    masterAccDataX: Number,
    masterAccDataY: Number,
    masterAccDataZ: Number,
    slaveAccDataX: Number,
    slaveAccDataY: Number,
    slaveAccDataZ: Number,
    masterGyroDataX: Number,
    masterGyroDataY: Number,
    masterGyroDataZ: Number,
    slaveGyroDataX: Number,
    slaveGyroDataY: Number,
    slaveGyroDataZ: Number,
    masterMagDataX: Number,
    masterMagDataY: Number,
    masterMagDataZ: Number,
    slaveMagDataX: Number,
    slaveMagDataY: Number,
    slaveMagDataZ: Number
}, {_id: false});

// Define the session schema
var sessionSchema = new Schema({
    sid: String,
    part: Number,
    user: String,
    datetime: Date,
    event: [EventSchema],
    datapoint: [SeriesDataSchema]
}, { collection: 'session' });

/**
 * Find all the sessions with a specified id and criteria matching
 * the provided data
 * @param callback callback to execute on success
 * @param data the search data required
 * @returns {Promise|Array|{index: number, input: string}|*}
 */
sessionSchema.statics.findOnlyMetadata = function(callback, data) {
    // Find all session documents
    if (data == undefined)
        // Get all the sessions
        return this.aggregate()
            .group({_id : '$sid', user : {$first: '$user'},datetime : {$min: '$datetime'}})
            .project({ _id: 0, user: 1, datetime: 1, sid: "$_id" })
            .exec(callback);
    else
        // Get all the session matching the data criteria
        return this.aggregate()
            .match(data)
            .group({_id : '$sid', user : {$first: '$user'},datetime : {$min: '$datetime'}})
            .project({ _id: 0, user: 1, datetime: 1, sid: "$_id" })
            .exec(callback);
};

/**
 * Find all the sessions in the current session instance's series
 * @param callback callback to execute on success
 * @returns {Query}
 */
sessionSchema.methods.findAllInSeries = function(callback) {
    // Find the session document
    return this.model('session')
        .find({sid: this.sid}, callback)
};

/**
 * Find all the sessions in the current session instance's series,
 * returning only the metadata.
 * @param callback callback to execute on success
 * @returns {Query}
 */
sessionSchema.methods.findOnlyMetadataAllInSeries = function(callback) {
    // Find the session documents belonging in the current series
    return this.model('session')
        .find({sid: this.sid}, {'_id' : 0})
        .select('sid user datetime')
        .exec(callback);
};

/**
 * Find all the sessions belonging to the current session's user
 * @param callback callback to execute on success
 * @returns {Query}
 */
sessionSchema.methods.findUserSessionMetadata = function(callback) {
    // Find the session document belonging to the current user
    return this.model('session')
        .find({user: this.user}, {'_id' : 0})
        .select('sid user datetime')
        .exec(callback);
};

/**
 * Count the number of documents in the session series,
 * and add this document in order
 * @param callback callback to execute on success
 * @returns {Query}
 */
sessionSchema.methods.saveInSeries = function(callback) {
    var doc = this;
    // Count the number of documents in the series
    this.model('session').count({sid: this.sid}, function(err, count) {
        // Return any errors that occur
        if (err)
            return handleError(err);
        // Add the part number and return the document
        doc.part = count;
        return doc.save(callback);
    });
};

module.exports = mongoose.model('session', sessionSchema);