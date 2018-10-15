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

package org.lockss.plugin.swjpcc;

import java.util.Set;
import java.util.regex.Pattern;

import org.lockss.test.LockssTestCase;
import org.lockss.util.SetUtil;

public class TestSwjpccUrlConsumer extends LockssTestCase {
  Set<String> originatingUrls = SetUtil.set(
      "http://www.swjpcc.com/storage/manuscripts/volume-17/issue-1-july-2018/085-18/085-18.pdf",
      "http://www.swjpcc.com/storage/pdf-version-of-articles/volume-2/010-11.pdf",
      "http://www.swjpcc.com/storage/manuscripts/volume-5/swjpcc-086-12/SWJPCC%20086-12%20Figure%201.jpg",
      "http://www.swjpcc.com/storage/manuscript-lists/past-volumes/Volume%205.xls",
      "http://www.swjpcc.com/storage/post-images/March%202011%20Critical%20Care%20Journal%20Club%20Figure%201.jpg",
      "http://www.swjpcc.com/storage/website-stuff/1_editors_headshots/Roehrs.jpg",
      "http://www.swjpcc.com/storage/website-stuff/instructions-for-authors/Instructions%20for%20Authors%20Table%201%208-1-13.jpg",
      "http://www.swjpcc.com/storage/manuscript-lists/issues/volume-12/Volume%2012%20Issue%204.xls",
      "http://www.swjpcc.com/storage/manuscript-lists/postings-by-category/Proceedings%207-26-18.xlsx"
      );
  
  Set<String> destinationUrls = SetUtil.set(
      "http://static1.1.sqspcdn.com/static/f/654826/27940362/1530586028657/085-18.pdf?token=xyzq",
      "http://static1.1.sqspcdn.com/static/f/654826/20910580/1352391929677/SWJPCC+086-12+Figure+1.jpg?token=xyzq",
      "http://static1.1.sqspcdn.com/static/f/654826/21433192/1357088301437/Volume+5.xls?token=d3Fjd",
      "http://static1.1.sqspcdn.com/static/f/654826/11416086/1301150829290/March+2011+Critical+Care+Journal+Club+Figure+1.jpg?token=1oJ97",
      "http://static1.1.sqspcdn.com/static/f/654826/14968229/1320386854650/Roehrs.jpg?token=fMO",
      "http://static1.1.sqspcdn.com/static/f/654826/23230100/1375399750430/Instructions+for+Authors+Table+1+8-1-13.jpg?token=PrkC",
      "http://static1.1.sqspcdn.com/static/f/654826/27954673/1532631823010/Proceedings+7-26-18.xlsx?token=SwW",
      " http://static1.1.sqspcdn.com/static/f/654826/27328645/1478815790737/097-16+Online+Supplement.pdf?token=PV3IL"
      );
  
  public void testOrigPdfPattern() throws Exception {
    Pattern origFullTextPat = SwjpccUrlConsumerFactory.getOrigPattern();
    for (String url : originatingUrls) {
      assertMatchesRE(origFullTextPat, url);
    }
  }
  
  public void testDestPdfPattern() throws Exception {
    Pattern destFullTextPat = SwjpccUrlConsumerFactory.getDestPattern();
    for (String url : destinationUrls) {
      assertMatchesRE(destFullTextPat, url);
    }
    
  }
  
}
