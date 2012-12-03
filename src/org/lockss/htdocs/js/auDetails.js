$(document).ready(function(){ $("#tabs").tabs(); });

$( ".selector" ).tabs({ selected: 0 });

function updateDiv(divToChange, key, pubImage) {
    var txt, x, i, elements, xmlhttp;
    if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
        xmlhttp = new XMLHttpRequest();
    } else {// code for IE6, IE5
        xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
    }
    xmlhttp.onreadystatechange = function () {
        if (xmlhttp.readyState == 4 && xmlhttp.status == 200) {
            xmlDoc = xmlhttp.responseXML;
            txt = "<a href=\"\" onClick=\"closeDiv('" + divToChange + "', '" + key + "', '" + pubImage + "');return false\"><img id =\"" + pubImage + "\" class=\"title-icon\" src=\"images/collapse.png\">" + divToChange.replace(/_/g, " ") + "</a>";
            txt += "<br /><br /><div><a href=\"DaemonStatus?table=ArchivalUnitTable&key=" + key + "\">Au details</a></div><div><a href=\"ServeContent?auid=" + key + "\">Serve content</a></div>";
            txt += "<br /><table class=\"detail-table\">";
            x = xmlDoc.getElementsByTagNameNS("http://lockss.org/statusui", "summaryinfo");
            for (i = 0; i < x.length; i++) {
                var title = x[i].getElementsByTagNameNS("http://lockss.org/statusui", "title")[0].textContent;
                var value = x[i].getElementsByTagNameNS("http://lockss.org/statusui", "value")[0].textContent;
                if (title != "") {
                    txt += "<tr><td class=\"title-cell\">" + title + "</td><td class=\"value-cell\">" + value + "</td></tr>";
                }
            }
            txt += "</table><br />"; 
            document.getElementById(divToChange).innerHTML = txt;
        }
    }
    xmlhttp.open("GET","DaemonStatus?table=ArchivalUnitTable&key=" + key + "&output=xml",true);
    xmlhttp.send();
}

function closeDiv(divToClose, key, pubImage) {
    var txt;
    txt = "<a href=\"\" onClick=\"updateDiv('" + divToClose + "', '" + key + "');return false\"><img id =\"" + pubImage + "\" class=\"title-icon\" src=\"images/expand.png\"/>" + divToClose.replace(/_/g, " ") + "</a>";
    document.getElementById(divToClose).innerHTML = txt;
}

function hideRows(styleClass, hrefId, pubImage) {
    $('.' + styleClass).addClass('hide-row');
    document.getElementById(hrefId).href = "javascript:showRows('" + styleClass + "', '" + hrefId + "', '" + pubImage + "')";
    document.getElementById(pubImage).src = "images/expand.png";
} 

function showRows(styleClass, hrefId, pubImage) {
  $('.' + styleClass).removeClass('hide-row');
    document.getElementById(hrefId).href = "javascript:hideRows('" + styleClass + "', '" + hrefId + "', '" + pubImage + "')";
    document.getElementById(pubImage).src = "images/collapse.png";
}