/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.hlthaff;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
//import java.util.regex.Pattern;
import java.io.OutputStreamWriter;

import org.apache.commons.io.IOUtils;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.LinkTag;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlCrawlFilterFactory;
import org.lockss.uiapi.util.Constants;

public class HealthAffairsHtmlCrawlFilterFactory extends BaseAtyponHtmlCrawlFilterFactory {
  // protected static final Pattern related_articlelink_pattern = Pattern.compile("related( article|editorial)?", Pattern.CASE_INSENSITIVE);

  NodeFilter[] filters = new NodeFilter[] {
      // Leave in this specific filter for Health Affairs which often puts 
      // related links on the TOC alongside the other formats (abs, full, pdf) ???
      new NodeFilter() {
            @Override public boolean accept(Node node) {
              if (!(node instanceof LinkTag)) return false;
              String allText = ((CompositeTag)node).toPlainTextString();
              // return related_articlelink_pattern.matcher(allText).find();
              return allText.toLowerCase().contains("related");
            }
        },
  };
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {
    return super.createFilteredInputStream(au, in, encoding);
  }
  
  public static void main(String[] args) throws Exception {
    String file1 = "/home/etenbrink/workspace/data/ha1.html";
    String file2 = "/home/etenbrink/workspace/data/ha2.html";
    String file3 = "/home/etenbrink/workspace/data/ha3.html";
    String file4 = "/home/etenbrink/workspace/data/ha4.html";
    IOUtils.copy(new HealthAffairsHtmlCrawlFilterFactory().createFilteredInputStream(null, 
        new FileInputStream(file1), Constants.DEFAULT_ENCODING), 
        new OutputStreamWriter(new FileOutputStream(file1 + ".out"), Constants.DEFAULT_ENCODING));
    IOUtils.copy(new HealthAffairsHtmlCrawlFilterFactory().createFilteredInputStream(null,
        new FileInputStream(file2), Constants.DEFAULT_ENCODING),
        new OutputStreamWriter(new FileOutputStream(file2 + ".out"), Constants.DEFAULT_ENCODING));
  }
  
}
