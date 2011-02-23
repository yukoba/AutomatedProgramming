var stdout = null;

function println(str) {
    for (var i = 0; i < arguments.length; i++) {
        print(arguments[i]);
    }
    print("\n");
}

function print(strs) {
    if (stdout == null) {
        stdout = document.createElement("div");
        document.body.appendChild(stdout);
    }

    for (var i = 0; i < arguments.length; i++) {
        var str = arguments[i];

        if (str === void 0) {
            str = "undefined";
        } else if (str == null || str.constructor !== String) {
            str = JSON.stringify(str);
        }

        str = str.replace(/</g, "&lt;").replace(/>/g, "&gt;")
                .replace(/\n/g, "<br>").replace(/\t/g, "&nbsp;&nbsp;&nbsp;&nbsp;");
        stdout.innerHTML += str + "\n";
    }
}
