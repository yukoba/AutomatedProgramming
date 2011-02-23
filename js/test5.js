// XPath を使って、式変形を行う。
// x = f(x) が事前に与えられた公式で、
// x != f(f(x)) を式変形して、矛盾を見つけ出す

window.onload = function() {
    main();
//    test();

    function test() {
        var target = document.getElementById("eq1");
        var eq = target.cloneNode(true);
        var newEqs = replaceByEq(target, eq.children[1], eq.children[0]);

        // デバッグ出力
        for (var i = 0; i < newEqs.length; i++) {
            console.log(i, newEqs[i]);
        }
    }

    function main() {
        // x != f(f(x))
        var targets = [document.getElementById("target")];
        // x = f(x)
        var eqs = [document.getElementById("eq1")];

        for (var k = 0; k < 3; k++) {
            // x != f(f(x)) を x = f(x) で変形
            var i, j, nextTargets = [];
            for (j = 0; j < targets.length; j++) {
                var target = targets[j];
                for (i = 0; i < eqs.length; i++) {
                    var eq = eqs[i];

                    var newEqs = replaceByEq(target, eq.children[0], eq.children[1]);
                    pushAll(nextTargets, newEqs);

                    newEqs = replaceByEq(target, eq.children[1], eq.children[0]);
                    pushAll(nextTargets, newEqs);
                }
            }
            pushAll(nextTargets, targets);
            targets = uniqueNodes(nextTargets);

            // 矛盾を探す
            if (hasContradiction(targets)) {
                console.log("矛盾を発見！成功しました！ k = " + k);
                break;
            }
        }

        // デバッグ出力
        for (i = 0; i < targets.length; i++) {
            console.log(i, targets[i]);
        }
    }

    /** 矛盾を探す */
    function hasContradiction(targets) {
        for (var i = 0; i < targets.length; i++) {
            var target = targets[i];
            var children = target.children;
            if (nodeToString(children[0]) == nodeToString(children[1])) {
                return true;
            }
        }
        return false;
    }

    /** target に対して、fromTerm -> toTerm の変形を施す */
    function replaceByEq(target, fromTerm, toTerm) {
        // from を XPath に変換する
        function convertFromTerm2XPath(fromTerm) {
            return follow(fromTerm).substring(1);

            function follow(term) {
                var s;
                if (term.tagName == "VAR") {
                    s = '/*[@type="' + term.getAttribute("type") + '"]';
                } else if (term.tagName == "FUNC") {
                    s = '/func[@name="' + term.getAttribute("name") + '"]';
                }

                var children = term.children;
                for (var i = 0; i < children.length; i++) {
                    s += follow(children[i]) + "/..";
                }

                return s;
            }
        }
        var fromXpath = convertFromTerm2XPath(fromTerm);

        // 変換元を探す
        var founds = document.evaluate(fromXpath, target, null, 7, null);
        // console.log("founds", founds);

        var eqs = [];
        for (var i = 0; i < founds.snapshotLength; i++) {
            var found = founds.snapshotItem(i);

            // 変数を埋める
            var vars = {};
            function fillVars(fromTerm, found) {
                if (fromTerm.tagName == "VAR") {
                    vars[fromTerm.getAttribute("name")] = found; // 変数を2回使っている場合、未対応
                }

                var children1 = fromTerm.children, children2 = found.children;
                for (var i = 0; i < children1.length; i++) {
                    fillVars(children1[i], children2[i]);
                }
            }
            fillVars(fromTerm, found);
            // console.log("vars", vars);

            // vars を使って、変換先のノードを作る
            function createTransformed(toTerm) {
                // 変数を置き換える
                var result;
                if (toTerm.tagName == "VAR") {
                    result = vars[toTerm.getAttribute("name")].cloneNode(true);
                    return result == null ? toTerm : result;
                }

                // 子供のない浅いコピーを作る
                result = toTerm.cloneNode(false);
                removeAllChildren(result);

                // 子供を埋める
                var children = toTerm.children;
                for (var i = 0; i < children.length; i++) {
                    result.appendChild(createTransformed(children[i]));
                }
                return result;
            }
            var newTerm = createTransformed(toTerm);
            // console.log("newTerm", newTerm);

            // 差し替える
            var parent = found.parentNode;
            parent.replaceChild(newTerm, found);

            // クローンして追加
            var cloneNode = target.cloneNode(true);
            cloneNode.removeAttribute("id");
            eqs.push(cloneNode);

            // 元に戻す
            parent.replaceChild(found, newTerm);
        }

        // ユニーク化して返す
        return uniqueNodes(eqs);
    }

    /** 子ノードを全て除去 */
    function removeAllChildren(node) {
        var children = node.childNodes;
        for (var i = children.length - 1; i >= 0; i--) {
            node.removeChild(children[i]);
        }
    }

    /** 独自ノードの文字列化 */
    function nodeToString(node) {
        var children = node.children;

        var s = "<" + node.tagName.toLowerCase();
        var attributes = node.attributes; // 本当は name でソートする必要あり！
        for (var i = 0; i < attributes.length; i++) {
            s += " " + attributes[i].name + "='" + attributes[i].value + "'";
        }
        if (children.length == 0) return s + "/>";

        s += ">";

        for (i = 0; i < children.length; i++) {
            s += nodeToString(children[i]);
        }

        s += "</" + node.tagName.toLowerCase() + ">";

        return s;
    }

    /** ノードの Array に対して、各ノードを文字列化して、ユニークな集合にする */
    function uniqueNodes(nodes) {
        var hash = {}, ary = [];
        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
            var str = nodeToString(node);
            if (str in hash) continue;
            hash[str] = true;
            ary.push(node);
        }
        return ary;
    }
};
