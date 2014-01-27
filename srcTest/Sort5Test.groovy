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
    void testFillVar2node() {
        def expr1 = "tail(tail(list0))"
        def expr2 = "tail(any0)"
        def node1 = convertExpr(expr1)
        def node2 = convertExpr(expr2)

        def var2node = new HashMap()
        def isOK = fillVar2node(var2node, node2, node1)
        println "isOK = $isOK"
        var2node.each { key, value -> print "$key: "; println value }

        assertEquals true, isOK
        assertEquals 1, var2node.size()
        assertEquals """tail(type:'List') {
  list0(type:'List')
}
""", node2string(var2node.any0)
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

        def node4 = fillIfType(replaceVar(node3, var2node))
        print "node4 = "
        println node4
    }

    @Test
    void testConvertTermToSearchCond2() {
        def expr1 = "f0(if(b0, any1, any2))"
        def expr2 = "isSorted(if(TRUE, list0, list1))"
        def expr3 = "if(b0, f0(any1), f0(any2))"
        def node1 = convertExpr(expr1)
        def node2 = convertExpr(expr2)
        def node3 = convertExpr(expr3)

        print "node1 = "; println node1
        print "node2 = "; println node2
        print "node3 = "; println node3

        println "--------------------------------------"
        def var2node = new HashMap()
        def isOK = fillVar2node(var2node, node1, node2)
        println "isOK = $isOK"
        var2node.each { key, value -> print "$key: "; println value }

        println "--------------------------------------"
        def node4 = fillIfType(replaceVar(node3, var2node))
        print "replaceVar = "
        println node4
    }
}
