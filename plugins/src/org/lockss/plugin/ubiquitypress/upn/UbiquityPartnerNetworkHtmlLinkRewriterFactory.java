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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.Option;

import org.apache.commons.lang.StringUtils;
import org.htmlparser.*;
import org.htmlparser.tags.*;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ubiquitypress.upn.UbiquityPartnerNetworkHtmlLinkExtractorFactory.UbiquityPartnerNetworkHtmlLinkExtractor;
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
  //static Pattern jsonStrWithDigitsPat = Pattern.compile("^([0-9A-Fa-f]*:[^\\[\\{]*)([\\[\\{].*)$",Pattern.MULTILINE);
  static Pattern jsonStrWithDigitsPat = Pattern.compile("^([0-9A-Fa-f]*:(?:HL|I)?)([\\[\\{].*)$");
  static Pattern rawJsonHrefPat = Pattern.compile("(\"href\":\")(/[^\"]+)(\")");

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
        if (node instanceof LinkTag) { // Note: LinkTag is <a>
          LinkTag link = (LinkTag)node;
          String href = link.getLink();
          if (href != null) {
            link.setLink(href.trim());
          }
        }
        if (   (node instanceof ImageTag)
            || (node instanceof Tag && "link".equalsIgnoreCase(((Tag)node).getTagName())) // Note: LinkTag is <a>
            || (node instanceof Tag && "source".equalsIgnoreCase(((Tag)node).getTagName()))) {
          Tag tag = (Tag)node;
          String attr = "link".equalsIgnoreCase(((Tag)node).getTagName()) ? "imagesrcset" : "srcset";
          String srcSet = tag.getAttribute(attr);
          if (StringUtils.isNotEmpty(srcSet)) {
            String[] candidateImageStrings = StringUtils.split(srcSet, ',');
            boolean atLeastOneChange = false;
            for (int i = 0 ; i < candidateImageStrings.length ; ++i) {
              Matcher srcSetMat = UbiquityPartnerNetworkHtmlLinkExtractor.srcSetPat.matcher(candidateImageStrings[i]);
              if (srcSetMat.matches()) {
                try {
                  String oldUrl = srcSetMat.group(2).replace("&amp;", "&");
                  if (!oldUrl.startsWith("/")) {
                    continue;
                  }
                  String newUrl = xform.rewrite(UrlUtil.encodeUrl(UrlUtil.resolveUri(baseUrl, oldUrl)));
                  candidateImageStrings[i] = String.format("%s %s", newUrl, srcSetMat.group(4));
                  atLeastOneChange = true;
                }
                catch (MalformedURLException mue) {
                  log.debug3("Malformed source set URL", mue);
                }
              }
            }
            if (atLeastOneChange) {
              tag.setAttribute(attr, StringUtils.join(candidateImageStrings, ','));
            }
          }
        }
        if(node instanceof ScriptTag){
          ScriptTag script = (ScriptTag)node;
          String scriptContentsBefore = script.toPlainTextString();
          String scriptContents = scriptContentsBefore;
          if (!StringUtil.isNullString(scriptContents)) {
            Matcher jsonPushMat = jsonPushPat.matcher(scriptContents);
            if(jsonPushMat.find()){
              log.debug3(String.format("Contents of the script tag before processing: %s", scriptContents));
              String jsonExpression = jsonPushMat.group(2);
              log.debug3("jsonExpression is: " + jsonExpression);
              StringBuffer sb = new StringBuffer();
              /*if(jsonExpression.startsWith("[") && !jsonExpression.endsWith("]")){
                //FIX ME
                jsonExpression = fixBadJSON(xform, jsonExpression, baseUrl);
                log.debug3(String.format("JSON expression adjusted to: %s", jsonExpression));
              }*/
              DocumentContext dc = null;
              try{
                dc = JsonPath.parse(jsonExpression);
              }catch(JsonPathException e){
                //some json couldn't parse, ignoring
                log.debug3(String.format("Could not parse JSON expression into dc: %s", jsonExpression), e);
                return false;
              }
              Object obj1 = dc.read("$",Object.class);
              
              if( !(obj1 instanceof ArrayList) ){
                log.debug3("JSON expression is not the expected array");
              }
              else {
                ArrayList arr1 = (ArrayList)obj1;
                if( !( arr1.size() > 1 && arr1.get(0) instanceof Integer && arr1.get(1) instanceof String ) ){
                  log.debug3("JSON expression array is not in the expected shape");
                }
                else {
                  String str1 = (String)arr1.get(1);
                  //split str1 around newlines
                  String[] arr2 = StringUtils.splitPreserveAllTokens(str1, '\n'); 
                  //for each substring check if it matches the str2 regex 
                  for(int i = 0; i < arr2.length; i++){
                    String strArr2 = arr2[i];
                    Matcher jsonStrWithDigitsMat = jsonStrWithDigitsPat.matcher(strArr2);
                    String str2 = null;
                    if(jsonStrWithDigitsMat.find(0)){
                      str2 = jsonStrWithDigitsMat.group(2);
                    }
                    if(!jsonStrWithDigitsMat.find(0) || (str2.startsWith("[") && !str2.endsWith("]")) || (!str2.startsWith("[") && str2.endsWith("]"))){
                      /*
                       * Unfortunately, there are some href links that are split
                       * across script tags. We need to find these and rewrite
                       * them ad-hoc. In this array of arrays, each sub-array
                       * represents a known case; the first item (index 0) is
                       * the interrupted end of strArr2 in one <script> tag, the
                       * second item (index 1) is the continuation of strArr2 in
                       * the next <script> tag, and the third and later (index 2
                       * and later) items are URLs where this specific
                       * interruption occurs.
                       */
                      String[][] splitHrefs = {
                          {
                            "\"href\":\"/en/a", "rticles/10.3943/jcss.45\",", // Link to https://jcss.demontfortuniversitypress.org/en/articles/10.3943/jcss.45 (which redirects to https://jcss.demontfortuniversitypress.org/articles/10.3943/jcss.45), found on...
                            "https://account.jcss.demontfortuniversitypress.org/index.php/dmu-j-jcss/issue/view/8", // which redirects to...
                            "https://jcss.demontfortuniversitypress.org/8/volume/0/issue/0", // which in turn redirects to...
                            "https://jcss.demontfortuniversitypress.org/en/8/volume/4/issue/0", // which in turn redirects to...
                            "https://jcss.demontfortuniversitypress.org/8/volume/4/issue/0"
                          },
                          {
                            "\"href\":\"/en/articles/10.2259", "9/jachs.111\",", // Link to https://jachs.org/en/articles/10.22599/jachs.111 (which redirects to https://jachs.org/articles/10.22599/jachs.111), found on... 
                            "https://account.jachs.org/index.php/wr-j-jachs/issue/view/6", // which redirects to...
                            "https://jachs.org/6/volume/0/issue/0", // which in turn redirects to...
                            "https://jachs.org/en/6/volume/3/issue/1", // which in turn redirects to...
                            "https://jachs.org/6/volume/3/issue/1"
                          },
                          {
                            "\"href\":\"/en/articles/", "10.22599/jachs.114\",", // Link to https://jachs.org/en/articles/10.22599/jachs.114 (which redirects to https://jachs.org/articles/10.22599/jachs.114), found on... 
                            "https://account.jachs.org/index.php/wr-j-jachs/issue/view/6", // which redirects to...
                            "https://jachs.org/6/volume/0/issue/0", // which in turn redirects to...
                            "https://jachs.org/en/6/volume/3/issue/1", // which in turn redirects to...
                            "https://jachs.org/6/volume/3/issue/1"
                          },
                          {
                            "\"href\":\"", "/en/articles/10.22599/jachs.34\",", // Link to https://jachs.org/en/articles/10.22599/jachs.34 (which redirects to https://jachs.org/articles/10.22599/jachs.34), found on... 
                            "https://account.jachs.org/index.php/wr-j-jachs/issue/view/1", // which redirects to...
                            "https://jachs.org/1/volume/0/issue/0", // which in turn redirects to...
                            "https://jachs.org/en/1/volume/1/issue/1", // which in turn redirects to...
                            "https://jachs.org/1/volume/1/issue/1"
                          },
                          {
                            "\"href\"", ":\"/en/articles/10.22599/jachs.71\",", // Link to https://jachs.org/en/articles/10.22599/jachs.71 (which redirects to https://jachs.org/articles/10.22599/jachs.71), found on... 
                            "https://account.jachs.org/index.php/wr-j-jachs/issue/view/2", // which redirects to...
                            "https://jachs.org/2/volume/0/issue/0", // which in turn redirects to...
                            "https://jachs.org/en/2/volume/2/issue/1", // which in turn redirects to...
                            "https://jachs.org/2/volume/2/issue/1"
                          },
                      };
                      special_cases_label: for (int specialCase = 0 ; specialCase < splitHrefs.length ; ++specialCase) {
                        for (int pageUrl = 2 ; pageUrl < splitHrefs[specialCase].length ; ++pageUrl) {
                          if (splitHrefs[specialCase][pageUrl].equals(url)) {
                            String splitEnding = splitHrefs[specialCase][0];
                            String splitBeginning = splitHrefs[specialCase][1];
                            if (strArr2.endsWith(splitEnding)) {
                              strArr2 += splitBeginning;
                              log.debug3(String.format("strArr2 lengthened to: %s", strArr2));
                              break special_cases_label; 
                            }
                            if (strArr2.startsWith(splitBeginning)) {
                              strArr2 = strArr2.substring(splitBeginning.length());
                              log.debug3(String.format("strArr2 shortened to: %s", strArr2));
                              break special_cases_label; 
                            }
                          }
                        }
                      }
                      String repl2 = fixBadJSON(xform, strArr2, baseUrl);
                      sb.append(repl2);
                    }else{
                      String str2Before = strArr2;
                      log.debug3(String.format("str2 is: %s", str2));
                      Configuration conf = Configuration.builder().options(Option.AS_PATH_LIST, Option.SUPPRESS_EXCEPTIONS, Option.ALWAYS_RETURN_LIST).build();
                    /*
                      some json is malformed (i.e., open brackets/parentheses are not closed properly)
                      Example: \n47:[\"$\",\"$L2e\",null,{\"ref\":\"$undefined\",\"href\":\"/en/6/volume/2/issue/0\",\"locale\":\"$"]
                      \n4e:[\"$\",\"$L2e\",null,{\"ref\":\"$undefined\",\"href\":\"/en/articles/28\",\"locale\":\"$undefined\",\"localeCookie\":\"$2a:"]
                    */
                    //if((str2.startsWith("[") && !str2.endsWith("]")) || (!str2.startsWith("[") && str2.endsWith("]"))){

                      //jsonStrWithDigitsMat.appendReplacement(sb, repl2);
                      /*
                      int closedCurly = StringUtils.countMatches(str2, "{") - StringUtils.countMatches(str2, "}");
                      log.debug3(String.format("Adjusting str2; closedCurly=%d", closedCurly));
                      if(closedCurly > 0){
                        //continue;
                        int lastOpenCurly = str2.lastIndexOf("{");
                        log.debug3("LAST OPEN CURLY IS " + lastOpenCurly);
                        if(lastOpenCurly >= 0){
                          int lastComma = str2.substring(lastOpenCurly).lastIndexOf(",") > -1 ? str2.substring(lastOpenCurly).lastIndexOf(",") : 0;
                          String lastItem = str2.substring(lastOpenCurly).substring(lastComma);
                          log.debug3("LAST ITEM IS " + lastItem);
                          if(lastItem.contains(":")){
                            if(lastItem.endsWith(":")){
                              //add a fake value
                              if(lastItem.endsWith("\":")){
                                str2 = str2 + "\"FAKE-VALUE\"";
                              }else{
                                str2 = str2 + "\"";
                              } 
                            }else{
                              //add closing quote
                              str2 = str2 + "\"";
                            }
                          }else{
                            if(lastItem.length() > 0){
                              //then add colon and fake value
                              if(!lastItem.endsWith("\"")){
                                str2 = str2 + "\"";
                              }
                              str2 = str2 + ":\"FAKE-VALUE\"";
                            }else{
                              //add a fake key, a colon and a fake value to str2
                              str2 = str2 + "\"FAKE-KEY\":\"FAKE-VALUE\"";
                            }
                          }
                        }
                      }
                      while(closedCurly > 0){
                        str2 = str2 + "}";
                        closedCurly--;
                      }
                      str2 = str2 + "]";
                      */
                    //}
                    //else {
                      DocumentContext dc2Paths = null;
                      try{
                        dc2Paths = JsonPath.using(conf).parse(str2);
                      }catch(JsonPathException e){
                        //some json couldn't parse, ignoring
                        log.debug3(String.format("Could not parse str2 into dc2Paths: %s", str2), e);
                      }
                      DocumentContext dc2Values = JsonPath.parse(str2);
                      //fix FRONTEND_URL
                      List<String> jsonPaths = null;
                      if(jsonStrWithDigitsMat.group(1).endsWith(":")){
                        jsonPaths = Arrays.asList("$..pageUrl",
                                                  "$..link",
                                                  "$..thumb",
                                                  "$..href",
                                                  "$..src",
                                                  "$..children[?(@[3].item.link)][2]",
                                                  "$..children[?(@[3].href)][2]",
                                                  "$..[?(@.content=~/https?:\\/\\/.*/)].content");
                      }else{
                        jsonPaths = Arrays.asList("$[?(@=~/(https?:\\/\\/|static\\/chunks\\/).*/)]",
                                                  "$..*[?(@=~/(https?:\\/\\/|static\\/chunks\\/).*/)]");
                      }
                      for(String jsonPath : jsonPaths){
                        log.debug3(String.format("Applying JSON Path: %s", jsonPath));
                        List<String> paths = dc2Paths.read(jsonPath);
                        for(String path : paths){
                          Object objVal = dc2Values.read(path);
                          log.debug3(String.format("Applying replacement to: %s", objVal));
                          if( !( objVal != null && objVal instanceof String ) ){
                            log.debug3("Not the expected object value");
                          }
                          else {
                            String val = (String)objVal;
                            String newUrl;
                            try{
                              newUrl = xform.rewrite(UrlUtil.encodeUrl(UrlUtil.resolveUri(baseUrl, val)));
                              log.debug3(String.format("Transformation from %s to %s", val, newUrl));
                              dc2Paths.set((String)path, newUrl);
                            }catch (MalformedURLException e){
                              log.debug3(String.format("Malformed URL during transformation: baseUrl=%s val=%s", baseUrl, val));
                            };
                          }
                        }
                      }
                      String repl2 = String.format("$1%s", Matcher.quoteReplacement(dc2Paths.jsonString()));
                      if (!repl2.equals(str2Before)) {
                        log.debug3(String.format("repl2 replacement is: %s", repl2));
                      }
                      String replaced = jsonStrWithDigitsMat.replaceFirst(repl2);
                      log.debug3("replaced string is : " + replaced);
                      sb.append(replaced);
                    }
                    if(i != arr2.length - 1){
                      sb.append("\n");
                    }
                  }
                  //jsonStrWithDigitsMat.appendTail(sb);
                  dc.set("$[1]",sb.toString());
                  String repl1 = String.format("$1%s$3",Matcher.quoteReplacement(dc.jsonString()));
                  if (!repl1.equals(str1)) {
                    log.debug3(String.format("str1 after all replacements: %s", sb.toString()));
                  }
                  scriptContents = jsonPushMat.replaceFirst(repl1);
                  script.setScriptCode(scriptContents);
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
            if (!scriptContents.equals(scriptContentsBefore)) {
              log.debug3(String.format("Contents of the script tag after processing: %s", scriptContents));
            }
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

  public String fixBadJSON(LinkTransform xform, String str, String baseUrl){
    // JSON string ends badly
    String previousStr = str;
    Matcher rawJsonHrefMat = rawJsonHrefPat.matcher(str);
    if (rawJsonHrefMat.find()) {
      try {
        String oldUrl = rawJsonHrefMat.group(2);
        String newUrl = xform.rewrite(UrlUtil.encodeUrl(UrlUtil.resolveUri(baseUrl, oldUrl)));
        str = rawJsonHrefMat.replaceFirst(String.format("$1%s$3", Matcher.quoteReplacement(newUrl)));
      }
      catch (MalformedURLException mue) {
        log.debug3(String.format("Malformed URL in substitution from: %s", rawJsonHrefMat.group(2)), mue);
      }
    }
    else {
      // Nothing
    }
    if (!str.equals(previousStr)) {
      log.debug3(String.format("str adjusted to: %s", str));
    }
    return str;
  }
}
