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
    printHtml("<h1>" + html + "</h1>", null);
}

var nextTableIdNo = 0;

function createLeftRightPanel() {
    var table = document.createElement("table");
    var tbody = document.createElement("tbody");
    var tr = document.createElement("tr");
    var td1 = document.createElement("td");
    var td2 = document.createElement("td");

    if (displayBlockNo == nextTableIdNo)
        table.setAttribute("style", "width: 100%;");
    else
        table.setAttribute("style", "width: 100%; display: none;");
    table.setAttribute("id", "table" + (nextTableIdNo++));
    td1.setAttribute("style", "width: 50%");
    td2.setAttribute("style", "width: 50%");
    tr.setAttribute("valign", "top");

    table.appendChild(tbody);
    tbody.appendChild(tr);
    tr.appendChild(td1);
    tr.appendChild(td2);
    document.body.appendChild(table);

    return [td1, td2];
}

function appendHtml(html, parentDom) {
    (parentDom ? parentDom : document.body).innerHTML += html;
}

function printHtml(html, parentDom) {
    var stdout = document.createElement("div");
    (parentDom ? parentDom : document.body).appendChild(stdout);
    stdout.innerHTML = html;
}

function printXml(dom, parentDom) {
    var str = formatXml(xmlSerializer.serializeToString(dom));

    var stdout = document.createElement("pre");
//    stdout.setAttribute("class", "prettyprint linenums lang-xml");
    stdout.setAttribute("class", "prettyprint lang-xml");
    (parentDom ? parentDom : document.body).appendChild(stdout);

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

var displayBlockNo = 0;

function addButtonEventHandler() {
    $("#backButton").click(function() {
        $("#table" + displayBlockNo).css("display", "none");
        displayBlockNo = Math.max(0, displayBlockNo - 1);
        $("#table" + displayBlockNo).css("display", "");
    });

    $("#nextButton").click(function() {
        $("#table" + displayBlockNo).css("display", "none");
        displayBlockNo = Math.max(0, displayBlockNo + 1);
        $("#table" + displayBlockNo).css("display", "");
    });
}
