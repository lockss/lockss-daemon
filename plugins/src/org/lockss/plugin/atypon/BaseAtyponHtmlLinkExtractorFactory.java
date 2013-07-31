/* $Id: BaseAtyponHtmlLinkExtractorFactory.java,v 1.1 2013-07-31 21:43:58 alexandraohlson Exp $
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

package org.lockss.plugin.atypon;

/* This will require daemon 1.62 and later for JsoupHtmlLinkExtractor support
The vanilla JsoupHtmlLinkExtractor will generate URLs from any forms that it finds on pages
without restrictions (inclusion/exclusion rules) and so long as those resulting URLs satisfy the crawl rules
they will be collected which is too broad because you can't know everything you might encounter. 
This is a thin wrapper that specifies what type of forms to INCLUDE to limit the potential collection. 
*/

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.lockss.extractor.HtmlFormExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractorFactory;
import org.lockss.util.SetUtil;

/* an implementation of JsoupHtmlLinkExtractor with a restrictor set */
public class BaseAtyponHtmlLinkExtractorFactory 
implements LinkExtractorFactory {
  
  /*
   * (non-Javadoc)
   * @see org.lockss.extractor.LinkExtractorFactory#createLinkExtractor(java.lang.String)
   * Simple version for most Atypon children
   * restrict the form download URLs to just those forms with the name="frmCitMgr"
   */
  public org.lockss.extractor.LinkExtractor createLinkExtractor(String mimeType) {
    Set<String> include = new HashSet<String>();
    Map<String, HtmlFormExtractor.FormFieldRestrictions> restrictor
    = new HashMap<String, HtmlFormExtractor.FormFieldRestrictions>();

    /* only include forms with the name "frmCitMgr" */
    include = SetUtil.fromCSV("frmCitmgr");
    HtmlFormExtractor.FormFieldRestrictions include_restrictions = new HtmlFormExtractor.FormFieldRestrictions(include,null);
    restrictor.put(HtmlFormExtractor.FORM_NAME, include_restrictions);
    
    // set up the link extractor with specific includes and excludes
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor();
    extractor.setFormRestrictors(restrictor);
    return extractor;
  }

  /*
   * A version of the method that allows a child to add additional restrictions
   * This method merges the child restrictions with the necessary base restriction
   */
  public org.lockss.extractor.LinkExtractor createLinkExtractor(String mimeType, Map<String, HtmlFormExtractor.FormFieldRestrictions> child_restrictor) {
    Set<String> base_include = new HashSet<String>();
    Map<String, HtmlFormExtractor.FormFieldRestrictions> base_restrictor
    = new HashMap<String, HtmlFormExtractor.FormFieldRestrictions>();
    
    // only include forms with the name "frmCitMgr" 
    base_include = SetUtil.fromCSV("frmCitmgr");
    HtmlFormExtractor.FormFieldRestrictions base_restrictions = new HtmlFormExtractor.FormFieldRestrictions(base_include,null);
    base_restrictor.put(HtmlFormExtractor.FORM_NAME, base_restrictions);
    
    // did the child add in any additional restrictions?
    if (child_restrictor != null) {
      //Iterate over the child's map
      for (String key : child_restrictor.keySet()) {
        //
        if (base_restrictor.containsKey(key)) {
          // the child also is restricting this key, merge the restrictions in to the base
          HtmlFormExtractor.FormFieldRestrictions child_val = child_restrictor.get(key);
          HtmlFormExtractor.FormFieldRestrictions base_val = base_restrictor.get(key);
          Set<String> tmp_inc = base_val.getInclude();
          Set<String> tmp_exc = base_val.getExclude();
          tmp_inc.addAll(child_val.getInclude());
          tmp_exc.addAll(child_val.getExclude());
          base_restrictor.put(key, new HtmlFormExtractor.FormFieldRestrictions(tmp_inc, tmp_exc)); 
        } else {
          // add the child restrictor 
          base_restrictor.put(key,  child_restrictor.get(key));
        }
      }
    }
    
    // set up the link extractor with specific includes and excludes
    JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor();
    extractor.setFormRestrictors(base_restrictor);
    return extractor;
  }
}