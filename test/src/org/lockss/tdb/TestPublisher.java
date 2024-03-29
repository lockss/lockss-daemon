/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.tdb;

import java.util.*;

import org.lockss.test.LockssTestCase;

public class TestPublisher extends LockssTestCase {

  public static final String NAME_VALUE = "Publisher Name";

  public static final String FOO_KEY = "publisherfookey";
    public static final String FOO_VALUE = "publisherfooval";

  public void testKeys() throws Exception {
    assertEquals("name", Publisher.NAME);
  }
  
  public void testEmpty() throws Exception {
    Publisher publisher = new Publisher();
    assertNull(publisher.getName());
    assertNull(publisher.getArbitraryValue(FOO_KEY));
  }
  
  public void testPublisher() throws Exception {
    Map<String, String> map = new LinkedHashMap<String, String>();
    Publisher publisher = new Publisher(map);
    map.put(Publisher.NAME, NAME_VALUE);
    assertEquals(NAME_VALUE, publisher.getName());
    map.put(FOO_KEY, FOO_VALUE);
    assertEquals(FOO_VALUE, publisher.getArbitraryValue(FOO_KEY));
    assertNull(publisher.getArbitraryValue("X" + FOO_KEY));
  }
  
}
