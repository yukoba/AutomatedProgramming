import groovy.transform.TypeChecked

@TypeChecked
class Sort5 {
    static List exprTexts = [
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

    static Map typeMap = [
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

    static final NodePrinter nodePrinter = new NodePrinter()

    static void main(String[] args) {
        new Sort5().test()
    }

    void test() {
        // exprTexts -> exprs
        List eqs = []
        for (String exprText in exprTexts) {
            eqs.add(removeSpace(exprText).split("==").collect {
                convertExprListToNode(null, convertExprTextToList(it as String))
            })
        }

        List target = eqs[eqs.size() - 1] as List
        List targets = [target]

        for (int k = 0; k < 50; k++) {
        }
    }

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
}
