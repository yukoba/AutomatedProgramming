var xmlSerializer = new XMLSerializer();

function println(str) {
    for (var i = 0; i < arguments.length; i++) {
        print(arguments[i]);
    }
    print("\n");
}

function print(strs) {
    var stdout = document.createElement("div");
    document.body.appendChild(stdout);

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

function printHeader(html) {
    printHtml("<h1>" + html + "</h1>");
}

function printHtml(html) {
    var stdout = document.createElement("div");
    document.body.appendChild(stdout);
    stdout.innerHTML = html;
}

function printXml(dom) {
    var str = formatXml(xmlSerializer.serializeToString(dom));

    var stdout = document.createElement("pre");
    stdout.setAttribute("class", "prettyprint lang-xml");
    document.body.appendChild(stdout);

    $(stdout).text(str);
}

/**
 * const, var タグだけ独別扱いした XML 整形
 * http://stackoverflow.com/questions/376373/pretty-printing-xml-with-javascript より変形
 */
function formatXml(xml) {
    var formatted = '';
    var reg = /(>)\s*(<)(\/*)/g;
    xml = xml.replace(reg, '$1\n$2$3');
    var pad = 0;
    $.each(xml.split('\n'), function(index, node) {
        // 超強引な特例処理！
        if (node == "</const>" || node == "</var>") {
            formatted = formatted.substring(0, formatted.length - 2) + "/>\n";
            if (pad > 0) pad--;
            return;
        }

        // 通常処理
        var indent = 0;
        if (node.match( /.+<\/\w[^>]*>$/ )) {
            indent = 0;
        } else if (node.match( /^<\/\w/ )) {
            if (pad != 0) {
                pad -= 1;
            }
        } else if (node.match( /^<\w[^>]*[^\/]>.*$/ )) {
            indent = 1;
        } else {
            indent = 0;
        }

        var padding = '';
        for (var i = 0; i < pad; i++) {
            padding += '  ';
        }

        formatted += padding + node + '\n';
        pad += indent;
    });

    return formatted;
}
