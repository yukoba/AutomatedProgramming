var fromA = [
    {node: ["int", "func", "f"], children: [
        {node: ["int", "var", "x"]}
    ]}
];
var toA = [
    {node: ["int", "var", "x"]}
];

var eqA = {
    left: {node: ["int", "var", "x"]},
    right: {node: ["int", "func", "f"], children:[
        {node: ["int", "func", "f"], children: [
            {node: ["int", "var", "x"]}
        ]}
    ]}
};

function replaceA(eqWhole, from, to, eq) {
    if (isMatch(eq, from)) {
        // eqWhole のうち eq の場所を from -> to の置換をしたやつの木全体のクローン
        replaceTree(eqWhole, eq, replaceTo(eq, from, to));
    }

    if (eq.children != null) {
        for (var i = 0; i < eq.children.length; i++) {
            replaceA(eqWhole, from, to, eq.children[i]);
        }
    }
}

function isMatch(eq1, eq2) {
    
}




var from1 = /int:func:f\((int:\w+:\w+\([^\)]*)\)/g;
var to1 = "$1";

var from2 = /(int:\w+:\w+\([^\)]*\))/g;
var to2 = "int:func:f($1)";

var eq = "int:var:x()=int:func:f(int:func:f(int:var:x()))";

function replace(eq, from, to) {
    var replaceds = [];
    var ary;
    while ((ary = from.exec(eq)) != null) {
        console.log(ary.index, from.lastIndex, ary[0]);

        var replaced = eq.substring(0, ary.index) + to.replace(/\$1/g, ary[1]) + eq.substring(from.lastIndex);
        console.log("->", replaced);
        replaceds.push(replaced);

        from.lastIndex -= ary[0].length - 1;
    }
    return uniqueArray(replaceds);
}

var replaceds = replace(eq, from1, to1);
//var replaceds = replace(eq, from2, to2);
for (var i = 0; i < replaceds.length; i++) {
    console.log(i, replaceds[i]);
}

function eq2regs(eq) {
    var ary = eq.split("=");
    var from = ary[0], to = ary[1];

    return {from:from, to:to};
}
