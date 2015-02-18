/*
 * $Id$
 */
/*
 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
package ca.athabascau.plugin.jrp;

import java.io.InputStream;

import org.htmlparser.NodeFilter;
import org.lockss.daemon.PluginException;
import org.lockss.filter.html.*;
import org.lockss.plugin.*;


/**
 * @author edwardsb
 *
 */
public class JournalOfResearchPracticeHtmlFilterFactory extends org.lockss.plugin.ojs2.OJS2HtmlFilterFactory {
  
  private static final NodeFilter[] filters = new NodeFilter[] {
      HtmlNodeFilters.tagWithAttribute("table", "id", "table1"),
  };
  
  
  /* @Override
   * @see org.lockss.plugin.FilterFactory#createFilteredInputStream(org.lockss.plugin.ArchivalUnit, java.io.InputStream, java.lang.String)
   */
  @Override
  public InputStream createFilteredInputStream(
      ArchivalUnit au, InputStream in, String encoding)
          throws PluginException {
    
    InputStream instr1 = super.createFilteredInputStream(au, in, encoding, filters);
    return instr1;
  }

}
