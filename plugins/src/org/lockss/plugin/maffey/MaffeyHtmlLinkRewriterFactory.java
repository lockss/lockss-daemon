/*
  * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/
package org.lockss.plugin.maffey;

import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Attribute;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.FormTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.MetaTag;
import org.htmlparser.util.NodeList;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.rewriter.*;
import org.lockss.servlet.ServletUtil.LinkTransform;
import org.lockss.util.Logger;

/**
 * This custom link rewriter performs Maffey specific rewriting 
 * to make the PDF links available from the article landing page
 * even if the "Pay per View" form is on the page instead of the PDF link.
 * 
 */
public class MaffeyHtmlLinkRewriterFactory implements LinkRewriterFactory {
  
  
  private static final Logger log =
    Logger.getLogger(MaffeyHtmlLinkRewriterFactory.class);
  
  /**
   * This link rewriter adds special processing to replace "Pay per View"
   * form elements with the appropriate PDF link.
   * We picked up the PDF file from the CLOCKSS TOC but want user to be able
   * to navigate to the PDF from this page.
   * Also - take the TOC breadcrumb link at the top of each article page and 
   * modify it to go to the only TOC we have, the CLOCKSS issue TOC
   * 
   */
  @Override
  public InputStream createLinkRewriter(String mimeType,
                                        ArchivalUnit au,
                                        InputStream in,
                                        String encoding,
                                        String url,
                                        LinkTransform xfm)
      throws PluginException, IOException {
    
    NodeFilterHtmlLinkRewriterFactory fact =
      new NodeFilterHtmlLinkRewriterFactory();
    
    fact.addPreXform(new MaffeyPreFilter(url));
    return fact.createLinkRewriter(mimeType, au, in, encoding, url, xfm);
  }
  
  
  static class MaffeyPreFilter implements NodeFilter {
    
    // /journal.php?journal_id=142&tab=volume#issue934">  
    protected static final Pattern TOC_PATTERN = 
        Pattern.compile(".*/journal\\.php\\?journal_id=[0-9]+&tab=volume#issue([0-9]+)$", Pattern.CASE_INSENSITIVE);   
    private static final String FILE_ID_PARAM = "fileId"; 
    private static final String FILE_NAME_PARAM = "filename"; 
    // getting information from an early part of the DOM for use later
    private String pdfInfo_fileId = null;
    private String pdfInfo_fileName = null;
    // might need this to make sure we create the correct PDF - not sure
    private String html_url = null;
    
    public MaffeyPreFilter(String url) {
      super();
      html_url = url;
    }
    
    public boolean accept(Node node) {
      // store the value of the PDF link arguments for later reassembly
      if (node instanceof MetaTag && ((MetaTag) node).getMetaTagName() != null &&
          ("citation_pdf_url".equalsIgnoreCase(((MetaTag) node).getMetaTagName()))) {
        String pdfInfo = ((MetaTag) node).getMetaContent();
        int qmark = pdfInfo.indexOf("?");
        if (qmark > 1){
          Map<String, String> query = parseUrlArgs(pdfInfo.substring(qmark + 1));
          if (query.containsKey(FILE_ID_PARAM)) {
            pdfInfo_fileId = query.get(FILE_ID_PARAM);
          }
          if (query.containsKey(FILE_NAME_PARAM)) {
            pdfInfo_fileName = query.get(FILE_NAME_PARAM);
          }
        }
      } else if (node instanceof FormTag) {
        // Now if we get a form tag instead of a PDF link, build the correct pdf link here
        String method = ((FormTag) node).getFormMethod();
        TagNode formInputId = ((FormTag) node).getInputTag("article_id");
        Attribute idAttr = ((TagNode)node).getAttributeEx("id");
        if ("post".equalsIgnoreCase(method) && idAttr != null 
            && "pay_per_view".equals(idAttr.getValue())) {
          String art_id = null;
          if (formInputId != null) {
            Attribute formIdVal = formInputId.getAttributeEx("value");
            // could put a check here that the form art_id matches
            // the art_id of the citation_pdf_url
            // not currenlty using this
            //if (formIdVal != null) {
            //  art_id = formIdVal.getValue();
            //}
            Node formParent = node.getParent();
            NodeList parentChildren = formParent.getChildren();
            // htmlparser isn't very smart - build start and end pair manually
            LinkTag link = new LinkTag();
            link.setTagName("A");
            TagNode endTag = new TagNode();
            endTag.setTagName("/A");
            endTag.setParent(formParent);
            // link will be of form: 
            // redirect_file.php?
            // fileType=pdf
            // &fileId=8753
            // &filename=10.1177_1179565417711106.pdf
            // to match what we collected from the CLOCKSS toc
            if (pdfInfo_fileId != null && pdfInfo_fileName != null) {
              link.setLink("redirect_file.php?fileType=pdf&fileId=" + this.pdfInfo_fileId + 
                  "&filename=" + this.pdfInfo_fileName);
            } else {
              // this page itself - just so it's not empty
              link.setLink(html_url);
            }
            link.setAttribute("target", "_blank");
            link.setAttribute("class","dwnload");
            link.setParent(formParent);
            // Not clear why but these need to be set. These values seem to work
            // though they're not exactly accurate.
            // and the start/end values need to be different
            link.setStartPosition(node.getStartPosition());
            endTag.setStartPosition(node.getEndPosition());
            link.setChildren(new NodeList(new TextNode("\nDownload Article PDF")));
            link.setEndTag(endTag);
            parentChildren.add(link);
            parentChildren.remove(node);

          }
        }
      //<a style="text-decoration: underline;" 
      //  href="./journal.php?journal_id=142&tab=volume#issue934">2017:9</a>  
      } else if (node instanceof LinkTag) {
        String linkUrl = ((LinkTag) node).extractLink();
        if (linkUrl == null) {
          return false;
        }
        Matcher tocMat = TOC_PATTERN.matcher(linkUrl); 
        if (tocMat.find()) {
          String issueID = tocMat.group(1);
          String newUrl = "lockss.php?t=clockss&pa=article&i_id=" + issueID;
          ((LinkTag)node).setLink(newUrl);
        }
      }
      return false;
    }


    //redirect_file.php?
    //and passed in to this is the remainder of the url
    // fileId=6960
    // &filename=5207-CMRO-Jaundice-in-Gall-Bladder-Cancer-–-The-Yellow-Signal.pdf
    //    &fileType=pdf"
    protected static Map<String, String> parseUrlArgs(String queryString) {
      queryString = queryString.replace("%26", "&");
      Map<String, String> ret = new HashMap<String, String>();

      for (String pair : queryString.split("&")) {
        log.debug3("pair: " + pair);
        int eq = pair.indexOf("=");
        String key;
        String val;
        if (eq < 0 || eq == pair.length() - 1) {
          key = pair;
          val = null;
        }
        else {
          key = pair.substring(0, eq);
          val = pair.substring(eq + 1);
        }

        ret.put(key, val);
      }

      return ret;
    }
  }

}
