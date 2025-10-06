/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.ubiquitypress.upn;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.Option;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.*;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ScriptTag;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.*;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.rewriter.NodeFilterHtmlLinkRewriterFactory.HtmlBaseProcessor;
import org.lockss.util.*;

public class UbiquityPartnerNetworkHtmlLinkRewriterFactory implements LinkRewriterFactory {

  static Logger log = Logger.getLogger(UbiquityPartnerNetworkHtmlLinkRewriterFactory.class);
  //{\"href\":\"/api/1.8.4/spritesheet#user\"}
  static Pattern jsonHref = Pattern.compile("(\\\\\"href\\\\\":\\s*\\\\\")(.*?)(\\\\\")");
  static Pattern jsonPushPat = Pattern.compile("^((?:self\\.__next_f|\\(self\\.__next_[fs]=self\\.__next_[fs]\\|\\|\\[\\]\\))\\.push\\()(.*)(\\))$");
  //json expressions may be across multiple lines (/n)
  static Pattern jsonStrWithDigitsPat = Pattern.compile("^((?:[0-9A-Fa-f]+)?:)([\\[\\{\"].*)$",Pattern.MULTILINE);

  // Matches protocol pattern (e.g. "http://")
  static final String protocolPat = "[^:/?#]+://+";
  
  // Matches protocol prefix of a URL (e.g. "http://") OR ref ("#...")
  // Used negated to find relative URLs, excluding those that are just a
  // #ref
  static final String protocolOrRefPrefixPat = "^(" + protocolPat + "|#)";

  @Override
  public InputStream createLinkRewriter(String mimeType, 
                                        ArchivalUnit au,
                                        InputStream in,
                                        String encoding,
                                        String url,
                                        LinkTransform xform)
      throws PluginException, IOException {
    NodeFilterHtmlLinkRewriterFactory fact = new NodeFilterHtmlLinkRewriterFactory();
    
    /*
     * Some "Download" PDF links in Utrecht University Library's Studium
     * (https://www.gewina-studium.nl/) have an href value with extraneous
     * whitespace before and after, even spanning multiple lines. The regular
     * expressions of NodeFilterHtmlLinkRewriterFactory are anchored and don't
     * rewrite such links. Pre-process all <a> tags by trimming the href value.
     * 
     * Example from
     * https://www.gewina-studium.nl/articles/abstract/10.18352/studium.1451/:
     * 
<a
    
        class="piwik_download"
        data-trackThis='downloads'
        data-category="PDF"
        data-label=
                "
            
                10.18352/studium.1451#1480
            "

    
        href="
    
        /articles/10.18352/studium.1451/galley/1480/download/
    ">
    PDF
    (EN)
</a>
     */

    class JSONRewritingFilter implements NodeFilter{
      String baseUrl;
      Pattern filter;
      boolean negateFilter = false;

      JSONRewritingFilter(String filter, boolean ignoreCase) {
        this.filter = Pattern.compile(filter,
                                      ignoreCase ? Pattern.CASE_INSENSITIVE : 0);
      }

      public JSONRewritingFilter setBaseUrl(String newBase) {
        baseUrl = newBase;
        return this;
      }

      public JSONRewritingFilter setNegateFilter(boolean val) {
        negateFilter = val;
        return this;
      }

      protected boolean isFilterMatch(String str) {
        boolean isMatch = filter.matcher(str).find();
        return negateFilter ? !isMatch : isMatch;
      }

      @Override
      public boolean accept(Node node) {
        if (node instanceof LinkTag) {
          LinkTag link = (LinkTag)node;
          String href = link.getLink();
          if (href != null) {
            link.setLink(href.trim());
          }
        }
        if(node instanceof ScriptTag){
          ScriptTag script = (ScriptTag)node;
          String s = script.toPlainTextString();
          if (!StringUtil.isNullString(s)) {
            //script.removeAttribute("src");
            log.debug3("the text BEFORE replacement is " + s);
            //if(s.startsWith("self.__next") || s.startsWith("(self.__next")){

            Matcher jsonPushMat = jsonPushPat.matcher(s);
            if(jsonPushMat.find()){
              StringBuffer sb = new StringBuffer();
              //contents of json
              String jsonExpression = jsonPushMat.group(2);
              if(jsonExpression.startsWith("[") && !jsonExpression.endsWith("]")){
                jsonExpression = jsonExpression + "\"]";
                log.debug3("NOT WOOHOO!!!! :(");
              }
              DocumentContext dc = null;
              try{
                dc = JsonPath.parse(jsonExpression);
              }catch(JsonPathException e){
                //some json couldn't parse, ignoring
                log.debug3("could not parse json dc: " + jsonExpression,e);
                return false;
              }
              Object obj1 = dc.read("$",Object.class);
              
              if(obj1 instanceof ArrayList){
                ArrayList arr1 = (ArrayList)obj1;
                if(arr1.size() > 1 && arr1.get(0) instanceof Integer && arr1.get(1) instanceof String){
                  String str1 = (String)arr1.get(1);
                  log.debug3("STRING 1 IS: " + str1);
                  Matcher jsonStrWithDigitsMat = jsonStrWithDigitsPat.matcher(str1);
                  while(jsonStrWithDigitsMat.find()){
                    String str2 = jsonStrWithDigitsMat.group(2);
                    Configuration conf = Configuration.builder().options(Option.AS_PATH_LIST, Option.SUPPRESS_EXCEPTIONS, Option.ALWAYS_RETURN_LIST).build();
                    /*log.debug3("STR2 is : " + str2);
                    //some json is not configured correctly (i.e., open brackets/parentheses are not closed properly)
                    //Example: \n47:[\"$\",\"$L2e\",null,{\"ref\":\"$undefined\",\"href\":\"/en/6/volume/2/issue/0\",\"locale\":\"$"]
                    //\n4e:[\"$\",\"$L2e\",null,{\"ref\":\"$undefined\",\"href\":\"/en/articles/28\",\"locale\":\"$undefined\",\"localeCookie\":\"$2a:"]
                    */
                    if(str2.startsWith("[") && !str2.endsWith("]")){
                      int closedCurly = StringUtils.countMatches(str2, "{") - StringUtils.countMatches(str2, "}");
                      if(closedCurly > 0){
                        int lastOpenCurly = str2.lastIndexOf("{");
                        int lastComma = str2.substring(lastOpenCurly).lastIndexOf(",");
                        String lastItem = str2.substring(lastComma);
                        if(lastItem.contains(":")){
                          if(lastItem.endsWith(":")){
                            //add a fake value
                          }else{
                            //do nothing
                          }
                        }else{
                          if(lastItem.length() > 0){
                            //then add colon and fake value
                          }else{
                            //add a fake key, a colon and a fake value to str2
                          }
                        }
                        int commas = StringUtils.countMatches(str2.substring(lastOpenCurly), ",");
                        int colons = StringUtils.countMatches(str2.substring(lastOpenCurly), ":");
                        if(colons == commas + 1){
                          //the object definition seems to be balanced (have all its key-value pairs); then do nothing
                          if(str2.endsWith(":")){
                            //last key-value pair doesn't have value
                            str2 = str2 + "\"FAKE-VALUE\"";
                          }
                        }else if(colons == commas){
                          //it appears the last key-value pair only has a key
                          str2 = str2 + ":\"FAKE-VALUE\"";
                        }else{
                          //there are probably colons and/or commas in the key-pair values 
                        }
                      }
                      while(closedCurly > 0){
                        str2 = str2 + "}";
                        closedCurly--;
                      }
                      str2 = str2 + "]";

                      /*if(str2.endsWith("$")){
                        str2 += "\"}]";
                      }else if(!str2.endsWith(":")){
                        str2 += ":\"blah\"}]";
                      }else{
                        str2 += "blah\"}]";
                      }
                      log.debug3("NEW STRING: " + str2);*/
                    }
                    DocumentContext dc2Paths = null;
                    try{
                      dc2Paths = JsonPath.using(conf).parse(str2);
                    }catch(JsonPathException e){
                      //some json couldn't parse, ignoring
                      log.debug3("could not parse json dc2paths: " + str2,e);
                    }
                    DocumentContext dc2Values = JsonPath.parse(str2);
                    //fix FRONTEND_URL
                    List<String> JSONpaths = Arrays.asList("$..pageUrl","$..link","$..href","$..children[?(@[3].item.link)][2]","$..children[?(@[3].href)][2]","$..FRONTEND_URL");
                    log.debug3("JSONpaths is " + JSONpaths.toString());
                    for(String JSONpath : JSONpaths){
                      List<String> paths = dc2Paths.read(JSONpath);
                      for(String path : paths){
                        log.debug3("the path is " + dc2Values.read(path));
                        String val = dc2Values.read(path);
                        if(val != null){
                          String newUrl;
                          try{
                            newUrl = xform.rewrite(UrlUtil.encodeUrl(UrlUtil.resolveUri(baseUrl, val)));
                            log.debug3("the new url is " + newUrl);
                            dc2Paths.set(path, newUrl);
                          }catch (MalformedURLException e){
                            log.debug("Couldn't resolve: the base url is " + baseUrl + " and the second group is " +  val);
                          };
                        }
                      }
                    }
                    jsonStrWithDigitsMat.appendReplacement(sb, String.format("$1%s", Matcher.quoteReplacement(dc2Paths.jsonString())));
                    log.debug3("STRING BUFFER IS " + sb.toString());
                  }
                  jsonStrWithDigitsMat.appendTail(sb);
                  log.debug3("AFTER APPEND TAIL: " + sb.toString());
                  dc.set("$[1]",sb.toString());
                  s = jsonPushMat.replaceFirst(String.format("$1%s$3",Matcher.quoteReplacement(dc.jsonString())));
                  script.setScriptCode(s);
                }
              }
              
              /* 
              Matcher mat = jsonHref.matcher(s);
              StringBuffer sb = new StringBuffer();
              while(mat.find()){
                String hrefVal = mat.group(2);
                log.debug3("hrefVal: " + hrefVal + ", filt: " + filter.pattern() + ", match: " + isFilterMatch(hrefVal));
                if (isFilterMatch(hrefVal)) {
                  log.debug3("the second matching group is " + hrefVal);
                  String newUrl;
                  try{
                    newUrl = xform.rewrite(UrlUtil.encodeUrl(UrlUtil.resolveUri(baseUrl, hrefVal)));
                  }catch (MalformedURLException e){
                    log.warning("Couldn't resolve: the base url is " + baseUrl + " and the second group is " +  hrefVal);
                    newUrl = hrefVal;
                  };
                  log.debug3("new url is " + newUrl);
                  String rep = "$1" + Matcher.quoteReplacement(newUrl) + "$3";
                  mat.appendReplacement(sb, rep);
                  log.debug3(rep);
                } else {
                  mat.appendReplacement(sb, Matcher.quoteReplacement(mat.group(0)));
                }
              }
              mat.appendTail(sb);
              script.setScriptCode(sb.toString());
              log.debug3("the text AFTER replacement is " + sb.toString());*/
            }
            log.debug3("the text AFTER replacement is " + s);
          }
        }
        return false;
      }
    }

    // Rewrite absolute links to urlStem/... to targetStem + urlStem/...
    Collection<String> urlStems = au.getUrlStems();
    log.debug3("Stems: " + urlStems);
    List<JSONRewritingFilter> xforms = new ArrayList<>();
    HtmlBaseProcessor baseProc = new HtmlBaseProcessor(url);

    String defUrlStem = null;
    for (String urlStem : urlStems) {
      if (defUrlStem == null) {
        defUrlStem = urlStem;
      }
      xforms.add(new JSONRewritingFilter("^" + urlStem, true)
                 .setBaseUrl(url));
    }
    if (defUrlStem == null) {
      throw new PluginException("No default URL stem for " + url);
    }

    // Transform protocol-relative link URLs.  These are essentially abs
    // links with no scheme.
    for (String urlStem : urlStems) {
      int colon = urlStem.indexOf("://");
      if (colon < 0) continue;
      String proto = urlStem.substring(0, colon);
      String hostPort = urlStem.substring(colon + 3);
      xforms.add(new JSONRewritingFilter("^//" + hostPort, true)
                 .setBaseUrl(url));
    }

    // Rewrite relative links
    xforms.add(new JSONRewritingFilter(protocolOrRefPrefixPat, true)
               .setBaseUrl(url)
               .setNegateFilter(true));

    baseProc.setXforms(ListUtil.list(xforms));
    log.debug3("Xforms: " + xforms);

    fact.addPreXform(baseProc);
    for (NodeFilter filt : xforms) {
      fact.addPreXform(filt);
    }
    
    /*
     * Images in Utrecht University Library's Studium
     * (https://www.gewina-studium.nl/) are displayed in a Featherlight
     * (https://noelboss.github.io/featherlight/) widget. In this context, an
     * <a> tag has a 'data-featherlight' attribute with a relative URL, and
     * contains a regular <img> tag inside it. When clicked, the image is
     * rendered in a popup, but using the 'data-featherlight' link from the <a>
     * tag.
     * 
     * Example from
     * https://www.gewina-studium.nl/articles/10.18352/studium.10120/:
     * 
<a href="#" data-featherlight="figures/Cocquyt_fig1.jpg">
  <img src="figures/Cocquyt_fig1.jpg">
</a>
     */
    fact.addAttrToRewrite("data-featherlight");
    
    return fact.createLinkRewriter(mimeType, au, in, encoding, url, xform);
  }

}
