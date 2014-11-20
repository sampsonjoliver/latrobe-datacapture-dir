/**
 * Created by Sam on 12/09/2014.
 */

/**
 * Creates a standard json response object indicating a successful response
 * and providing a data message as a response.
 * @param res The response object
 * @param data the data to include as message
 * @returns {*} a json body including status and data
 */
exports.Success = function (res, data){
    return res.json({status: 'success', data: data});
};

/**
 * Creates a standard json response object indicating a failed response
 * and providing a data message as a response.
 * @param res The response object
 * @param data the data to include as message
 * @returns {*} a json body including status and data
 */
exports.Failure = function (res, data){
    return res.json({status: 'fail', data: data});
};