// XPath を使って、式変形を行う。

window.onload = function() {
    main();
//    runByActions();

//    function runByActions() {
//        // 証明する式
//        var targets = [document.getElementById("target")];
//        // 公式・定義
//        var eqs = document.getElementById("eqs").children;
//
//        printEqs(targets, eqs);
//
//        var actions = [[0, 1, true], [0, 0, true]];
//
//        for (var k = 0; k < actions.length; k++) {
//            // x != f(f(x)) を x = f(x) で変形
//            var i, j, nextTargets = [];
//
//            var target = targets[actions[k][0]];
//            var eq = eqs[actions[k][1]];
//            var left2right = actions[k][2];
//
//            if (left2right) {
//                var newEqs = replaceByEq(target, eq.children[0], eq.children[1]);
//                pushAll(nextTargets, newEqs);
//            } else {
//                newEqs = replaceByEq(target, eq.children[1], eq.children[0]);
//                pushAll(nextTargets, newEqs);
//            }
//            console.log("newEqs", newEqs);
//
//            pushAll(nextTargets, targets);
//            targets = uniqueNodes(nextTargets);
//
//            // 矛盾を探す
//            if (hasContradiction(targets)) {
//                console.log("矛盾を発見！成功しました！ k = " + k);
//                break;
//            }
//        }
//
//        nextTargets = findIfSwapIf(targets[0]);
//        console.log("nextTargets", nextTargets);
//        targets.push(nextTargets[1]);
//
//        nextTargets = findIfSwapIf(nextTargets[1]);
////        console.log("nextTargets", nextTargets);
//        targets.push(nextTargets[1]);
//
//        // targets[4] の if (len(ary) == 2) の中の len(ary) == 2 が true に置換できる
//        nextTargets = findAndRemoveSameCondInIf(targets[4]);
//        console.log("nextTargets", nextTargets);
//        targets.push(nextTargets[0]);
//
//        // デバッグ出力
//        printEqs(targets, eqs);
//    }

    function main() {
        // 証明する式
        var targets = [document.getElementById("target")];
        // 公式・定義
        var eqs = document.getElementById("eqs").children;

        printEqs(targets, eqs);

        var target = targets[0], newTargets;
        for (var k = 0; k < 50; k++) {
            var newTargetCreated = false;

//            if (k == 37) {
//                console.log("k = 37");
//            }

            // if(A) { B } の B の中に A = true を入れる
            if (!newTargetCreated) {
                newTargets = findAndRemoveSameCondInIf(target);
                if (newTargets.length > 0) {
                    console.log("ifの条件をtrue/falseに置換しました");
                    target = newTargets[0];
                    newTargetCreated = true;
                }
            }

            // 木を複雑化する方の式変形の数
            var makeLargerEqCount = 2;

            if (!newTargetCreated) {
                // 末尾の等式優先（木を複雑化しない式変形）
                for (var i = eqs.length - 1; i >= makeLargerEqCount; i--) {
                    var eq = eqs[i];
                    newTargets = replaceByEq(target, eq.children[0], eq.children[1], true);
                    if (newTargets.length > 0) {
                        console.log("used eq = ", eq.children[0].getAttribute("name"), eq);
                        target = newTargets[0];
                        newTargetCreated = true;
                        break;
                    }
                }
            }

            if (!newTargetCreated) {
                newTargets = findIfSwapIf(target);
                if (newTargets.length > 0) {
                    console.log("ifの親関数をifの中に入れました");
                    target = newTargets[0];
                    newTargetCreated = true;
                }
            }

            if (!newTargetCreated) {
                // 木を複雑化する方の式変形
                for (i = makeLargerEqCount - 1; i >= 0; i--) {
                    eq = eqs[i];
                    // TODO なぜ、isUseRootReplace = false が必要？
                    newTargets = replaceByEq(target, eq.children[0], eq.children[1], false);
                    if (newTargets.length > 0) {
                        console.log("used eq = ", eq);
                        target = newTargets[0];
                        newTargetCreated = true;
                        break;
                    }
                }
            }

            if (!newTargetCreated) {
                console.error("Cannot change more, k = ", k);
                break;
            }

            targets = [target];
            
            printEqs(targets, []);

            // 矛盾チェック
            if (hasContradiction(targets)) {
                console.log("成功：背理法で矛盾を発見！式変形の回数は " + (k + 1) + " 回です。");
                break;
            }
        }
        console.log("終了");
    }

    function printEqs(targets, eqs) {
        if (targets.length > 1) {
            console.group("target");
            for (var i = 0; i < targets.length; i++) {
                console.log(i, targets[i]);
            }
            console.groupEnd();
        } else {
            console.log("target = ", targets[0]);
        }

        if (eqs.length > 0) {
            console.group("eq");
            for (i = 0; i < eqs.length; i++) {
                console.log(i, eqs[i]);
            }
            console.groupEnd();
        }
    }

    /** 矛盾を探す */
    function hasContradiction(targets) {
        for (var i = 0; i < targets.length; i++) {
            var target = targets[i];
            var children = target.children;
            if (nodeToString(children[0]) == nodeToString(children[1])) {
//                var s0 = nodeToString(children[0]);
//                var s1 = nodeToString(children[1]);
                return true;
            }
        }
        return false;
    }

    function findAndRemoveSameCondInIf(target) {
        var xpath = "*/..//func[@name='if']";
        var founds = cloneXpathFounds(document.evaluate(xpath, target, null, 5, null));

        var newTargets = [];
        for (var i = 0; i < founds.length; i++) {
            pushAll(newTargets, removeSameCondInIf(target, founds[i]));
        }
        return newTargets;
    }

    /** if (A) { B } の時、Bの中にAと同じ物が現れたら true / false に置き換える */
    function removeSameCondInIf(target, ifTerm) {
        var ifCond = ifTerm.children[0];
        if (ifCond.tagName == "CONST") return [];

        var ifCondStr = nodeToString(ifCond);
        var newTargets = [];

        function replace(thenTerm, replaceToValue, mustBeContains) {
            var thenTermStr = nodeToString(thenTerm);
            if (thenTermStr == ifCondStr) {
                // 置換先の定数項
                var constTerm = document.createElement("const");
                constTerm.setAttribute("value", "" + replaceToValue);
                constTerm.setAttribute("type", "boolean");

                // 置換
                var thenTermParent = thenTerm.parentNode;
                thenTermParent.replaceChild(constTerm, thenTerm);
                var newTarget = target.cloneNode(true);
//                target = newTarget;
                newTargets.push(newTarget);
                thenTermParent.replaceChild(thenTerm, constTerm);
            } else {
                if (mustBeContains || thenTermStr.indexOf(ifCondStr) != -1) {
                    // 子供をたどる
                    var children = thenTerm.children;
                    for (var i = 0; i < children.length; i++) {
                        replace(children[i], replaceToValue, true);
                    }
                }
            }
        }
        replace(ifTerm.children[1], true, false);
        replace(ifTerm.children[2], false, false);

        // 否定形で置換を行う
        var negateIfCond = negate(ifCond);
        if (negateIfCond != null) {
            ifCond = negateIfCond;
            ifCondStr = nodeToString(ifCond);
            replace(ifTerm.children[1], false, false);
            replace(ifTerm.children[2], true, false);
        }

        return newTargets;
//        return newTargets.length > 0 ? [target] : newTargets;
    }

    function findIfSwapIf(target) {
        var xpath = "*/..//func[@name='if']";
        var founds = cloneXpathFounds(document.evaluate(xpath, target, null, 5, null));

        var newTargets = [];
        for (var i = 0; i < founds.length; i++) {
            var newTarget = swapIf(target, founds[i]);
            if (newTarget != null) {
                newTargets.push(newTarget);
            }
        }
        return newTargets;
    }

    /** if と その親の関数を交換 */
    function swapIf(target, ifTerm) {
        var parent = ifTerm.parentNode;

        // ifの親がifでないことが条件
        if (parent.tagName != "FUNC" || parent.getAttribute("name") == "if") return null;

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
        ifTermClone.setAttribute("type", parent.getAttribute("type"));
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
    function replaceByEq(target, fromTerm, toTerm, isUseRootReplace) {
        // from を XPath に変換する
        function convertFromTerm2XPath(fromTerm) {
            return follow(fromTerm, -1).substring(1);

            function follow(term, pos) {
                var s;
                if (term.tagName == "VAR") {
                    s = '/*[';
                    if (pos >= 0) {
                        s += 'position()=' + (pos + 1);
                    }
                    var type = term.getAttribute("type");
                    if (type != "*") {
                        s += ' and @type="' + type + '"';
                    }
                    s += "]";
                } else if (term.tagName == "FUNC") {
                    s = '/func[';
                    if (pos >= 0) {
                        s += 'position()=' + (pos + 1) + ' and '
                    }
                    s += '@name="' + term.getAttribute("name") + '"]';
                } else if (term.tagName == "CONST") {
//                    s = '/const[';
                    s = '/*['; // TODO const にすると誤動作する。後で原因究明！
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
        var fromXpath = (isUseRootReplace ? "*/..//" : "*//") + convertFromTerm2XPath(fromTerm);

        // 変換元を探す
//        console.log("fromXpath", fromXpath);
        var founds = document.evaluate(fromXpath, target, null, 7, null);
//        console.log("founds", founds);

//        if (founds.snapshotLength == 0) {
//            founds = document.evaluate(fromXpath.substring(3), target, null, 7, null);
//        }

        var eqs = [];
        for (var i = 0; i < founds.snapshotLength; i++) {
            var found = founds.snapshotItem(i);

            // 変数を埋める
            var vars = {}, isMismatch = false;
            function fillVars(fromTerm, found) {
                if (fromTerm.tagName == "VAR") {
                    var name = fromTerm.getAttribute("name");
                    if (name in vars) {
                        if (nodeToString(vars[name]) != nodeToString(found)) {
                            isMismatch = true;
                            return;
                        }
                    } else {
                        vars[name] = found; // 変数を2回使っている場合、未対応
                    }
                }

                var children1 = fromTerm.children, children2 = found.children;
                if (fromTerm.tagName != "VAR" && 
                        children1.length != children2.length) { isMismatch = true; return; }
                for (var i = 0; i < children1.length; i++) {
                    fillVars(children1[i], children2[i]);
                    if (isMismatch) return;
                }
            }
            fillVars(fromTerm, found);
            // console.log("vars", vars);

            if (isMismatch) continue;

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

//            if (cloneNode.children[0].tagName == "CONST" && cloneNode.children[1].tagName == "CONST") {
//                console.log("Error");
//            }
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

        // name でソート
        var attrs = node.attributes;
        var attributes = [];
        for (var i = 0; i < attrs.length; i++) {
            attributes.push({name: attrs[i].name, value: attrs[i].value});
        }
        attributes.sort(function (a, b) { return a.name < b.name ? -1 : 1 });

        for (i = 0; i < attributes.length; i++) {
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

    /**
     * TODO ここは、もっと汎用的なパターンマッチングに置き換える必要あり！
     * a > b -> a < b 要素は == にならないことを利用
     */
    function negate(term) {
        // lt のみ扱う
        if (term.tagName != "FUNC" || term.getAttribute("name") != "lt") return null;

        var termClone = term.cloneNode(true);

        // 2つの引数を入れ替え
        var temp = termClone.children[0];
        termClone.replaceChild(termClone.children[1], temp);
        termClone.appendChild(temp);

        return termClone;
    }
};
