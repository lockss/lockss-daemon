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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;

public class TestTitle {

  public static final String NAME_VALUE = "Title Name";
  public static final String DOI_VALUE = "DOI Value";
  public static final String EISSN_VALUE = "eISSN Value";
  public static final String ISSN_VALUE = "ISSN Value";
  public static final String ISSNL_VALUE = "ISSN-L Value";
  public static final String TYPE_VALUE = "Type Value";
  
  public static final String FOO_KEY = "titlefookey";
  public static final String FOO_VALUE = "titlefooval";

  @Test
  public void testKeys() throws Exception {
    assertEquals("doi", Title.DOI);
    assertEquals("eissn", Title.EISSN);
    assertEquals("issn", Title.ISSN);
    assertEquals("issnl", Title.ISSNL);
    assertEquals("name", Title.NAME);
    assertEquals("type", Title.TYPE);
  }
  
  @Test
  public void testType() throws Exception {
    assertEquals(Title.TYPE_JOURNAL, Title.TYPE_DEFAULT);
    assertEquals("book", Title.TYPE_BOOK);
    assertEquals("bookSeries", Title.TYPE_BOOK_SERIES);
    assertEquals("journal", Title.TYPE_JOURNAL);
    assertEquals("proceedings", Title.TYPE_PROCEEDINGS);
  }
  
  @Test
  public void testEmpty() throws Exception {
    Publisher publisher = new Publisher();
    Title title = new Title(publisher);
    assertSame(publisher, title.getPublisher());
    assertNull(title.getName());
    assertNull(title.getDoi());
    assertNull(title.getEissn());
    assertNull(title.getIssn());
    assertNull(title.getIssnl());
    assertEquals(Title.TYPE_DEFAULT, title.getType());
    assertNull(title.getArbitraryValue(FOO_KEY));
  }

  @Test
  public void testTitle() throws Exception {
    Publisher publisher = new Publisher();
    Map<String, String> map = new HashMap<String, String>();
    Title title = new Title(publisher, map);
    assertSame(publisher, title.getPublisher());
    map.put(Title.NAME, NAME_VALUE);
    assertEquals(NAME_VALUE, title.getName());
    map.put(Title.DOI, DOI_VALUE);
    assertEquals(DOI_VALUE, title.getDoi());
    map.put(Title.EISSN, EISSN_VALUE);
    assertEquals(EISSN_VALUE, title.getEissn());
    map.put(Title.ISSN, ISSN_VALUE);
    assertEquals(ISSN_VALUE, title.getIssn());
    map.put(Title.ISSNL, ISSNL_VALUE);
    assertEquals(ISSNL_VALUE, title.getIssnl());
    map.put(Title.TYPE, TYPE_VALUE);
    assertEquals(TYPE_VALUE, title.getType());
    map.put(FOO_KEY, FOO_VALUE);
    assertEquals(FOO_VALUE, title.getArbitraryValue(FOO_KEY));
    assertNull(title.getArbitraryValue("X" + FOO_KEY));
  }
  
}
