$(document).ready(function () {
    $("#tabs").tabs({
        beforeLoad: function (event, ui) {
            ui.panel.html('<img src="images/ajax-loader.gif" width="24" height="24" style="vertical-align:middle;">Loading..</img>');
        },
        load: function (event, ui) {
            var filterList = document.getElementsByClassName("filters");
            var tab = $("#tabs").tabs("option", "active");
            for (var i = 0; i < filterList.length; i++) {
                if (filterList.item(i).getAttribute("href").indexOf("tab=") == -1) {
                    filterList.item(i).setAttribute("href", filterList.item(i).getAttribute("href") + "&tab=" + tab);
                } else {
                    filterList.item(i).setAttribute("href", filterList.item(i).getAttribute("href").replace(/&tab=\d+/, "&tab=" + tab));
                }
            }
            if (document.getElementById("filter-tab") != null) {
                document.getElementById("filter-tab").setAttribute("value", tab);
            }
            
            // initialise tabs triboxes
            var selectedPanel = $("#tabs div.ui-tabs-panel:not(.ui-tabs-hide)");
            initTribox(selectedPanel);
            selectedPanel.find(".shift-click-box").shiftSelectable();
        },
        cache: true
    });
});

function updateDiv(divToChange, key, pub, pubImage) {
    key = encodeKey(key);
    var rowTxt, cellTxt, x, i, xmlhttp, crawlError, pollError;
    var headerArray = ["Plugin", "Access Type", "Content Size", "Disk Usage (MB)",
        "Repository"];
    if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
        xmlhttp = new XMLHttpRequest();
    } else {// code for IE6, IE5
        xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
    }
    xmlhttp.onreadystatechange = function () {
        if (xmlhttp.readyState == 4 && xmlhttp.status == 200) {
            xmlDoc = xmlhttp.responseXML;
            rowTxt = "<a href=\"javascript:closeDiv('" + divToChange + "', '" +
                key + "', '" + pub + "', '" + pubImage + "')\"><img id =\"" +
                pubImage + "\" class=\"title-icon\" src=\"images/expanded.gif\">"
                + divToChange.replace(/_/g, " ") + "</a>";
            cellTxt = "<br /><table class=\"detail-table\"><tbody>";
            cellTxt += "<div class='au-actions'>[ <a class=\"au-link\"\n\
     href=\"DaemonStatus?table=ArchivalUnitTable&key=" + key + "\" \n\
target=\"_blank\">View Detailed AU information</a><sup><img src=\"images/external_link.png\"/></sup> ]</div>";
            x = xmlDoc.getElementsByTagNameNS("http://lockss.org/statusui", "summaryinfo");
            for (i = 0; i < x.length; i++) {
                var title = x[i].getElementsByTagNameNS("http://lockss.org/statusui",
                    "title")[0].textContent;
                var value = x[i].getElementsByTagNameNS("http://lockss.org/statusui",
                    "value")[0].textContent;
                if (title != "" && headerArray.indexOf(title) != -1) {
                    cellTxt += "<tr><td class=\"title-cell\">" + title
                        + "</td><td class=\"value-cell\">" + value + "</td></tr>";
                }
                if (getCrawlError(x, i, title) != null) {
                    crawlError = getCrawlError(x, i, title);
                }
                if (getPollError(x, i, title) != null) {
                    pollError = getPollError(x, i, title);
                }
            }
            if (crawlError != null) {
                cellTxt += "<tr><td colspan='2'><div class=\"au-error\">Last Crawl Result - " + crawlError
                    + " <a class=\"error-info\" href=\"javascript:void(0);\" title=\"Error\"><img id=\"" + divToChange
                    + "_error\" src=\"images/button_info.png\" /></a></div></td></tr>";
            }
            if (pollError != null) {
                cellTxt += "<tr><td colspan='2'><div class=\"au-error\">Last Poll Result - " + pollError
                    + " <img src=\"images/button_info.png\" /></div></td></tr>";
            }
            cellTxt += "</tbody></table>";
            cellTxt += "<div class='au-actions'><form method=\"POST\" \n\
action=\"/DisplayContentStatus\" class=\"remove-au-form\">\n\
<input name=\"lockssAction\" type=\"hidden\"><input name=\"removeAu\" value="
                + key + " type=\"hidden\">[ <input name=\"\" class=\"remove-au-button\" value=\"Remove AU\" \n\
type=\"button\" id=\"Ab_Imperio_Volume_2011\" onClick=\"if(confirm('Confirm deletion of AU: "
                + divToChange.replace(/_/g, " ") + "'))lockssButton(this, 'DoRemoveAus')\"> ] " +
                "<a class=\"remove-info\" href=\"javascript:void(0);\" title=\"Removes this AU\"><img id=\"" +
                divToChange + "_info\" src=\"images/button_info.png\" /></a></form></div>";
            document.getElementById(divToChange + "_au_title").innerHTML = rowTxt;
            var rowId = divToChange + "_cell";
            document.getElementById(rowId).innerHTML = cellTxt;
            document.getElementById(divToChange + "_row").className =
                document.getElementById(divToChange + "_row").className.replace(/\bhide-row\b/, '');
            $(".remove-info").tooltip({position: {my: "center bottom-20", at: "center top", of: "#" + divToChange + "_info",
                using: function (position, feedback) {
                    $(this).css(position);
                    $("<div>")
                        .addClass("arrow").addClass(feedback.vertical).addClass(feedback.horizontal).appendTo(this);
                }}});
            $(".error-info").tooltip({position: {my: "center bottom-20", at: "center top", of: "#" + divToChange + "_error",
                using: function (position, feedback) {
                    $(this).css(position);
                    $("<div>")
                        .addClass("arrow").addClass(feedback.vertical).addClass(feedback.horizontal).appendTo(this);
                }}});
        }
    }
    xmlhttp.open("GET", "DaemonStatus?table=ArchivalUnitTable&key=" + key
        + "&output=xml", true);
    xmlhttp.send();
}

function getCrawlError(x, i, title) {
    var crawlErrorArray = ["Can't fetch permission page", "Fetch error", "Interrupted by daemon exit", "No permission from publisher"];
    if (title == "Last Crawl Result") {
        var value = x[i].getElementsByTagNameNS("http://lockss.org/statusui", "value")[0].textContent;
        if (value != "" && crawlErrorArray.indexOf(value) != -1) {
            return value;
        }
    }
    return null;
}

function getPollError(x, i, title) {
    var pollErrorArray = ["Error", "Aborted"];
    if (title == "Last Poll Result") {
        var value = x[i].getElementsByTagNameNS("http://lockss.org/statusui", "value")[0].textContent;
        if (value != "" && pollErrorArray.indexOf(value) != -1) {
            return value;
        }
    }
    return null;
}

function closeDiv(divToClose, key, pub, pubImage) {
    key = encodeKey(key);
    var txt = "<a href=\"javascript:updateDiv('" + divToClose + "', '" + key
        + "', '" + pub + "', '" + pubImage + "')\"><img id =\"" + pubImage
        + "\" class=\"title-icon\" src=\"images/extend-right.gif\"/>"
        + divToClose.replace(/_/g, " ") + "</a>";
    if (document.getElementById(divToClose + "_row").className.indexOf("hide-row") == -1) {
        document.getElementById(divToClose + "_row").setAttribute("class",
            document.getElementById(divToClose + "_row").className.trim() + " hide-row");
    }
    document.getElementById(divToClose + "_au_title").innerHTML = txt;
}

function hideRows(styleClass, hrefId, pubImage, pub) {
    $('.' + styleClass).addClass('hide-row');
    var auList = document.getElementsByClassName(pub + "-au");
    for (var i = 0; i < auList.length; i++) {
        if (auList.item(i).className.indexOf("hide-row") == -1) {
            auList.item(i).setAttribute("class",
                auList.item(i).className.trim() + " hide-row");
        }
        if (document.getElementById(auList.item(i).id.replace("row", "au_title").replace(",", "")) != null) {
            var elementList = document.getElementById(auList.item(i).id.replace("row", "au_title").replace(",", "")).children;
            for (var j = 0; j < elementList.length; j++) {
                if (elementList[j] != null) {
                    elementList[j].setAttribute("href", elementList[j].getAttribute("href").replace("closeDiv", "updateDiv"));
                    var imgList = elementList[j].children;
                    for (var k = 0; k < imgList.length; k++) {
                        if (imgList[k] != null && imgList[k].getAttribute("src") != null) {
                            imgList[k].setAttribute("src", "images/extend-right.gif");
                        }
                    }
                }
            }
        }
    }
    document.getElementById(hrefId).href = "javascript:showRows('" + styleClass
        + "', '" + hrefId + "', '" + pubImage + "', '" + pub + "')";
    document.getElementById(pubImage).src = "images/control_add.png";
}

function showRows(styleClass, hrefId, pubImage, pub) {
    $('.' + styleClass).removeClass('hide-row');
    document.getElementById(hrefId).href = "javascript:hideRows('" + styleClass
        + "', '" + hrefId + "', '" + pubImage + "', '" + pub + "')";
    document.getElementById(pubImage).src = "images/control_remove.png";
}

function encodeKey(key) {
    return encodeURI(key).replace(/&/g, "%26").replace(/~/g, "%7E");
}

function titleCheckbox(checkbox, titleString) {
    var check = false;
    if (checkbox.checked) {
        check = true;
    }
    var checkboxList = document.getElementsByClassName(titleString + "_chkbox");
    for (var i = 0; i < checkboxList.length; i++) {
        checkboxList[i].checked = check;
    }
}