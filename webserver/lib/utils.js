/**
 * Created by Sam on 11/09/2014.
 */

/**
 * Filter an array of data and remove all duplicate data items,
 * using the id as comparator.
 * @param a the array of data
 * @param b placeholder var
 * @param c placeholder var
 * @returns {*} an array with duplicate items removed
 */
exports.FilterUniqueSessionID = function (a,b,c){
    if (a.length > 0) {
        b = a.length;
        while (c = --b)
            while (c--)
                if (b < a.length && c < a.length)
                    a[b].sid !== a[c].sid || a.splice(c, 1);
        return a;
    }
};

/**
 * Filter an array of data and remove all duplicate data items,
 * using a comparator to identify duplicates.
 * @param comparator the comparator to identify duplicates
 * @param a the array of data
 * @param b placeholder var
 * @param c placeholder var
 * @returns {*} an array with duplicate items removed
 */
exports.FilterArrayByDistinct = function (comparator, a,b,c){
    if (a.length > 0) {
        b = a.length;
        while (c = --b)
            while (c--)
                comparator(a[b], a[c]) || a.splice(c, 1);
        return a;
    }
};