import org.junit.Test

import static Sort5.*
import static org.junit.Assert.*

class Sort5Test {
    @Test
    void testNodeBuilder() {
        def nb = new NodeBuilder()

        def n1 = nb.ab(a: 1) { cd() }
        println "n1 = $n1"

//        def n2 = nb.ab { cd=1 }
//        def n2 = {foo a=1 b="abc"} as Node
//        println "n2 = $n2"
    }

    @Test
    void testConvertExpr() {
//        def e1 = "head(tail(A)) == head(swap(A))"
//        def e2 = "f( if(a, b, c) ) == if(a, f(b), f(c))"
//        def e3 = "if(TRUE, a, b) == a"
//        def e4 = "if(TRUE, a(c,d), b) == a"
//        def e5 = "isSorted(tail(list0)) == TRUE"
        def e6 = "lt(len(append(e0, list0)), 2) == TRUE"

        def ary = removeSpace(e6).split("==")
        def left = convertExprTextToList(ary[0])
        def node = convertExprListToNode(null, left)
        println "ary  = $ary"
        println "left = $left"
        println "node = "
        println node
    }
}
