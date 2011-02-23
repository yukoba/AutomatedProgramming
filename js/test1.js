// 言葉の定義として、命題論理 (propositional logic)・述語論理 (predicate logic) を logic とよぶ
(function() {
    window.onload = solve;

    /** 解くべき問題 */
    var problem = [["not", "P"], "or", "R"];

    /** 事実 */
    var facts = [
            [["not", "P"], "or", "Q"],
            [["not", "Q"], "or", "R"]
    ];

    function solve() {
        var logics = clone(facts);

        // 背理法。問題の否定を追加
        addLogic(logics, negate(problem));
        println("logics: ", logics);

        //while (true) {
        for (var i = 0; i < 10; i++) { // 暴走防止のため１０回で終了
            // 矛盾を探す
            if (isContradicted(logics)) {
                println("\nFound contradiction. Done!");
                break;
            }

            // 融合 (resolve) のためのハッシュテーブルを作る
            var resolveHashs = createResolveHashs(logics);
            //println(resolveHashs);

            // 融合を行い、新しい命題を作る
            resolveAll(logics, resolveHashs);
       }
    }

    /** 矛盾を探す */
    function isContradicted(logics) {
        println("\n-----------------[isContradicted]------------\n", "logics: ", logics);

        // 高速化のためハッシュを作る
        var logicsHash = {};
        for (var i = 0; i < logics.length; i++) {
            var logic = logics[i];
            logicsHash[sortLogic(logic)] = true;
        }

        // 各論理の否定を作り logics に含まれているか調べる
        for (i = 0; i < logics.length; i++) {
            logic = logics[i];

            var negated = negate(logic);
            println("\tnegate: ", logic, " -> ", negated);

            if (negated in logicsHash) {
                return true;
            }
        }
        return false;
    }

    function sortLogic(logic) {
        // TODO 未実装
        // TODO "P or Q" と "Q or P" を同一視するため、logic をソートするなどの作業
        return logic;
    }

    /** 融合 */
    function resolveAll(logics, resolveHashs) {
        println("\n-----------------[resolveAll]------------");
        var resolved = [];
        for (var key in resolveHashs.nonNegateHash) {
            if (key in resolveHashs.negateHash) {
                for (var i = 0; i < resolveHashs.nonNegateHash[key].length; i++) {
                    var idx1 = resolveHashs.nonNegateHash[key][i];
                    for (var j = 0; j < resolveHashs.negateHash[key].length; j++) {
                        var idx2 = resolveHashs.negateHash[key][j];
                        resolved.push(resolve(logics, idx1, idx2, key));
                    }
                }
            }
        }
        //println(resolved);
        pushAll(logics, resolved);
    }

    /** ２つの論理を融合 */
    function resolve(logics, idx1, idx2, key) {
        var newLogic = [];

        var lgs = [logics[idx1], logics[idx2]];
        for (var i = 0; i < lgs.length; i++) {
            var lg = lgs[i];
            if (lg instanceof Array) {
                if (lg.length == 2) {
                    if (!isUsingKey(lg, key)) throw new Error();
                } else if (lg.length == 3) {
                    if (lg[1] != "and" && lg[1] != "or") throw new Error();
                    if (!isUsingKey(lg[0], key)) newLogic.push(lg[0], "or");
                    if (!isUsingKey(lg[2], key)) newLogic.push(lg[2], "or");
                } else {
                    throw new Error();
                }
            } else {
                if (!isUsingKey(lg, key)) throw new Error();
            }
        }

        // 末尾の or を除去
        if (newLogic[newLogic.length - 1] == "or") {
            newLogic.pop();
        }

        // 単一配列は配列の外に出す
        if (newLogic.length == 1) {
            newLogic = newLogic[0];
        }

        println(logics[idx1], ", ", logics[idx2], " -> ", newLogic);

        return newLogic;
    }

    /** 論理が key を使っているかどうか */
    function isUsingKey(logic, key) {
        if (logic instanceof Array) {
            if (logic.length == 2) {
                if (logic[0] != "not") throw new Error();
                return logic[1] == key;
            } else {
                throw new Error();
            }
        } else {
            return logic == key;
        }
    }

    /** 否定 */
    function negate(logic) {
        switch (logic.length) {
            case 1: return ["not", logic[0]];
            case 2:
                switch (logic[0]) {
                    case "not": return logic[1];
                    default:throw new Error();
                }
            case 3:
                switch (logic[1]) {
                    case "or":  return [negate(logic[0]), "and", negate(logic[2])];
                    case "and": return [negate(logic[0]), "or", negate(logic[2])];
                    default:throw new Error();
                }
            default: throw new Error();
        }
    }

    /** 命題論理を追加 */
    function addLogic(logics, logic) {
        if (logic.length <= 2) {
            logics.push(logic);
        } else if (logic.length == 3) {
            if (logic[1] == "or") {
                logics.push(logic);
            } else if (logic[1] == "and") {
                addLogic(logics, logic[0]);
                addLogic(logics, logic[2]);
            } else {
                throw new Error();
            }
        } else {
            throw new Error();
        }
    }

    /** 融合のために、使われている記号と not がかかっている記号のハッシュテーブルを作る */
    function createResolveHashs(logics) {
        var nonNegateHash = {}, negateHash = {};

        for (var i = 0; i < logics.length; i++) {
            var logic = logics[i];

            if (!(logic instanceof Array) || logic.length == 2) {
                addOneLogicToHash(nonNegateHash, negateHash, logic, i);
            } else if (logic.length == 3) {
                addOneLogicToHash(nonNegateHash, negateHash, logic[0], i);
                addOneLogicToHash(nonNegateHash, negateHash, logic[2], i);
            } else {
                throw new Error();
            }
        }

        return {nonNegateHash: nonNegateHash, negateHash: negateHash};
    }

    /** 融合のためのハッシュに論理を一つ追加 */
    function addOneLogicToHash(nonNegateHash, negateHash, logic, i) {
        if (!(logic instanceof Array)) {
            addToArrayHashTable(nonNegateHash, logic, i);
        } else if (logic.length == 2) {
            if (logic[0] == "not") {
                addToArrayHashTable(negateHash, logic[1], i);
            } else {
                throw new Error();
            }
        } else {
            throw new Error();
        }
    }
})();
