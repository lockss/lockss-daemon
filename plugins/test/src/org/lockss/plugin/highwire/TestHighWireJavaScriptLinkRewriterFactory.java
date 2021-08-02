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
import org.lockss.test.*;

public class TestHighWireJavaScriptLinkRewriterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private HighWireJavaScriptLinkRewriterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new HighWireJavaScriptLinkRewriterFactory();
    mau = new MockArchivalUnit();
  }

  static final String input_1 = 
    "/org/lockss/plugin/highwire/HighWireJavaScriptLinkRewriter_input_1.html";
  static final String output_1 = 
    "/org/lockss/plugin/highwire/HighWireJavaScriptLinkRewriter_output_1.html";

  static final String input_2 = 
    "/org/lockss/plugin/highwire/HighWireJavaScriptLinkRewriter_input_1.html";
  static final String output_2 = 
    "/org/lockss/plugin/highwire/HighWireJavaScriptLinkRewriter_output_2.html";

  public void testCase1() throws Exception {
    InputStream input = null;
    InputStream filtered = null;
    InputStream expected = null;

    try {
    input = getResourceAsStream(input_1);
    filtered = fact.createLinkRewriter("text/html", mau, input, "UTF-8", 
        "http://jid.sagepub.com/cgi/framedreprint/13/1/5", null);
    expected = getResourceAsStream(output_1);
    String s_expected = StringUtil.fromInputStream(expected);
    String s_filtered = StringUtil.fromInputStream(filtered); 
    assertEquals(s_expected, s_filtered);
    } finally {
      IOUtil.safeClose(input);
      IOUtil.safeClose(filtered);
      IOUtil.safeClose(expected);
    }
  }

  public void testCase2() throws Exception {
    InputStream input = null;
    InputStream filtered = null;
    InputStream expected = null;

    try {
    input = getResourceAsStream(input_2);
    filtered = fact.createLinkRewriter("text/html", mau, input, "UTF-8", 
        "http://jid.sagepub.com/xyzzy/plugh", null);
    expected = getResourceAsStream(output_2);
    String s_expected = StringUtil.fromInputStream(expected);
    String s_filtered = StringUtil.fromInputStream(filtered); 
    assertEquals(s_expected, s_filtered);
    } finally {
      IOUtil.safeClose(input);
      IOUtil.safeClose(filtered);
      IOUtil.safeClose(expected);
    }
  }

}
