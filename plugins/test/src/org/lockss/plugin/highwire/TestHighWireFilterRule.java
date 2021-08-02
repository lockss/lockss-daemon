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

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.LockssTestCase;

public class TestHighWireFilterRule extends LockssTestCase {

  private HighWireFilterRule rule;

  public void setUp() throws Exception {
    super.setUp();
    rule = new HighWireFilterRule();
  }

  private static final String inst1 = "<FONT SIZE=\"-2\" FACE=\"verdana,arial,helvetica\">\n	<NOBR><STRONG>Institution: Periodicals Department/Lane Library</STRONG></NOBR>\n	<NOBR><A TARGET=\"_top\" HREF=\"/cgi/login?uri=%2Fcgi%2Fcontent%2Ffull%2F4%2F1%2F121\">Sign In as Personal Subscriber</A></NOBR>";

  private static final String inst2 = "<FONT SIZE=\"-2\" FACE=\"verdana,arial,helvetica\">\n	<NOBR><STRONG>Institution: Stanford University Libraries</STRONG></NOBR>\n	<NOBR><A TARGET=\"_top\" HREF=\"/cgi/login?uri=%2Fcgi%2Fcontent%2Ffull%2F4%2F1%2F121\">Sign In as Personal Subscriber</A></NOBR>";

  private static final String inst3 = "<FONT SIZE=\"-2\" FACE=\"verdana,arial,helvetica\">\n    <NOBR><STRONG>Institution: Stanford University Libraries</STRONG></NOBR>\n      <NOBR><A TARGET=\"_top\" HREF=\"/cgi/login?uri=%2Fcgi%2Fcontent%2Ffull%2F4%2F1%2F121\">Sign In as SOMETHING SOMETHING</A></NOBR>";

  public void testFiltering() throws IOException {
    Reader readerA;
    Reader readerB;

    readerA = rule.createFilteredReader(new StringReader(inst1));
    readerB = rule.createFilteredReader(new StringReader(inst2));
    assertEquals(StringUtil.fromReader(readerA),
                 StringUtil.fromReader(readerB));

    readerA = rule.createFilteredReader(new StringReader(inst1));
    readerB = rule.createFilteredReader(new StringReader(inst3));
    assertEquals(StringUtil.fromReader(readerA),
                 StringUtil.fromReader(readerB));
  }

}
