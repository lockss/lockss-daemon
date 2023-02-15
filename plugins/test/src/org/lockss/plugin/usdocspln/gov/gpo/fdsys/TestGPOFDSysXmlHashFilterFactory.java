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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestGPOFDSysXmlHashFilterFactory extends LockssTestCase {
  
  private static final String ENC = Constants.DEFAULT_ENCODING;

  private GPOFDSysXmlHashFilterFactory fact;

  public void setUp() throws Exception {
    super.setUp();
    fact = new GPOFDSysXmlHashFilterFactory();
  }

  public void testHttp() throws Exception {
    doTestString("<storage>" +
                 "<contentLocation><contentLocationType>URI</contentLocationType>" +
                 "<contentLocationValue>Public Access Rendition https://www.gpo.gov/fdsys/" +
                 "pkg/CFR-2015-title12-vol4/html/CFR-2015-title12-vol4.htm</contentLocationValue>" +
                 "</contentLocation>" +
                 "<storageMedium>hard disk</storageMedium>" +
                 "</storage>",
                 
                 "<storage>" +
                 "<contentLocation><contentLocationType>URI</contentLocationType>" +
                 "<contentLocationValue>Public Access Rendition http://www.gpo.gov/fdsys/" +
                 "pkg/CFR-2015-title12-vol4/html/CFR-2015-title12-vol4.htm</contentLocationValue>" +
                 "</contentLocation>" +
                 "<storageMedium>hard disk</storageMedium>" +
                 "</storage>");
  }

  
  private void doTestString(String expectedOutput,
                            String input) throws Exception {
    assertEquals(expectedOutput,
        StringUtil.fromInputStream(fact.createFilteredInputStream(null,
            new StringInputStream(input),
            ENC)));
  }
  
}