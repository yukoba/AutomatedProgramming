// XPath を使って、式変形を行う。

window.onload = function() {
//    main();
    runByActions();

    function runByActions() {
        // 証明する式
        var targets = [document.getElementById("target")];
        // 公式・定義
        var eqs = document.getElementById("eqs").children;

        printEqs(targets, eqs);

        var actions = [[0, 1, true], [0, 0, true]];

        for (var k = 0; k < actions.length; k++) {
            // x != f(f(x)) を x = f(x) で変形
            var i, j, nextTargets = [];

            var target = targets[actions[k][0]];
            var eq = eqs[actions[k][1]];
            var left2right = actions[k][2];

            if (left2right) {
                var newEqs = replaceByEq(target, eq.children[0], eq.children[1]);
                pushAll(nextTargets, newEqs);
            } else {
                newEqs = replaceByEq(target, eq.children[1], eq.children[0]);
                pushAll(nextTargets, newEqs);
            }
            console.log("newEqs", newEqs);

            pushAll(nextTargets, targets);
            targets = uniqueNodes(nextTargets);

            // 矛盾を探す
            if (hasContradiction(targets)) {
                console.log("矛盾を発見！成功しました！ k = " + k);
                break;
            }
        }

        nextTargets = findIfSwapIf(targets[0]);
//        console.log(nextTargets);
        targets.push(nextTargets[1]);


        nextTargets = findIfSwapIf(nextTargets[1]);
//        console.log("nextTargets", nextTargets);
        targets.push(nextTargets[1]);

        // TODO targets[4] の if (len(ary) == 2) の中の len(ary) == 2 が true に置換できる

        // デバッグ出力
        printEqs(targets, eqs);
    }

    function main() {
        // 証明する式
        var targets = [document.getElementById("target")];
        // 公式・定義
        var eqs = document.getElementById("eqs").children;

        printEqs(targets, eqs);

        for (var k = 0; k < 1; k++) {
            // x != f(f(x)) を x = f(x) で変形
            var i, j, nextTargets = [];
            for (j = 0; j < targets.length; j++) {
                var target = targets[j];
                console.group("target");
                console.log("target", target);
                for (i = 0; i < eqs.length; i++) {
                    var eq = eqs[i];

                    console.group("eq " + i);
                    console.log("eq", eq);
//                    if (k == 1 && j == 0 && i == 2) {
//                        console.log("target", target, eq);
//                    }

                    var newEqs = replaceByEq(target, eq.children[0], eq.children[1]);
                    pushAll(nextTargets, newEqs);

                    newEqs = replaceByEq(target, eq.children[1], eq.children[0]);
                    pushAll(nextTargets, newEqs);
                    console.groupEnd();
                }
                console.groupEnd();
//                if (k == 1 && j == 0) break;
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
        printEqs(targets, eqs);
    }

    function printEqs(targets, eqs) {
        console.group("target");
        for (i = 0; i < targets.length; i++) {
            console.log(i, targets[i]);
        }
        console.groupEnd();

        console.group("eq");
        for (i = 0; i < eqs.length; i++) {
            console.log(i, eqs[i]);
        }
        console.groupEnd();
    }

    /** 矛盾を探す */
    function hasContradiction(targets) {
        for (var i = 0; i < targets.length; i++) {
            var target = targets[i];
            var children = target.children;
            if (nodeToString(children[0]) == nodeToString(children[1])) {
                var s0 = nodeToString(children[0]);
                var s1 = nodeToString(children[1]);
                return true;
            }
        }
        return false;
    }

    function findIfSwapIf(target) {
        var xpath = "*//func[@name='if']";
        var founds = cloneXpathFounds(document.evaluate(xpath, target, null, 5, null));

        var eqs = [];
        for (var i = 0; i < founds.length; i++) {
            eqs.push(swapIf(target, founds[i]));
        }
        return eqs;
    }

    /** if と その親の関数を交換 */
    function swapIf(target, ifTerm) {
        var parent = ifTerm.parentNode;
        var ifTermIdx = getNodeIndexInParent(ifTerm);
        var origIfTermChild1 = ifTerm.children[1], origIfTermChild2 = ifTerm.children[2];

        // parent clone を作る
        var parent1 = parent.cloneNode(true);
        var parent2 = parent.cloneNode(true);

        // parent - ifTerm - parent clone - ifTermの子 という状態を作る
        parent1.replaceChild(origIfTermChild1.cloneNode(true), parent1.children[ifTermIdx]);
        parent2.replaceChild(origIfTermChild2.cloneNode(true), parent2.children[ifTermIdx]);
        ifTerm.replaceChild(parent1, origIfTermChild1);
        ifTerm.replaceChild(parent2, origIfTermChild2);

        // クローンして戻り値を作成
        var ifTermClone = ifTerm.cloneNode(true);
        var resultTarget;
        if (target === parent) {
            resultTarget = ifTermClone;
        } else {
            // parentの親 - ifTerm という状態を作り、間の parent を抜く
            var parentParentNode = parent.parentNode;
            parentParentNode.replaceChild(ifTermClone, parent);
            resultTarget = target.cloneNode(true);
            parentParentNode.replaceChild(parent, ifTermClone);
        }

        // 元に戻す。やることは ifTerm - parent clone - ifTermの子 の parent clone を除去
        ifTerm.replaceChild(origIfTermChild1, parent1);
        ifTerm.replaceChild(origIfTermChild2, parent2);

        return resultTarget;
    }

    /** target に対して、fromTerm -> toTerm の変形を施す */
    function replaceByEq(target, fromTerm, toTerm) {
        // from を XPath に変換する
        function convertFromTerm2XPath(fromTerm) {
            return follow(fromTerm, -1).substring(1);

            function follow(term, pos) {
                var s;
                if (term.tagName == "VAR") {
                    s = '/*[';
                    if (pos >= 0) {
                        s += 'position()=' + (pos+1) + ' and ';
                    }
                    s += '@type="' + term.getAttribute("type") + '"]';
                } else if (term.tagName == "FUNC") {
                    s = '/func[';
                    if (pos >= 0) {
                        s += 'position()=' + (pos+1) + ' and '
                    }
                    s += '@name="' + term.getAttribute("name") + '"]';
                } else if (term.tagName == "CONST") {
                    s = '/const[';
                    if (pos >= 0) {
                        s += 'position()=' + (pos+1) + ' and '
                    }
                    s += '@value="' + term.getAttribute("value") + '" and '+
                            '@type="' + term.getAttribute("type") + '"]';
                }

                var children = term.children;
                for (var i = 0; i < children.length; i++) {
                    s += follow(children[i], i) + "/..";
                }

                return s;
            }
        }
        var fromXpath = "*//" + convertFromTerm2XPath(fromTerm);

        // 変換元を探す
        console.log("fromXpath", fromXpath);
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
                    result = vars[toTerm.getAttribute("name")];
                    return (result == null ? toTerm : result).cloneNode(true);
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

            if (cloneNode.children[0].tagName == "CONST" && cloneNode.children[1].tagName == "CONST") {
                console.log("Error");
            }
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
