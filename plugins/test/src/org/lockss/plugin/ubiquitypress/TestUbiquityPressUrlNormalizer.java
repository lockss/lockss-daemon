/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ubiquitypress;

import org.lockss.plugin.ubiquitypress.UbiquityPressUrlNormalizer;
import org.lockss.test.LockssTestCase;

public class TestUbiquityPressUrlNormalizer extends LockssTestCase {
  
  public void testUbiquityPressNormalizer() throws Exception {
    UbiquityPressUrlNormalizer norm = new UbiquityPressUrlNormalizer();
    assertEquals("http://www.google.com/ui/uo",
                 norm.doNormalize("http://www.google.com/ui/uo",
                                   "http://www.presentpasts.info/","pp"));
    assertEquals("http://www.google.com/ui/uo",
                 norm.doNormalize("http://www.google.com/ui/uo",
                                   "http://www.presentpasts.info/",null));
    assertEquals("http://www.google.com/ui/uo",
                 norm.doNormalize("http://www.google.com/ui/uo",
                                   null,"pp"));
    assertEquals("http://www.presentpasts.info/article/52/64",
                 norm.doNormalize("http://www.presentpasts.info/article/52/64",
                                   "http://www.presentpasts.info/", null));
    assertEquals("http://www.presentpasts.info/index.php/pp/article/52/64",
                 norm.doNormalize("http://www.presentpasts.info/article/52/64",
                                   "http://www.presentpasts.info/","pp"));
    assertEquals(
        "http://www.presentpasts.info/index.php/pp/article/view/pp.52/92",
        norm.doNormalize("http://www.presentpasts.info/article/view/pp.52/92",
                          "http://www.presentpasts.info/","pp"));
    assertEquals("http://www.presentpasts.info/index.php/pp/help.html",
                 norm.doNormalize("http://www.presentpasts.info/help.html",
                                   "http://www.presentpasts.info/","pp"));
  }
  
}
