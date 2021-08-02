/*
 * $Id:$
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

package org.lockss.plugin.highwire.ash;

import java.util.Set;
import java.util.regex.Pattern;

import org.lockss.test.LockssTestCase;
import org.lockss.util.SetUtil;

public class TestASHUrlConsumer extends LockssTestCase {
  Set<String> originatingUrls = SetUtil.set(
      "http://www.bloodjournal.org/highwire/filestream/318501/field_highwire_adjunct_files/0/blood-2012-10-455055-1.pdf"
      );
  
  Set<String> destinationUrls = SetUtil.set(
      "http://www.bloodjournal.org/content/bloodjournal/suppl/2012/12/18/blood-2012-10-455055.DC1/blood-2012-10-455055-1.pdf?sso-checked=true"
      );
  
  public void testOrigPdfPattern() throws Exception {
    Pattern origFullTextPat = ASHJCoreUrlConsumerFactory.getOrigPattern();
    for (String url : originatingUrls) {
      assertMatchesRE(origFullTextPat, url);
    }
  }
  
  public void testDestPdfPattern() throws Exception {
    Pattern destFullTextPat = ASHJCoreUrlConsumerFactory.getDestPattern();
    for (String url : destinationUrls) {
      assertMatchesRE(destFullTextPat, url);
    }
    
  }
  
}
