import groovy.transform.TypeChecked

@TypeChecked
class Sort5 {
    static final List exprTexts = [
            "isSorted(tail(list0)) == TRUE",
            "lt(head(tail(tail(list0))), head(tail(list0))) == TRUE",
            "isSorted(sortInner(tail(list0))) == TRUE",
            "isSorted(sortInner(tail(swap(list0)))) == TRUE",

            // 公式・定義・公理
            "head(tail(swap(list0))) == head(list0)",
            "tail(tail(swap(list0))) == tail(tail(list0))",

            "tail(append(e0, list0)) == list0",
            "head(append(e0, list0)) == e0",

            "lt(len(append(e0, list0)), 2) == TRUE",

            "if(TRUE, any0, any1) == any0",
            "if(FALSE, any0, any1) == any1",
            "if(b0, any0, any0) == any0",

            "and(b0, TRUE) == b0",
            "and(TRUE, b0) == b0",

            // if の親関数を if の中に入れる
            "f0(if(b0, any1, any2)) == if(b0, f0(any1), f0(any2))",
            "f0(if(b0, any1, any2), any3) == if(b0, f0(any1, any3), f0(any2, any3))",
            "f0(if(b0, any1, any2), any3, any4) == if(b0, f0(any1, any3, any4), f0(any2, any3, any4))",

            // 判定条件
            "isSorted(list0) == " +
                    "if(lt(len(list0), 2), TRUE, and(lt(head(tail(list0)), head(list0)), isSorted(tail(list0))))",

            // 証明対象
            "sortInner(list0) == " +
                    "if(lt(len(list0), 2), list0, " +
                    "if(lt(head(tail(list0)), head(list0)), " +
                    "list0, " +
                    "append(sortInner(tail(swap(list0))), head(tail(list0)))))",
    ]

    static final Map typeMap = [
            sortInner: "List",
            isSorted: "Boolean",

            tail: "List",
            head: "Element",
            TRUE: "Boolean",
            FALSE: "Boolean",
            lt: "Boolean",
            append: "List",
            len: "Int",
            swap: "List",
            and: "Boolean",

            list0: "List",
            list1: "List",
            list2: "List",
            list3: "List",

            "0": "Int",
            "1": "Int",
            "2": "Int",

            b0: "Boolean",
            b1: "Boolean",
            b2: "Boolean",
            b3: "Boolean",

            e0: "Element",
            e1: "Element",
            e2: "Element",
            e3: "Element",

            f0: "Function",

            any0: "*",
            any1: "*",
            any2: "*",
            any3: "*",
    ]

    static final Set vars = [
            "list0", "list1", "list2", "list3",
            "b0", "b1", "b2", "b3",
            "f0",
            "any0", "any1", "any2", "any3"
    ] as Set

    static final Set consts = [
            "0", "1", "2"
    ] as Set

    static final NodePrinter nodePrinter = new NodePrinter()

    static void main(String[] args) {
        // exprTexts -> exprs
        List<Node> eqs = []
        for (String exprText in exprTexts) {
            eqs << eqToNode(removeSpace(exprText).split("==").collect {
                convertExprListToNode(null, convertExprTextToList(it as String))
            })
        }

        Node target = eqs[eqs.size() - 1]
        List<Node> targets = [target]

        List<Node> newTargets
        for (int k = 0; k < 1; k++) {
            def newTargetCreated = false

            // if(A) { B } の B の中に A = true を入れる
            if (!newTargetCreated) {
                newTargets = findAndRemoveSameCondInIf(target)
                if (newTargets.size() > 0) {
                    println "if の条件を true/false に置換しました"
                    target = newTargets[0]
                    newTargetCreated = true
                }
            }

            // 木を複雑化する方の式変形の数
            int makeLargerEqCount = 2

            if (!newTargetCreated) {
                // 末尾の等式優先（木を複雑化しない式変形）
                for (int i = eqs.size() - 1; i >= makeLargerEqCount; i--) {
                    def eq = eqs[i]
                    newTargets = replaceByEq(target, eq.children()[0] as Node, eq.children()[1] as Node, true)
                    if (newTargets.size() > 0) {
                        println "下記の式を代入（木を複雑化しない式変形）"
                        println eq
                        target = newTargets[0]
                        newTargetCreated = true
                        break
                    }
                }
            }

            if (!newTargetCreated) {
                newTargets = findIfSwapIf(target);
                if (newTargets.size() > 0) {
                    println "if の親関数を if の中に入れました。"
                    target = newTargets[0]
                    newTargetCreated = true
                }
            }

            if (!newTargetCreated) {
                // 木を複雑化する方の式変形
                for (int i = makeLargerEqCount - 1; i >= 0; i--) {
                    def eq = eqs[i]
                    // TODO なぜ、isUseRootReplace = false が必要？
                    newTargets = replaceByEq(target, eq.children()[0] as Node, eq.children()[1] as Node, false)
                    if (newTargets.size() > 0) {
                        println "下記の式を代入（木を複雑化する方の式変形）"
                        println eq
                        target = newTargets[0]
                        newTargetCreated = true
                        break
                    }
                }
            }

            if (!newTargetCreated) {
                println "Cannot change more, k = $k"
                break;
            }

            targets = [target];
            println "変換結果"
            println targets

            // 矛盾チェック
            if (hasContradiction(targets)) {
                println "成功：背理法で矛盾を発見！式変形の回数は ${k + 1}回です。"
                break;
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
                thenTerm.replaceNode(constTerm)
                newTargets << target.clone()
                constTerm.replaceNode(thenTerm)
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

    static List<Node> findIfSwapIf(Node target) {
        def founds = target.depthFirst().findAll { Node n -> n.name() == "if" }.clone() as List<Node>
        def newTargets = []
        for (def found in founds) {
            def newTarget = swapIf(target, found)
            if (newTarget != null) {
                newTargets << newTarget
            }
        }
        return newTargets
    }

    /** if と その親の関数を交換 */
    static Node swapIf(Node target, Node ifTerm) {
        def parent = ifTerm.parent() as Node

        // ifの親がifでないことが条件
        if (parent == null || parent.name() == "if") return null

        def ifTermIdx = ifTerm.parent().children().indexOf(ifTerm)
        def origIfTermChild1 = ifTerm.children()[1] as Node
        def origIfTermChild2 = ifTerm.children()[2] as Node

        // parent clone を作る
        def parent1 = parent.clone() as Node
        def parent2 = parent.clone() as Node

        // parent - ifTerm - parent clone - ifTermの子 という状態を作る
        parent1.children()[ifTermIdx] = origIfTermChild1.clone()
        parent2.children()[ifTermIdx] = origIfTermChild2.clone()
        origIfTermChild1.replaceNode(parent1)
        origIfTermChild2.replaceNode(parent2)

        // クローンして戻り値を作成
        def ifTermClone = ifTerm.clone() as Node
        ifTermClone.attributes()["type"] = parent.attribute("type")
        def resultTarget
        if (target.is(parent)) {
            resultTarget = ifTermClone
        } else {
            // parentの親 - ifTerm という状態を作り、間の parent を抜く
            parent.replaceNode(ifTermClone)
            resultTarget = target.clone()
            ifTermClone.replaceNode(parent)
        }

        // 元に戻す。やることは ifTerm - parent clone - ifTermの子 の parent clone を除去
        parent1.replaceNode(origIfTermChild1)
        parent2.replaceNode(origIfTermChild2)

        return resultTarget as Node
    }

    static boolean isAllTrue(List<Closure> list, def node) {
        for (def closure in list) {
            if (!closure.call(node)) return false
        }
        return true
    }

    /** Node を検索条件に変換する。検索条件は全て true でないといけない。 */
    static List<Closure> convertTermToSearchCond(Node term, int pos = -1) {
        def name = term.name()
        def type = term.attribute("type")

        def conds = []
        if (isVar(name)) {
            if (pos >= 0) {
                conds << { Node node -> position(node) == (pos + 1) }
            }
            if (type != "*") {
                conds << { Node node -> node.attribute("type") == type }
            }
        } else if (isConst(name)) {
            conds << { Node node -> node.attribute("name") == name }
            conds << { Node node -> node.attribute("type") == type }
            if (pos >= 0) {
                conds << { Node node -> position(node) == (pos + 1) }
            }
        } else { // 関数
            conds << { Node node -> node.attribute("name") == name }
            if (pos >= 0) {
                conds << { Node node -> position(node) == (pos + 1) }
            }
        }

        def children = term.children();
        for (int i = 0; i < children.size(); i++) {
            def childConds = convertTermToSearchCond(children[i] as Node, i)
            conds << { Node node -> isAllTrue(childConds, node.children()[i]) }
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
    static List<Node> replaceByEq(Node target, Node fromTerm, Node toTerm, boolean isUseRootReplace) {
        // fromTerm を 条件群 に変換する
//        def fromXpath = (isUseRootReplace ? "*/..//" : "*//") + convertFromTerm2XPath(fromTerm);
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
            found.replaceNode(newTerm)
            // クローンして追加
            eqs << target.clone()
            // 元に戻す
            newTerm.replaceNode(found)
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

    static int position(Node node) { node.parent().children().indexOf(node) }

    static boolean isVar(s) { s in vars }

    static boolean isConst(s) { s in consts }

    // -----------------------------------------------------------------------------------------

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
        Node node = new Node(parent, list[0] as String, [type: typeMap[list[0]]])
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
}
