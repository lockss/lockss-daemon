/*
 * $Id: TestAllExceptSubtreeNodeFilter.java,v 1.1.2.3 2014-07-18 15:49:43 wkwilson Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair;

import java.io.ByteArrayOutputStream;

import org.apache.commons.io.IOUtils;
import org.lockss.filter.html.*;
import org.lockss.test.*;

public class TestAllExceptSubtreeNodeFilter extends LockssTestCase {

  /*
   * If there is a matching node for the pruned tree and a matching node for
   * the retained tree within it, the expected result is that the retained
   * subtree will remain but everything else in the pruned subtree will be gone
   * (modulo whitespace artifacts of the transform).
   */
  public void testRetainSubtree() throws Exception {
    doOneTest("<html class='keep'>\n" +
              " <body class='keep'>\n" +
              "  <div class='keep'>...</div>\n" + 
              "  <div class='keep' isprunedtree='true'><div class='keep'><div class='keep' isretainedtree='true'>\n" +
              "     <div class='keep'>...</div>\n" +
              "     <div class='keep'>...</div>\n" +
              "     <div class='keep'>...</div>\n" +
              "    </div></div></div>\n" + 
              "  <div class='keep'>...</div>\n" + 
              " </body>\n" +
              "</html>\n",
              "<html class='keep'>\n" +
              " <body class='keep'>\n" +
              "  <div class='keep'>...</div>\n" + 
              "  <div class='keep' isprunedtree='true'>\n" +
              "   <div class='prune'>...</div>\n" +
              "   <div class='keep'>\n" +
              "    <div class='prune'>...</div>\n" +
              "    <div class='keep' isretainedtree='true'>\n" +
              "     <div class='keep'>...</div>\n" +
              "     <div class='keep'>...</div>\n" +
              "     <div class='keep'>...</div>\n" +
              "    </div>\n" +
              "    <div class='prune'>...</div>\n" +
              "   </div>\n" +
              "   <div class='prune'>...</div>\n" +
              "  </div>\n" + 
              "  <div class='keep'>...</div>\n" + 
              " </body>\n" +
              "</html>\n");
  }
  
  /*
   * If there is a matching node for the pruned tree but no matching node for
   * the retained tree within it, the expected result is that the entire
   * pruned tree is removed (modulo whitespace artifacts of the transform).
   */
  public void testRetainNothing() throws Exception {
    doOneTest("<html class='keep'>\n" +
              " <body class='keep'>\n" +
              "  <div class='keep'>...</div>\n" +
              "  \n" +
              "  <div class='keep'>...</div>\n" + 
              " </body>\n" +
              "</html>\n",
              "<html class='keep'>\n" +
              " <body class='keep'>\n" +
              "  <div class='keep'>...</div>\n" + 
              "  <div class='prune' isprunedtree='true'>\n" +
              "   <div class='prune'>...</div>\n" +
              "   <div class='prune'>\n" +
              "    <div class='prune'>...</div>\n" +
              "    <div class='prune' isretainedtree='FALSE'>\n" +
              "     <div class='prune'>...</div>\n" +
              "     <div class='prune'>...</div>\n" +
              "     <div class='prune'>...</div>\n" +
              "    </div>\n" +
              "    <div class='prune'>...</div>\n" +
              "   </div>\n" +
              "   <div class='prune'>...</div>\n" +
              "  </div>\n" + 
              "  <div class='keep'>...</div>\n" + 
              " </body>\n" +
              "</html>\n");
  }
  
  /*
   * If there is a matching node for the retained tree but no matching node for
   * the pruned tree above it, the expected result is that nothing is changed.
   */
  public void testRetainAll() throws Exception {
    String noChange = "<html class='keep'>\n" +
                      " <body class='keep'>\n" +
                      "  <div class='keep'>...</div>\n" + 
                      "  <div class='keep' isprunedtree='FALSE'>\n" +
                      "   <div class='keep'>...</div>\n" +
                      "   <div class='keep'>\n" +
                      "    <div class='keep'>...</div>\n" +
                      "    <div class='keep' isretainedtree='true'>\n" +
                      "     <div class='keep'>...</div>\n" +
                      "     <div class='keep'>...</div>\n" +
                      "     <div class='keep'>...</div>\n" +
                      "    </div>\n" +
                      "    <div class='keep'>...</div>\n" +
                      "   </div>\n" +
                      "   <div class='keep'>...</div>\n" +
                      "  </div>\n" + 
                      "  <div class='keep'>...</div>\n" + 
                      " </body>\n" +
                      "</html>\n";
    doOneTest(noChange,
              noChange);
  }
  
  protected static void doOneTest(String expectedOutput, String input) throws Exception {
    AllExceptSubtreeNodeFilter retainSubtreeNodeFilter =
        new AllExceptSubtreeNodeFilter(HtmlNodeFilters.tagWithAttribute("div", "isprunedtree", "true"),
                                    HtmlNodeFilters.tagWithAttribute("div", "isretainedtree", "true"));
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    IOUtils.copy(new HtmlFilterInputStream(new StringInputStream(input),
                                           HtmlNodeFilterTransform.exclude(retainSubtreeNodeFilter)),
                 baos);
    assertEquals(expectedOutput,
                 baos.toString());
  }
  
}
