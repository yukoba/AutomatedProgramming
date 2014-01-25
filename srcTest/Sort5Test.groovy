

import org.junit.Test
import static org.junit.Assert.*

class Sort5Test {
	@Test void test() {
        def nb = new NodeBuilder()

        def n1 = nb.ab(a:1) { cd() }
        println "n1 = $n1"

        //def n2 = nb.ab { cd=1 }
//        def n2 = {foo a=1 b="abc"} as Node
//        println n2

        def e1 = "A.tail.head == A.swap.head"
        def e3 = "head(tail(A)) == head(swap(A))"
        def e2 = "f( if(a, b, c) ) == if(a, f(b), f(c))"
//        def e4 = "if(TRUE, a, b) == a"
        def e4 = "if(TRUE, a(c,d), b) == a"

        def e4_2 = removeSpace(e4).split("==")
        def e4_3 = parseFunc(e4_2[0])
        println "e4_2 = $e4_2"
        println "e4_3 = $e4_3"

        println replaceNode(e4_3, ["a", "c", "d"], "e");
        println replaceNode(["a", ["a", ["c", "d"]], ["a", ["c", "d"]]], ["a", ["c", "d"]], "e");
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

    static def replaceNode(expr, from, to, List replaced = [false]) {
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
