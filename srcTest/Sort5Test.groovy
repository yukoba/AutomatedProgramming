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

    @Test
    void testFindAndRemoveSameCondInIf() {
        def expr = "if(b1, b1, FALSE)"
        def node = convertExpr(expr)
        println node

        def newTargets = findAndRemoveSameCondInIf(node)
        for (def target in newTargets) {
            println target
        }
    }

    @Test
    void testConvertTermToSearchCond() {
        def expr1 = "tail(list0)"
        def expr2 = "len(tail(list0))"
        def node1 = convertExpr(expr1)
        def node2 = convertExpr(expr2)

        def searchCond = convertTermToSearchCond(node1)
        def founds = node2.depthFirst().findAll { isAllTrue(searchCond, it) } as List<Node>
        println "[Result]"
        for (def found in founds) {
            println found
        }
    }

    @Test
    void testFillVar2node() {
        def expr1 = "tail(tail(list0))"
        def expr2 = "tail(any0)"
        def node1 = convertExpr(expr1)
        def node2 = convertExpr(expr2)

        def var2node = new HashMap()
        def isOK = fillVar2node(var2node, node2, node1)
        println "isOK = $isOK"
        var2node.each { key, value -> print "$key: "; println value }
    }

    @Test
    void testReplaceVar() {
        def expr1 = "tail(tail(list0))"
        def expr2 = "tail(any0)"
        def expr3 = "len(any0)"
        def node1 = convertExpr(expr1)
        def node2 = convertExpr(expr2)
        def node3 = convertExpr(expr3)

        def var2node = new HashMap()
        def isOK = fillVar2node(var2node, node2, node1)
        println "isOK = $isOK"
        var2node.each { key, value -> print "$key: "; println value }

        def node4 = replaceVar(node3, var2node)
        print "node4 = "
        println node4
    }
}
