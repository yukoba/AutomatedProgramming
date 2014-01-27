import groovy.transform.TypeChecked

import static Sort5Exprs.*

/**
 * Sort5 では挿入ソートの内側ループの証明をやっています。
 *
 * sort4.html と比較して、以下の点が改良されています。
 * 1. XPath を使用していたのを廃止して、普通に木構造をたどっています。
 * 2. if の親関数を子関数に繰り込むのに特殊な処理をしていません。
 * 3. 与える式が XML から文字列になり、読みやすくなっています。
 * 4. tail と head の定義がおかしかったので、逆にしました。
 *
 * 注意：本来 Node の子供は Node, String が取れるのですが、Node しか使用しないというルールでコードを書いています。
 *
 * 各 Node は以下の３つの情報を持っています。
 * 1. 関数名（name）
 * 2. 型 (type)
 * 3. 子供 Node
 */
@TypeChecked
class Sort5 {
    static final NodePrinter nodePrinter = new NodePrinter()

    static void main(String[] args) {
        def target = eqTextToNode(targetText)
        // exprTexts -> eqs
        def eqs = exprTexts.collect { eqTextToNode(it as String) }

        for (int k = 0; k < 50; k++) {
            def newTargetCreated = false

            // if (A) { B } の B の中に A = true を入れる
            if (!newTargetCreated) {
                def newTargets = findAndRemoveSameCondInIf(target)
                if (newTargets.size() > 0) {
                    println "${k + 1}回目：if の条件を true/false に置換しました"
                    target = newTargets[0]
                    newTargetCreated = true
                }
            }

            // 通常の式変形
            if (!newTargetCreated) {
                for (int i = 0; i < eqs.size(); i++) {
                    def eq = eqs[i]
                    def newTargets = replaceByEq(target, eq.children()[0] as Node, eq.children()[1] as Node,
                            false)
//                            i == eqs.size() - 2) // TODO ずる：頭の isSorted を置換されると困る
                    if (newTargets.size() > 0) {
                        println "${k + 1}回目：下記の式を代入"
                        println eq
                        target = newTargets[0]
                        newTargetCreated = true
                        break
                    }
                }
            }

            if (!newTargetCreated) {
                println "${k + 1}回目：これ以上式変形が出来ません"
                break
            }

            println "変換結果"
            println target
            println "-----------------------------------------------------------------------------------------------"

            // 矛盾チェック
            if (hasContradiction([target])) {
                println "成功：背理法で矛盾を発見！式変形の回数は ${k + 1} 回です。"
                break
            }
        }
    }

    /** 矛盾を探す */
    static boolean hasContradiction(List<Node> targets) {
        for (def target in targets) {
            def children = target.children() as List<Node>
            if (children[0].name() == "FALSE" && children[1].name() == "TRUE") return true
            if (children[1].name() == "FALSE" && children[0].name() == "TRUE") return true
        }
        return false
    }

    // ------------------------------------------------------------------------------------------------------------

    static List<Node> findAndRemoveSameCondInIf(Node target) {
        def newTargets = []
        def ifTerms = target.breadthFirst().findAll { Node n -> n.name() == "if" }
        for (def ifTerm in ifTerms) {
            newTargets.addAll(removeSameCondInIf(target, ifTerm as Node))
        }
        return newTargets
    }

    /** if (A) { B } の時、A と同じ物が B の中に現れたら TRUE, FALSE に置き換える */
    static List<Node> removeSameCondInIf(Node target, Node ifTerm) {
        def newTargets = []

        def ifChildren = ifTerm.children() as List<Node>
        def ifCond = ifChildren[0]
        if (ifCond.name() == "TRUE" || ifCond.name() == "FALSE") return newTargets
        def ifCondStr = ifCond as String

        // 肯定系で置換
        replaceThenTerm(ifCondStr, ifChildren[1], "TRUE", target, newTargets)
        replaceThenTerm(ifCondStr, ifChildren[2], "FALSE", target, newTargets)

        // 否定形で置換
        def negateIfCond = negate(ifCond)
        if (negateIfCond != null) {
            def negateIfCondStr = negateIfCond as String
            replaceThenTerm(negateIfCondStr, ifChildren[1], "FALSE", target, newTargets)
            replaceThenTerm(negateIfCondStr, ifChildren[2], "TRUE", target, newTargets)
        }

        return newTargets
    }

    /** if の条件が then の中に見つかったら replaceTo に置き換える */
    static void replaceThenTerm(String ifCondStr, Node thenTerm, String replaceTo, Node target, List newTargets) {
        def thenTermStr = thenTerm as String
        if (thenTermStr == ifCondStr) {
            // 定数項に置換
            def constTerm = new Node(null, replaceTo, [type: "Boolean"])
            swapNode(thenTerm, constTerm)
            newTargets << target.clone()
            swapNode(thenTerm, constTerm)
        } else if (thenTermStr.contains(ifCondStr)) {
            // 子供をたどる
            for (def child in thenTerm.children()) {
                replaceThenTerm(ifCondStr, child as Node, replaceTo, target, newTargets)
            }
        }
    }

    /**
     * TODO ここは、もっと汎用的なパターンマッチングに置き換える必要あり！
     * a > b -> a < b 要素は == にならないことを利用し、子ノード２つを入れ替えてる。
     */
    static Node negate(Node term) {
        // lt のみ扱う
        if (term.name() == "lt") {
            def termClone = term.clone() as Node
            termClone.children().reverse(true)
            return termClone
        } else {
            return null
        }
    }

    // ------------------------------------------------------------------------------------------------------------

    /** target に対して、fromTerm -> toTerm の変形を施す */
    static List<Node> replaceByEq(Node target, Node fromTerm, Node toTerm, boolean skipTop = false) {
        def eqs = []
        def targetDescendants = target.breadthFirst() as List<Node>
        if (skipTop) targetDescendants = targetDescendants.drop(3) // TODO ずる：頭の isSorted を置換されると困る
        for (Node targetChild in targetDescendants) {
            if (targetChild.name() == "eq") continue

            // 変数を埋める
            def var2node = new HashMap()
            def isMatch = fillVar2node(var2node, fromTerm, targetChild)
            if (!isMatch) continue

            // var2node を使って、変換先のノードを作る
            def newTerm = fillIfType(replaceVar(toTerm, var2node))

            // 差し替える
            swapNode(targetChild, newTerm)
            // クローンして追加
            eqs << target.clone()
            // 元に戻す
            swapNode(targetChild, newTerm)
        }

        // ユニーク化して返す
        return uniqueNodes(eqs)
    }

    /** pattern が target に適合するかどうか調べながら、var2node を埋める。戻り値はパターンマッチしたかどうか。 */
    static boolean fillVar2node(Map var2node, Node pattern, Node target) {
        def patternName = pattern.name()
        def patternType = pattern.attribute("type")
        def targetName = target.name()
        def targetType = target.attribute("type")

        // 型が一致する事を確認
        if (patternType != "*" && targetType != "*") {
            if (patternType != targetType) return false
        }

        if (isVar(patternName)) {
            // パターン側が変数
            if (pattern.children().size() == 0) {
                // パターン側で子供なし
                if (patternName in var2node) {
                    // 変数が使用済みなら内容は同一でないといけない
                    if ((var2node[patternName] as String) != (target as String)) return false
                } else {
                    // 変数に target の子 Node 含め全て代入
                    var2node[patternName] = target
                }

                // パターンの方で「変数」かつ「子供なし」は target の子供をチェックせずに全て変数に代入
                return true
            } else {
                // パターン側で子供あり。その場合は、var2node には関数名だけを String で入れる。
                if (patternName in var2node) {
                    // 変数が使用済みなら内容は同一でないといけない
                    if (var2node[patternName] != target.name()) return false
                } else {
                    var2node[patternName] = target.name()
                }

                // パターンの子供も全てマッチすることをこの後チェック
            }
        } else {
            // パターン側が定数もしくは関数
            if (patternName != targetName) return false
        }

        // 子供のパターンマッチング
        def patternChildren = pattern.children() as List<Node>
        def targetChildren = target.children() as List<Node>
        if (patternChildren.size() != targetChildren.size()) return false
        for (int i = 0; i < patternChildren.size(); i++) {
            def isMatch = fillVar2node(var2node, patternChildren[i], targetChildren[i])
            if (!isMatch) return false
        }

        return true // マッチした
    }

    /** var2nodeを使って term の変数を置き換えた新しい Node を作成。 */
    static Node replaceVar(Node term, Map var2node) {
        def result
        def termName = term.name()
        if (isVar(termName)) {
            // 変数を置き換える
            if (termName in var2node) {
                if (var2node[termName] instanceof String) {
                    result = new Node(null, var2node[termName], [type: typeMap[var2node[termName] as String]])
                } else {
                    return var2node[termName].clone() as Node
                }
            } else {
                return term.clone() as Node
            }
        } else {
            // 子供のない浅いコピーを作る
            result = new Node(null, termName, term.attributes().clone())
        }
        // 子供を埋める
        for (def child in term.children()) {
            result.append(replaceVar(child as Node, var2node))
        }
        return result
    }

    /** ノードの List に対して、各ノードを文字列化して、重複するのを除去して、ユニークな集合にする */
    static List uniqueNodes(List nodes) {
        def hash = new HashSet()
        def list = []
        for (def node in nodes) {
            def str = node as String
            if (!(str in hash)) {
                hash << str
                list << node
            }
        }
        return list
    }

    // ------------------------------------------------------------------------------------------------------------

    static Node convertExpr(String s) { fillIfType(convertExprListToNode(null, convertExprTextToList(removeSpace(s)))) }

    static List convertExprTextToList(String s) {
        List list = []
        int start = s.indexOf('(')
        if (start == -1) return [s]
        int end = s.lastIndexOf(')')
        list.add(s[0..(start - 1)])

        int bracketCount = 0
        StringBuilder sb = new StringBuilder()
        for (int i = start + 1; i < end; i++) {
            switch (s[i]) {
                case "(": bracketCount++; sb.append(s[i]); break;
                case ")": bracketCount--; sb.append(s[i]); break;
                case ",":
                    if (bracketCount == 0) {
                        list.add(convertExprTextToList(sb.toString()))
                        sb = new StringBuilder()
                    } else {
                        sb.append(s[i])
                    }
                    break
                default:
                    sb.append(s[i])
                    break
            }
        }
        assert bracketCount == 0
        if (sb.size() > 0)
            list.add(convertExprTextToList(sb.toString()))
        return list
    }

    static Node convertExprListToNode(Node parent, List list) {
        Node node = new Node(parent, list[0] as String, [type: typeMap[list[0] as String]])
        for (int i = 1; i < list.size(); i++) {
            convertExprListToNode(node, list[i] as List)
        }
        return node
    }

    static Node fillIfType(Node node) {
        for (Node n in (node.depthFirst() as List<Node>)) {
            if (n.name() == "if") {
                n.attributes()["type"] = (n.children()[1] as Node).attribute("type")
            }
        }
        return node
    }

    // ------------------------------------------------------------------------------------------------------------

    static void println(Node node) { nodePrinter.print(node) }

    /** Node を読みやすい文字列に変換 */
    static String node2string(Node node) {
        def sw = new StringWriter()
        def np = new NodePrinter(new PrintWriter(sw))
        np.print(node)
        return sw.toString()
    }

    static String removeSpace(String s) { s.replaceAll(' ', '') }

    /** 文字列形式の等式を Node に変換 */
    static Node eqTextToNode(String eqText) {
        Node node = new Node(null, "eq")
        eqText.split("==").each { node.append(convertExpr(it as String)) }
        return node
    }

    static void swapNode(Node node1, Node node2) {
        // 名前
        def name1 = node1.name()
        node1.name = node2.name()
        node2.name = name1

        // 属性
        def attributes1 = node1.attributes()
        def attributes2 = node2.attributes()
        def attributes2clone = node2.attributes().clone()
        attributes2.clear()
        attributes2.putAll(attributes1)
        attributes1.clear()
        attributes1.putAll(attributes2clone as Map)

        // 子供
        def node1children = [] as List<Node>
        def node2children = [] as List<Node>
        node1children.addAll(node1.children())
        node2children.addAll(node2.children())
        for (Node n in node1children) {
            node1.remove(n)
        }
        for (Node n in node2children) {
            node2.remove(n)
            node1.append(n)
        }
        for (Node n in node1children) {
            node2.append(n)
        }

        // Node の String value は使用しないので swap しない
    }

    static boolean isVar(s) { s in vars }

    static boolean isConst(s) { s in consts }
}
