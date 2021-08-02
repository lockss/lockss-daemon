/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.highwire;

import java.text.*;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.test.LockssTestCase;
import org.lockss.util.TimeZoneUtil;

public class TestHighWirePressCrawlWindow extends LockssTestCase {

  public void testCrawlWindowCA() throws ParseException {
    TimeZone.setDefault(TimeZoneUtil.getExactTimeZone("GMT"));

    HighWirePressCrawlWindow windowFactory = new HighWirePressCrawlWindow();
    CrawlWindow window = windowFactory.makeCrawlWindow();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    sdf.setTimeZone(TimeZoneUtil.getExactTimeZone("America/Los_Angeles"));

    assertTrue(window.canCrawl(sdf.parse("2006-06-19 04:59:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 04:59:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-06-19 05:00:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 05:00:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-06-19 05:01:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 05:01:00.0")));

    assertFalse(window.canCrawl(sdf.parse("2006-06-19 10:59:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 10:59:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-06-19 11:00:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 11:00:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-06-19 11:01:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 11:01:00.0")));

    //can crawl on weekends
    assertTrue(window.canCrawl(sdf.parse("2006-09-09 08:00:00.0"))); 
    assertTrue(window.canCrawl(sdf.parse("2006-09-10 08:00:00.0"))); 
  }

  public void testCrawlWindowNY() throws ParseException {
    TimeZone.setDefault(TimeZoneUtil.getExactTimeZone("GMT"));

    HighWirePressCrawlWindow windowFactory = new HighWirePressCrawlWindow();
    CrawlWindow window = windowFactory.makeCrawlWindow();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    sdf.setTimeZone(TimeZoneUtil.getExactTimeZone("America/New_York"));

    assertTrue(window.canCrawl(sdf.parse("2006-06-19 07:59:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 07:59:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-06-19 08:00:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 08:00:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-06-19 08:01:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 08:01:00.0")));

    assertFalse(window.canCrawl(sdf.parse("2006-06-19 13:59:00.0")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 13:59:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-06-19 14:00:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 14:00:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-06-19 14:01:00.0")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 14:01:00.0")));
  }

  public void testCrawlWindowDSTCheck() throws ParseException {
    TimeZone.setDefault(TimeZoneUtil.getExactTimeZone("GMT"));

    HighWirePressCrawlWindow windowFactory = new HighWirePressCrawlWindow();
    CrawlWindow window = windowFactory.makeCrawlWindow();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z");
//     sdf.setTimeZone(TimeZone.getTimeZone("PDT"));

    assertTrue(window.canCrawl(sdf.parse("2006-06-19 04:59:00.0 PDT")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 04:59:00.0 PST")));
    assertFalse(window.canCrawl(sdf.parse("2006-06-19 05:00:00.0 PDT")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 05:00:00.0 PST")));
    assertFalse(window.canCrawl(sdf.parse("2006-06-19 05:01:00.0 PDT")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 05:01:00.0 PST")));

    assertFalse(window.canCrawl(sdf.parse("2006-06-19 10:59:00.0 PDT")));
    assertFalse(window.canCrawl(sdf.parse("2006-12-19 10:59:00.0 PST")));
    assertTrue(window.canCrawl(sdf.parse("2006-06-19 11:00:00.0 PDT")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 11:00:00.0 PST")));
    assertTrue(window.canCrawl(sdf.parse("2006-06-19 11:01:00.0 PDT")));
    assertTrue(window.canCrawl(sdf.parse("2006-12-19 11:01:00.0 PST")));
  }
}
