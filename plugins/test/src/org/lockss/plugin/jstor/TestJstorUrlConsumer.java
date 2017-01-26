/*
 * $Id:$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.jstor;

import java.util.Set;
import java.util.regex.Pattern;

import org.lockss.plugin.jstor.JstorCSUrlConsumerFactory.JstorCSUrlConsumer;
import org.lockss.test.LockssTestCase;
import org.lockss.util.SetUtil;


/* 
 * This test doesn't do all that much. It just makes sure that real life URLS used
 * by JSTOR actually map to the regexp used by the url consumer for orig and dest.
 * 
 */
public class TestJstorUrlConsumer extends LockssTestCase {
  Set<String> originatingUrls = SetUtil.set(
      "http://www.jstor.org/stable/pdf/10.2972/hesperia.84.1.0001.pdf",
      "http://www.jstor.org/stable/10.2972/hesperia.84.4.0857",
      "http://www.jstor.org/stable/40024320",
      "http://www.jstor.org/stable/10.2307/40024320"
      );

  Set<String> destinationUrls = SetUtil.set(
      "http://www.jstor.org/stable/pdf/10.2972/hesperia.84.1.0001.pdf?acceptTC=true&coverpage=false",
      "http://www.jstor.org/stable/pdf/40024320.pdf?acceptTC=true&coverpage=false"
      );


  public void testOrigPdfPattern() throws Exception {
    Pattern origFullTextPat = Pattern.compile(JstorCSUrlConsumer.ORIG_STRING, Pattern.CASE_INSENSITIVE);
    for (String url : originatingUrls) {
      assertMatchesRE(origFullTextPat, url);
    }
  }

  public void testDestPdfPattern() throws Exception {
    Pattern destFullTextPat = Pattern.compile(JstorCSUrlConsumer.DEST_PDF_STRING, Pattern.CASE_INSENSITIVE);
    for (String url : destinationUrls) {
      assertMatchesRE(destFullTextPat, url);
    }

  }
  
}
