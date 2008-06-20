<SCRIPT language="Javascript">
<!-- 
   // This script was modified from the Internet Archive's
   // script with help from WERA
   
   function xResolveUrl(url) {
      var image = new Image();
      image.src = url;
      return image.src;
   }
   function xLateUrl(aCollection, sProp, mode) {
      var i = 0;
      for(i = 0; i < aCollection.length; i++) {
        var prop = aCollection[i][sProp];
        if (typeof(prop) == "string") { 
          if (prop.indexOf("mailto:") == -1 && prop.indexOf("javascript:") == -1) {
            // aCollection[i]["target"] = "_top";
            if(prop.indexOf("/cgi/reprint") >= 0) {
              //alert("PDF(" + prop + "): " + sProp);
              prop = prop + ".pdf";
            }
            if(prop.indexOf(urlTarget) == 0) {
              //alert("Journal(" + prop + "): " + sProp);
              prop = urlPrefix + encodeURIComponent(prop);
            } else if (prop.indexOf(urlPrefix+urlTarget) == 0) {
              //alert("Munged(" + prop + "): " + sProp);
              //prop = urlPrefix + urlTarget + encodeURIComponent(prop.substring(urlPrefix.length+urlTarget.length+1));
            } else if (prop.indexOf(urlPrefix) == 0) {
              //alert("Local(" + prop + "): " + sProp);
              //prop = encodeURIComponent(prop);
            } else if (prop.indexOf(urlLocalPrefix) == 0) {
              //alert("Local(" + prop + "): " + sProp);
              prop = urlPrefix + urlTarget + encodeURIComponent(prop.substring(urlLocalPrefix.length+1));
            } else if (prop.indexOf("/") == 0) {
              //alert("Relative(" + prop + "): " + sProp);
              prop = urlPrefix + urlTarget + encodeURIComponent(prop.substring(1));
            } else if (prop.indexOf("#") == 0) {
              //alert("Relative2(" + prop + "): " + sProp);
              prop = urlPrefix + encodeURIComponent(urlSuffix + prop);
            } else if (prop.indexOf("http") != 0) {
              //alert("Relative3(" + prop + "): " + sProp);
              prop = urlPrefix + urlTarget + encodeURIComponent(prop);
            }
            //alert("xLatedUrl(" + prop + "): " + sProp);
            aCollection[i][sProp] = prop;
          }
        }
      }
   }

   //debugger;
   xLateUrl(document.getElementsByTagName("IMG"),"src","inline");
   xLateUrl(document.getElementsByTagName("A"),"href","standalone");
   xLateUrl(document.getElementsByTagName("AREA"),"href","standalone");
   xLateUrl(document.getElementsByTagName("LINK"),"href","inline");
   xLateUrl(document.getElementsByTagName("OBJECT"),"codebase","inline");
   xLateUrl(document.getElementsByTagName("OBJECT"),"data","inline");
   xLateUrl(document.getElementsByTagName("APPLET"),"codebase","inline");
   xLateUrl(document.getElementsByTagName("APPLET"),"archive","inline");
   xLateUrl(document.getElementsByTagName("EMBED"),"src","inline");
   xLateUrl(document.getElementsByTagName("BODY"),"background","inline");
   xLateUrl(document.getElementsByTagName("SCRIPT"),"src","inline");
   xLateUrl(document.getElementsByTagName("FRAME"),"src","inline");
   var frames = document.getElementsByTagName("FRAME","inline");
   if (frames) {
       for (k = 0; k < frames.length; k++) {
           if (typeof(frames[i]["src"]) == "string") {
               var l = frames[i]["src"].indexOf("frameset_url=");
               if (l > -1) {
                   frames[i]["src"] = frames[i]["src"].substring(0,l+13) +
                       urlPrefix + xLateUrl(frames[i]["src"].substring(l+14));
               }
           }
       }
   }
   var forms = document.getElementsByTagName("FORM","inline");
   if (forms) {
       var j = 0;
       for (j = 0; j < forms.length; j++) {
              f = forms[j];
              if (typeof(f.action)  == "string") {
                 if(typeof(f.method)  == "string") {
                     if(typeof(f.method) != "post") {
                        f.action = urlPrefix + encodeURIComponent(f.action);
                     }
                  }
              }
        }
    }

   var interceptRunAlready = false;
   function intercept_js_href_iawm(destination) {
     if(!interceptRunAlready &&top.location.href != destination) {
       interceptRunAlready = true;
       top.location.href = urlPrefix+xResolveUrl(destination);
     }
   } 
   // ie triggers
   href_iawmWatcher = document.createElement("a");
   top.location.href_iawm = top.location.href;
   if(href_iawmWatcher.setExpression) {
     href_iawmWatcher.setExpression("dummy","intercept_js_href_iawm(top.location.href_iawm)");
   }
   // mozilla triggers
   function intercept_js_moz(prop,oldval,newval) {
     intercept_js_href_iawm(newval);
     return newval;
   }
   if(top.location.watch) {
     top.location.watch("href_iawm",intercept_js_moz);
   }

   var notice = 
     "<div style='" +
     "position:relative;z-index:99999;"+
     "border:1px solid;color:black;background-color:lightYellow;font-size:10px;font-family:sans-serif;padding:5px'>" + 
     weraNotice +
  	 " [ <a style='color:blue;font-size:10px;text-decoration:underline' href=\"javascript:void(top.disclaimElem.style.display='none')\">" + weraHideNotice + "</a> ]" +
     "</div>";

    function getFrameArea(frame) {
      if(frame.innerWidth) return frame.innerWidth * frame.innerHeight;
      if(frame.document.documentElement && frame.document.documentElement.clientHeight) return frame.document.documentElement.clientWidth * frame.document.documentElement.clientHeight;
      if(frame.document.body) return frame.document.body.clientWidth * frame.document.body.clientHeight;
      return 0;
    }

    function disclaim() {
      if(top!=self) {
        largestArea = 0;
        largestFrame = null;
        for(i=0;i<top.frames.length;i++) {
          frame = top.frames[i];
          area = getFrameArea(frame);
          if(area > largestArea) {
            largestFrame = frame;
            largestArea = area;
          }
        }
        if(self!=largestFrame) {
          return;
        }
      }
     disclaimElem = document.createElement('div');
     disclaimElem.innerHTML = notice;
     top.disclaimElem = disclaimElem;
     document.body.insertBefore(disclaimElem,document.body.firstChild);
    }
    // disclaim();

-->

</SCRIPT>

