/* $Id: FutureScienceLinkExtractorFactory.java,v 1.1 2013-07-01 22:18:04 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.futurescience;

/*
 * IN PROGRESS - In order to support the needs of all the Atypon children, instead of having 
 * each child implement their own LinkExtractorFactory, we need to have the BaseAtypon implement some 
 * abstract version that takes form the children a list or set of things to include and exclude.
 * 
 Create a local version of the LinkExtractorFactory so that 
 we can use the constructor that allows for restrictions.
 After 1.62 is released we can simplify this because the necessary constructor will be directly accessible
 
Here is the URL for a citation download page:
http://www.future-science.com/action/showCitFormats?doi=10.4155%2Fbio.12.154 (bioanalysis journal)

Extracted citation urls look like this:
http://future-science.com/action/downloadCitation?direct=true&doi=10.4155%2Fbio.12.154&downloadFileName=fus_bio4_1843&include=cit&submit=Download+article+metadata

we need it to look like this for the BaseAtyponArticleIterator:
http://future-science.com//action/downloadCitation?doi=XX%2FYY&format=ris&include=cit
so in combination with the url normalizer, we'll need to 
  remove the &downloadFileName=fus_bio4_1843
  remove the &direct=true
  and replace the &include=cit&submit=Download+article+metadata
      with &format=ris&include=cit
*/

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.lockss.extractor.ArticleMetadataExtractorFactory;
import org.lockss.extractor.HtmlFormExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.util.SetUtil;


public class FutureScienceLinkExtractorFactory 
implements LinkExtractorFactory {
  
  /* the JSoupHtmlLinkExtractor did not used to expose the constructor that took restrictors
   * so we had to subclass. This can go away after 1.62 is released and all we'll have to do
   * is set up the appropriate restrictors and call the JSoup constructor directly.
   */
  
  public static class MyJsoupHtmlLinkExtractor extends JsoupHtmlLinkExtractor {
    MyJsoupHtmlLinkExtractor(boolean enableStats, boolean processForms,
        Map<String,HtmlFormExtractor.FormFieldRestrictions> restrictors,
        Map<String,HtmlFormExtractor.FieldIterator> generators) {
      super(enableStats,processForms, restrictors, generators);
    }
  }
  public org.lockss.extractor.LinkExtractor createLinkExtractor(String mimeType) {
    Set<String> include = new HashSet<String>();
    Set<String> exclude = new HashSet<String>();
    Map<String, HtmlFormExtractor.FormFieldRestrictions> restrictor
    = new HashMap<String, HtmlFormExtractor.FormFieldRestrictions>();
    
 
    /* We don't want to restrict the type of formats we get. This code was originally here for
    testing and verification. I'm keeping it in now as an example for other simliiar uses

    include = SetUtil.fromCSV("ris", "bibtex");
    HtmlFormExtractor.FormFieldRestrictions format_restrictions = new HtmlFormExtractor.FormFieldRestrictions(include,null);
    restrictor.put("format", format_restrictions); //format=(ris|bibtex)
    */
    
    // 2. restrictions excluding the direct=on
    exclude = SetUtil.fromCSV("true");
    HtmlFormExtractor.FormFieldRestrictions direct_restrictions = new HtmlFormExtractor.FormFieldRestrictions(null,exclude);
    restrictor.put("direct", direct_restrictions);
    
    // set up the link extractor with specific includes and excludes
    return new MyJsoupHtmlLinkExtractor(false, true,restrictor,null);
  }
}