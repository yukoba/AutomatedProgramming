function clone(obj) {
    // 手抜き
    return JSON.parse(JSON.stringify(obj));
}

function pushAll(ary1, ary2) {
    for (var i = 0; i < ary2.length; i++) {
        ary1.push(ary2[i]);
    }
}

/** key -> value の配列、というハッシュテーブルに key, value を追加 */
function addToArrayHashTable(hashTable, key, value) {
    if (!(key in hashTable)) hashTable[key] = [];
    hashTable[key].push(value);
}

function uniqueArray(ary) {
    if (!(ary instanceof Array)) throw new TypeError(); // Non-null Array

    var hash = {}, result = [];
    for (var i = 0; i < ary.length; i++) {
        var v = ary[i];
        if (!(v in hash)) {
            hash[v] = true;
            result.push(v);
        }
    }
    return result;
}

