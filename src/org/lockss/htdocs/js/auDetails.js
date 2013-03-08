$(document).ready(function(){
  $("#tabs").tabs();
});

$( ".selector" ).tabs({
  selected: 0
});

function updateDiv(divToChange, key, pubImage) {
  var txt, x, i, xmlhttp;
  var headerArray = ["Plugin", "Access Type", "Content Size", "Disk Usage (MB)",
  "Repository", "Status", "Publisher", "Available From Publisher", "Created",
  "Crawl Pool", "Last Completed Crawl", "Last Crawl", "Last Crawl Result",
  "Last Completed Poll", "Last Poll", "Last Poll Result"];
  if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
    xmlhttp = new XMLHttpRequest();
  } else {// code for IE6, IE5
    xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
  }
  xmlhttp.onreadystatechange = function () {
    if (xmlhttp.readyState == 4 && xmlhttp.status == 200) {
      xmlDoc = xmlhttp.responseXML;
      txt = "<a href=\"\" onClick=\"closeDiv('" + divToChange + "', '" +
      key + "', '" + pubImage + "');return false\"><img id =\"" + 
      pubImage + "\" class=\"title-icon\" src=\"images/collapse.png\">" 
      + divToChange.replace(/_/g, " ") + "</a>";
      txt += "<br /><br /><div><a class=\"au-details-link\"\n\
     href=\"DaemonStatus?table=ArchivalUnitTable&key=" + key + "\" \n\
target=\"_blank\">Au details</a><form method=\"POST\" method=\"POST\" \n\
action=\"/DisplayContentStatus\" class=\"remove-au-button\">\n\
<input name=\"lockssAction\" type=\"hidden\"><input name=\"removeAu\" value=" 
      + key + " type=\"hidden\"><input name=\"\" value=\"Remove AU\" \n\
type=\"button\" id=\"Ab_Imperio_Volume_2011\" onClick=\"if(confirm('Confirm deletion of "
      + unescape(unescape(key)) + "'))lockssButton(this, 'DoRemoveAus')\"></form></div>";
      txt += "<br /><table class=\"detail-table\"><tbody>";
      x = xmlDoc.getElementsByTagNameNS("http://lockss.org/statusui", "summaryinfo");
      for (i = 0; i < x.length; i++) {
        var title = x[i].getElementsByTagNameNS("http://lockss.org/statusui", 
        "title")[0].textContent;
        var value = x[i].getElementsByTagNameNS("http://lockss.org/statusui", 
        "value")[0].textContent;
        if (title != "" && headerArray.indexOf(title) != -1) {
          txt += "<tr><td class=\"title-cell\">" + title 
            + "</td><td class=\"value-cell\">" + value + "</td></tr>";
        }
      }
      txt += "</tbody></table><br />"; 
      document.getElementById(divToChange).innerHTML = txt;
    }
  }
  xmlhttp.open("GET","DaemonStatus?table=ArchivalUnitTable&key=" + key 
    + "&output=xml",true);
  xmlhttp.send();
}

function closeDiv(divToClose, key, pubImage) {
  var txt;
  txt = "<a href=\"\" onClick=\"updateDiv('" + divToClose + "', '" + key 
    + "');return false\"><img id =\"" + pubImage 
    + "\" class=\"title-icon\" src=\"images/expand.png\"/>" 
    + divToClose.replace(/_/g, " ") + "</a>";
  document.getElementById(divToClose).innerHTML = txt;
}

function hideRows(styleClass, hrefId, pubImage) {
  $('.' + styleClass).addClass('hide-row');
  document.getElementById(hrefId).href = "javascript:showRows('" + styleClass 
    + "', '" + hrefId + "', '" + pubImage + "')";
  document.getElementById(pubImage).src = "images/expand.png";
} 

function showRows(styleClass, hrefId, pubImage) {
  $('.' + styleClass).removeClass('hide-row');
  document.getElementById(hrefId).href = "javascript:hideRows('" + styleClass 
    + "', '" + hrefId + "', '" + pubImage + "')";
  document.getElementById(pubImage).src = "images/collapse.png";
}