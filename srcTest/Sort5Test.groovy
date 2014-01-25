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
    void test() {
//        def e1 = "A.tail.head == A.swap.head"
//        def e3 = "head(tail(A)) == head(swap(A))"
//        def e2 = "f( if(a, b, c) ) == if(a, f(b), f(c))"
//        def e4 = "if(TRUE, a, b) == a"
        def e4 = "if(TRUE, a(c,d), b) == a"

        def e4_2 = removeSpace(e4).split("==")
        def e4_3 = parseFunc(e4_2[0])
        println "e4_2 = $e4_2"
        println "e4_3 = $e4_3"

        println replaceNode(e4_3, ["a", "c", "d"], "e");
        println replaceNode(["a", ["a", ["c", "d"]], ["a", ["c", "d"]]], ["a", ["c", "d"]], "e");
    }
}
