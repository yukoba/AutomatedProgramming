
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

            0: "Int",
            1: "Int",
            2: "Int",

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

    static void main(String[] args) {
        new Sort5().test()
    }

    void test() {
        // exprTexts -> exprs
        List eqs = []
        for (String exprText in exprTexts) {
            eqs.add(removeSpace(exprText).split("==").collect{parseFunc(it as String)})
        }

        def target = eqs[eqs.size() - 1]
        List targets = [target]

        for (int k = 0; k < 50; k++) {

        }

    }

    static String removeSpace(String s) { s.replaceAll(' ', '') }

    static def parseFunc(String s) {
        List list = []
        int start = s.indexOf('(')
        if (start == -1) return s
        int end = s.lastIndexOf(')')
        list.add(s[0..(start-1)])

        int bracketCount = 0
        StringBuilder sb = new StringBuilder()
        for (int i = start + 1; i < end; i++) {
            switch (s[i]) {
                case "(": bracketCount++; sb.append(s[i]); break;
                case ")": bracketCount--; sb.append(s[i]); break;
                case ",":
                    if (bracketCount == 0) {
                        list.add(parseFunc(sb.toString()))
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
            list.add(parseFunc(sb.toString()))
        return list
    }

    static def replaceNode(expr, from, to, replaced = [false]) {
        if (replaced[0])
            return expr
        if (expr == from) {
            replaced[0] = true
            return to
        }
        switch (expr) {
            case String:
                return expr
            case List:
                int idx = 0
                return expr.collect { idx++ == 0 ? it : replaceNode(it, from, to, replaced) }
            default:
                return null
        }
    }
}
