import groovy.transform.TypeChecked

@TypeChecked
class Sort5Exprs {
    /** 証明対象 */
    static final String targetText = "isSorted(sortInner(list0)) == TRUE"

    static final List exprTexts = [
            // 公式・定義・公理
            "and(TRUE, b0) == b0",
            "and(b0, TRUE) == b0",

            "if(b0, any0, any0) == any0",
            "if(FALSE, any0, any1) == any1",
            "if(TRUE, any0, any1) == any0",

            "lt(len(append(e0, list0)), 2) == FALSE",

            "head(append(e0, list0)) == e0",
            "tail(append(e0, list0)) == list0",

            "tail(tail(swap(list0))) == tail(tail(list0))",
            "head(tail(swap(list0))) == head(list0)",

            "isSorted(sortInner(tail(swap(list0)))) == TRUE",
            "isSorted(sortInner(tail(list0))) == TRUE",
            "lt(head(tail(tail(list0))), head(tail(list0))) == TRUE",
            "isSorted(tail(list0)) == TRUE",

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
                    "append(head(tail(list0)), sortInner(tail(swap(list0))))))",
    ]

    static final Map typeMap = [
            sortInner: "List",
            isSorted: "Boolean",

            if: "*",
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

            f0: "*",

            any0: "*",
            any1: "*",
            any2: "*",
            any3: "*",
    ]

    static final Set vars = [
            "list0", "list1", "list2", "list3",
            "b0", "b1", "b2", "b3",
            "e0", "e1", "e2", "e3",
            "f0",
            "any0", "any1", "any2", "any3"
    ] as Set

    static final Set consts = [
            "0", "1", "2"
    ] as Set
}
