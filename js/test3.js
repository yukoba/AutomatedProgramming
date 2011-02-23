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
