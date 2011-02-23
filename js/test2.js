function Eq(x1, x2) {
    this.x1 = x1;
    this.x2 = x2;
    this.toString = function() {
        return "Eq(" + this.x1 + "," + this.x2 + ")";
    };
    this.toHashKey = function() {
        return "Eq(" + x1.toHashKey() + "," + x2.toHashKey() + ")";
    };
    this.getChildren = function() {
        return [x1, x2];
    };
}

function Var(name) {
    this.name = name;
    this.toString = function() {
        return "Var(" + this.name + ")";
    };
    this.toHashKey = function() {
        return "Var";
    };
    this.getChildren = function() {
        return [];
    };
}

function Func(name) {
    this.name = name;
    this.args = Array.prototype.slice.call(arguments, 1);
    this.toString = function() {
        return name + "(" + this.args.join(",") + ")";
    };
    this.toHashKey = function() {
        return name + "(" + this.args.map(function(x) {
            return x.toHashKey();
        }).join(",") + ")";
    };
    this.getChildren = function() {
        return this.args;
    };
}

function Not(value) {
    this.value = value;
    this.toString = function() {
        return "Not(" + this.value + ")";
    };
    this.toHashKey = function() {
        return "Not(" + value.toHashKey() + ")";
    };
    this.getChildren = function() {
        return [value];
    };
}

//var fucts = [
//        ["eq", ["var", "x"], ["f", [["var", "x"]]]],
//        ["not", ["eq", ["var", "x"],
//            ["f", [["f", [["var", "x"]]]]]
//        ]]
//];

var facts = [
        new Eq(new Var("x"), new Func("f", new Var("x"))),
        new Not(new Eq(new Var("x"),
                new Func("f", new Func("f", new Var("x")))))
];

var eqs = createEqs();
function createEqs() {
    var eqs = {};
    for (var i = 0; i < facts.length; i++) {
        var fact = facts[i];
        if (fact instanceof Eq) {
            function add(key, value) {
                addToArrayHashTable(eqs, key.toHashKey(), value);
            }
            add(fact.x1, fact.x2);
            add(fact.x2, fact.x1);
        }
    }
    return eqs;
}

//console.log(eqs);

/** logic が eqs の中で使われている場所を探す */
function findInEqs(eqs, logic) {
    var key = logic.toHashKey();
    if (key in eqs) {
//        console.log(logic);
    }
    logic.getChildren().forEach(function(child) {
        findInEqs(eqs, child);
    });
}
findInEqs(eqs, facts[1]);
