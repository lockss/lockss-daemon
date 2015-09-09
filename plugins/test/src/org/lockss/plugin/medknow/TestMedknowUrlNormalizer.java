/* $Id$

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.medknow;

import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.LockssTestCase;

// TestMednowUrlNormalizer tests for duplicate urls containing article ids (aid)
public class TestMedknowUrlNormalizer extends LockssTestCase {

  public void testUrlNormalizer() throws Exception {
    UrlNormalizer normalizer = new MedknowUrlNormalizer();
    
    // MedknowUrlNormalizer returns the original url, if it does not contains
    // article id (aid)
    assertEquals("http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=13;epage=16;aulast=Kothari",
        normalizer.normalizeUrl("http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=13;epage=16;aulast=Kothari;aid=jpgm_2013_59_2_110_113831", null));
    // this one is unchanged
    assertEquals("http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=13;epage=16;aulast=Kothari",
        normalizer.normalizeUrl("http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=13;epage=16;aulast=Kothari", null));
    
    // on the TOC for an issue url, remove the unnecessary month identifier.
    assertEquals("http://www.jpgmonline.com/showBackIssue.asp?issn=0022-3859;year=2013;volume=59;issue=3",
        normalizer.normalizeUrl("http://www.jpgmonline.com/showBackIssue.asp?issn=0022-3859;year=2013;volume=59;issue=3;month=July-September", null));
    // but it's okay if it doesn't have the month - do nothing
    assertEquals("http://www.jpgmonline.com/showBackIssue.asp?issn=0022-3859;year=2013;volume=59;issue=3",
        normalizer.normalizeUrl("http://www.jpgmonline.com/showBackIssue.asp?issn=0022-3859;year=2013;volume=59;issue=3", null));
    
    //having some trouble with the apostrophe in this URL...is it in the daemon or plugin
    assertEquals("http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D%27Souza;type=2",
        normalizer.normalizeUrl("http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D%27Souza;type=2", null));
    assertEquals("http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D%27Souza",
        normalizer.normalizeUrl("http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D%27Souza", null));
    assertEquals("http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D%27Souza;type=1",
        normalizer.normalizeUrl("http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D%27Souza;type=1", null));
    
    
    // for a citation landing page, remove the unnecessary arguments so we don't get multiples
    assertEquals("http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=3;spage=179;epage=185;aulast=Singh",
        normalizer.normalizeUrl("http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=3;spage=179;epage=185;aulast=Singh;aid=jpgm_2013_59_3_179_118034", null));
    assertEquals("http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=3;spage=179;epage=185;aulast=Singh",
        normalizer.normalizeUrl("http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=3;spage=179;epage=185;aulast=Singh;type=0;aid=jpgm_2013_59_3_179_118034", null));
    assertEquals("http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=3;spage=179;epage=185;aulast=Singh",
        normalizer.normalizeUrl("http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=3;spage=179;epage=185;aulast=Singh;type=0", null));
    // and no change for the simple version
    assertEquals("http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=3;spage=179;epage=185;aulast=Singh",
        normalizer.normalizeUrl("http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=3;spage=179;epage=185;aulast=Singh", null));
    
  }
  
}
