/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.medknow;

import org.lockss.plugin.BaseUrlHttpHttpsUrlNormalizer;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.test.LockssTestCase;

// TestMednowUrlNormalizer tests for duplicate urls containing article ids (aid)
public class TestMedknowUrlNormalizer extends LockssTestCase {

  public void testUrlNormalizer() throws Exception {
    BaseUrlHttpHttpsUrlNormalizer normalizer = new MedknowUrlNormalizer();
    
    // MedknowUrlNormalizer returns the original url, if it does not contains
    // article id (aid)
    assertEquals("http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=13;epage=16;aulast=Kothari",
        normalizer.additionalNormalization("http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=13;epage=16;aulast=Kothari;aid=jpgm_2013_59_2_110_113831", null));
    // this one is unchanged
    assertEquals("http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=13;epage=16;aulast=Kothari",
        normalizer.additionalNormalization("http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=13;epage=16;aulast=Kothari", null));
    
    // on the TOC for an issue url, remove the unnecessary month identifier.
    assertEquals("http://www.jpgmonline.com/showBackIssue.asp?issn=0022-3859;year=2013;volume=59;issue=3",
        normalizer.additionalNormalization("http://www.jpgmonline.com/showBackIssue.asp?issn=0022-3859;year=2013;volume=59;issue=3;month=July-September", null));
    // but it's okay if it doesn't have the month - do nothing
    assertEquals("http://www.jpgmonline.com/showBackIssue.asp?issn=0022-3859;year=2013;volume=59;issue=3",
        normalizer.additionalNormalization("http://www.jpgmonline.com/showBackIssue.asp?issn=0022-3859;year=2013;volume=59;issue=3", null));
    // but be sure if there is a "supp=Y" that gets left
    assertEquals("http://www.urologyannals.com/showBackIssue.asp?issn=0974-7796;year=2015;volume=7;issue=6;supp=Y",
        normalizer.additionalNormalization("http://www.urologyannals.com/showBackIssue.asp?issn=0974-7796;year=2015;volume=7;issue=6;month=July;supp=Y", null));
    
    //having some trouble with the apostrophe in this URL...is it in the daemon or plugin
    assertEquals("http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D%27Souza;type=2",
        normalizer.additionalNormalization("http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D%27Souza;type=2", null));
    assertEquals("http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D%27Souza",
        normalizer.additionalNormalization("http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D%27Souza", null));
    assertEquals("http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D%27Souza;type=1",
        normalizer.additionalNormalization("http://www.jpgmonline.com/article.asp?issn=0022-3859;year=2013;volume=59;issue=1;spage=15;epage=20;aulast=D%27Souza;type=1", null));
    
    
    // for a citation landing page, remove the unnecessary arguments so we don't get multiples
    assertEquals("http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=3;spage=179;epage=185;aulast=Singh",
        normalizer.additionalNormalization("http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=3;spage=179;epage=185;aulast=Singh;aid=jpgm_2013_59_3_179_118034", null));
    assertEquals("http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=3;spage=179;epage=185;aulast=Singh",
        normalizer.additionalNormalization("http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=3;spage=179;epage=185;aulast=Singh;type=0;aid=jpgm_2013_59_3_179_118034", null));
    assertEquals("http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=3;spage=179;epage=185;aulast=Singh",
        normalizer.additionalNormalization("http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=3;spage=179;epage=185;aulast=Singh;type=0", null));
    // and no change for the simple version
    assertEquals("http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=3;spage=179;epage=185;aulast=Singh",
        normalizer.additionalNormalization("http://www.jpgmonline.com/citation.asp?issn=0022-3859;year=2013;volume=59;issue=3;spage=179;epage=185;aulast=Singh", null));
    
    
    // for encoding authLast
    assertEquals("http://www.jpgmonline.com/article.asp?issn=0189-6725;year=2015;volume=12;issue=3;spage=200;epage=202;aulast=P%E9rez-Egido;type=0",
        normalizer.additionalNormalization("http://www.jpgmonline.com/article.asp?issn=0189-6725;year=2015;volume=12;issue=3;spage=200;epage=202;aulast=P" + (char) 0xE9 + "rez-Egido;type=0", null));
    assertEquals("http://www.jpgmonline.com/article.asp?issn=0189-6725;year=2015;volume=12;issue=3;spage=200;epage=202;aulast=P%E9rez-Egido;type=0",
        normalizer.additionalNormalization("http://www.jpgmonline.com/article.asp?issn=0189-6725;year=2015;volume=12;issue=3;spage=200;epage=202;aulast=P\u00e9rez-Egido;type=0", null));
    assertEquals("http://www.jpgmonline.com/article.asp?issn=0189-6725;year=2015;volume=12;issue=3;spage=200;epage=202;aulast=Perez-Egido;type=0",
        normalizer.additionalNormalization("http://www.jpgmonline.com/article.asp?issn=0189-6725;year=2015;volume=12;issue=3;spage=200;epage=202;aulast=Perez-Egido;type=0", null));
    
  }
  
}
