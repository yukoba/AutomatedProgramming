import groovy.transform.TypeChecked

import static Sort5Exprs.*

@TypeChecked
class Sort5 {
    static final NodePrinter nodePrinter = new NodePrinter()

    static void main(String[] args) {
        // exprTexts -> eqs
        def eqs = exprTexts.collect { eqToNode((it as String).split("==").collect { convertExpr(it as String) }) }
        // 一番最後の式が証明対象
        def target = eqs.last()

        for (int k = 0; k < 1; k++) {
            def newTargetCreated = false

            // if (A) { B } の B の中に A = true を入れる
            if (!newTargetCreated) {
                def newTargets = findAndRemoveSameCondInIf(target)
                if (newTargets.size() > 0) {
                    println "if の条件を true/false に置換しました"
                    target = newTargets[0]
                    newTargetCreated = true
                }
            }

            // 通常の式変形
            if (!newTargetCreated) {
                for (int i = 0; i < eqs.size(); i++) {
                    def eq = eqs[i]
                    def newTargets = replaceByEq(target, eq.children()[0] as Node, eq.children()[1] as Node)
                    if (newTargets.size() > 0) {
                        println "下記の式を代入"
                        println eq
                        target = newTargets[0]
                        newTargetCreated = true
                        break
                    }
                }
            }

            if (!newTargetCreated) {
                println "これ以上式変形が出来ません。k = $k"
                break
            }

            println "変換結果"
            println target

            // 矛盾チェック
            if (hasContradiction([target])) {
                println "成功：背理法で矛盾を発見！式変形の回数は ${k + 1}回です。"
                break
            }
        }

        println "終了"
    }

    /** 矛盾を探す */
    static boolean hasContradiction(List<Node> targets) {
        for (def target in targets) {
            def children = target.children()
            if ((children[0] as String) == (children[1] as String)) {
                return true
            }
        }
        return false
    }

    static List<Node> findAndRemoveSameCondInIf(Node target) {
        def founds = target.depthFirst().findAll { Node n -> n.name() == "if" }.clone() as List<Node>
        def newTargets = []
        for (Node found in founds) {
            newTargets.addAll(removeSameCondInIf(target, found))
        }
        return newTargets
    }

    /** if (A) { B } の時、Bの中にAと同じ物が現れたら true / false に置き換える */
    static List<Node> removeSameCondInIf(Node target, Node ifTerm) {
        def ifChildren = ifTerm.children() as List<Node>
        def ifCond = ifChildren[0]
        if (ifCond.name() == "TRUE" || ifCond.name() == "FALSE") return []

        def ifCondStr = ifCond as String
        def newTargets = []

        Closure replace = null
        replace = { Node thenTerm, boolean replaceToValue, boolean mustBeContains ->
            def thenTermStr = thenTerm as String
            if (thenTermStr == ifCondStr) {
                // 定数項に置換
                def constTerm = new Node(null, replaceToValue.toString().toUpperCase(), [type: "Boolean"])
                swapNode(thenTerm, constTerm)
                newTargets << target.clone()
                swapNode(thenTerm, constTerm)
            } else {
                if (mustBeContains || thenTermStr.contains(ifCondStr)) {
                    // 子供をたどる
                    thenTerm.children().each { replace(it as Node, replaceToValue, true) }
                }
            }
        }
        replace(ifChildren[1], true, false)
        replace(ifChildren[2], false, false)

        // 否定形で置換を行う
        def negateIfCond = negate(ifCond)
        if (negateIfCond != null) {
            ifCond = negateIfCond
            ifCondStr = ifCond as String
            replace(ifChildren[1], false, false)
            replace(ifChildren[2], true, false)
        }

        return newTargets
    }

    /** Node を検索条件に変換する。検索条件は全て true でないといけない。 */
    static List<Closure> convertTermToSearchCond(Node term, int pos = -1) {
        def name = term.name()
        def type = term.attribute("type")

        def conds = []
        if (isVar(name)) {
            if (pos >= 0) {
                conds << { Node node -> position(node) == pos }
            }
            if (type != "*") {
                conds << { Node node -> node.attribute("type") == type }
            }
        } else if (isConst(name)) {
            conds << { Node node -> node.name() == name }
            conds << { Node node -> node.attribute("type") == type }
            if (pos >= 0) {
                conds << { Node node -> position(node) == pos }
            }
        } else { // 関数
            conds << { Node node -> node.name() == name }
            if (pos >= 0) {
                conds << { Node node -> position(node) == pos }
            }
        }

        def children = term.children()
        for (int i = 0; i < children.size(); i++) {
            def childConds = convertTermToSearchCond(children[i] as Node, i)
            def i2 = i as int // クロージャーにバインドさせるため、新しいインスタンスを作成
            conds << { Node node -> isAllTrue(childConds, node.children()[i2]) }
        }

        return conds
    }

    /** var2node を埋める。戻り値は var2node の作成に成功したかどうか。 */
    static boolean fillVar2node(Map var2node, Node from, Node found) {
        def fromName = from.name()
        if (isVar(fromName)) {
            if (fromName in var2node) {
                if ((var2node[fromName] as String) != (found as String)) {
                    return false
                }
            } else {
                var2node[fromName] = found // 変数を2回使っている場合、未対応
            }
        }

        def fromChildren = from.children()
        def foundChildren = found.children()
        if (isVar(fromName) && fromChildren.size() != foundChildren.size()) {
            return false
        }
        for (int i = 0; i < fromChildren.size(); i++) {
            def isOK = fillVar2node(var2node, fromChildren[i] as Node, foundChildren[i] as Node)
            if (!isOK) return false
        }

        return true // 成功
    }

    /** var2nodeを使って term の変数を置き換えた新しい Node を作成。 */
    static Node replaceVar(Node term, Map var2node) {
        def termName = term.name()
        if (isVar(termName)) {
            // 変数を置き換える
            return (termName in var2node ? var2node[termName] : term).clone() as Node
        } else {
            // 子供のない浅いコピーを作る
            def result = new Node(null, termName, term.attributes().clone())

            // 子供を埋める
            for (def child in term.children()) {
                result.appendNode(replaceVar(child as Node, var2node))
            }
            return result
        }
    }

    /** target に対して、fromTerm -> toTerm の変形を施す */
    static List<Node> replaceByEq(Node target, Node fromTerm, Node toTerm) {
        // fromTerm を 条件群 に変換する
        def searchCond = convertTermToSearchCond(fromTerm)
        // 変換元を探す
        def founds = target.depthFirst().findAll { isAllTrue(searchCond, it) } as List<Node>

        def eqs = []
        for (def found in founds) {
            // 変数を埋める
            def var2node = new HashMap()
            def isOK = fillVar2node(var2node, fromTerm, found)
            if (!isOK) continue

            // var2node を使って、変換先のノードを作る
            def newTerm = replaceVar(toTerm, var2node)

            // 差し替える
            swapNode(found, newTerm)
            // クローンして追加
            eqs << target.clone()
            // 元に戻す
            swapNode(found, newTerm)
        }

        // ユニーク化して返す
        return uniqueNodes(eqs)
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

    /**
     * TODO ここは、もっと汎用的なパターンマッチングに置き換える必要あり！
     * a > b -> a < b 要素は == にならないことを利用
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

    // -----------------------------------------------------------------------------------------

    static Node convertExpr(String s) { convertExprListToNode(null, convertExprTextToList(removeSpace(s))) }

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

    static void println(Node node) { nodePrinter.print(node) }

    static String removeSpace(String s) { s.replaceAll(' ', '') }

    static Node eqToNode(List<Node> list) {
        Node node = new Node(null, "eq")
        list.each { node.append(it as Node) }
        return node
    }

    static void swapNode(Node node1, Node node2) {
        def attributes1 = node1.attributes()
        def attributes2 = node2.attributes()
        def attributes2clone = node2.attributes().clone()

        attributes2.clear()
        attributes2.putAll(attributes1)
        attributes1.clear()
        attributes1.putAll(attributes2clone as Map)

        def value1 = node1.value()
        node1.value = node2.value()
        node2.value = value1

        def name1 = node1.name()
        node1.name = node2.name()
        node2.name = name1
    }

    static boolean isAllTrue(List<Closure> list, def node) {
        if (node == null) return false
        for (def closure in list) {
            if (!closure.call(node)) return false
        }
        return true
    }

    static int position(Node node) { node.parent().children().indexOf(node) }

    static boolean isVar(s) { s in vars }

    static boolean isConst(s) { s in consts }
}
