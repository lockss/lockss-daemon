/*
 * $Id$
 */

/*

Copyright (c) 2010-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.exporter.kbart;

import java.util.*;

import javax.naming.OperationNotSupportedException;

import org.lockss.config.TdbTestUtil;
import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.exporter.kbart.KbartTitle.Field.SortType;
import org.lockss.test.LockssTestCase;
import org.lockss.util.CachingComparator;
import org.lockss.util.CatalogueOrderComparator;
import org.lockss.util.ListUtil;

import junit.framework.TestCase;

public class TestKbartFieldOrderComparator extends LockssTestCase {
  
  //Comparator<KbartTitle> kbtc = KbartTitleComparatorFactory.getComparator(Field.PUBLICATION_TITLE);
  
  // Shared ID fields - note that the types match Field.sortType declarations, but are not enforced, 
  // so the values must be carefully chosen. For example the ids and date are not alphanumeric, so 
  // if they are given alphanumeric values the normalisation will be wrong. The fields
  // which are alphanumeric are expected to differ but be the same in full normalisation.
  private static String TITLE  = "A Title of Distinction";
  private static String PRINT_ID  = "1111-9999";
  private static String ONLINE_ID = "9999-1111";
  private static String DATE      = "1976";
  private static String VOL       = "Catch 22";
  // Accented versions
  // Å Tïtlê øf Dìstînctíoñ
  private static String TITLE_ID_ACCENTED  = "\u00C5 T\u00EFtl\u00EA \u00F8f D\u00ECst\u00EEnct\u00EDo\u00F1";
  // Çàtçh 22
  private static String VOL_ACCENTED       = "\u00C7\u00E0t\u00E7h 22";

  // List the fields that we have bothered to fill in for testing; the rest will all be 
  // empty and therefore the same.
  private EnumSet testFields = EnumSet.of(Field.PUBLICATION_TITLE, Field.PRINT_IDENTIFIER, 
      Field.ONLINE_IDENTIFIER, Field.DATE_FIRST_ISSUE_ONLINE, Field.NUM_FIRST_VOL_ONLINE);
  
  // Title 1 - use basic field values 
  private final KbartTitle title1 = TestKbartTitle.createKbartTitle(
      new HashMap<KbartTitle.Field, String>() {{
        put(Field.PUBLICATION_TITLE, TITLE);
        put(Field.PRINT_IDENTIFIER, PRINT_ID);
        put(Field.ONLINE_IDENTIFIER, ONLINE_ID);
        put(Field.DATE_FIRST_ISSUE_ONLINE, DATE);
        put(Field.NUM_FIRST_VOL_ONLINE, VOL);
      }}
  );
  
  // Title 2 - differing only on case
  private final KbartTitle title2 = TestKbartTitle.createKbartTitle(
      new HashMap<KbartTitle.Field, String>() {{
        put(Field.PUBLICATION_TITLE, TITLE.toLowerCase());
        put(Field.PRINT_IDENTIFIER, PRINT_ID.toLowerCase());
        put(Field.ONLINE_IDENTIFIER, ONLINE_ID.toLowerCase());
        put(Field.DATE_FIRST_ISSUE_ONLINE, DATE.toLowerCase());
        put(Field.NUM_FIRST_VOL_ONLINE, VOL.toLowerCase());
      }}
  );
  
  // Title 3 - differing only on accents
  private final KbartTitle title3 = TestKbartTitle.createKbartTitle(
      new HashMap<KbartTitle.Field, String>() {{
        put(Field.PUBLICATION_TITLE, TITLE_ID_ACCENTED);
        put(Field.PRINT_IDENTIFIER, PRINT_ID);
        put(Field.ONLINE_IDENTIFIER, ONLINE_ID);
        put(Field.DATE_FIRST_ISSUE_ONLINE, DATE);
        put(Field.NUM_FIRST_VOL_ONLINE, VOL_ACCENTED);
      }}
  );

  private final List<KbartTitle> titles = new ArrayList<KbartTitle>() {{
    add(title1);
    add(title2);
    add(title3);
  }};

  // Duplicate entries
  private final List<KbartTitle> titlesDuplicate = new ArrayList<KbartTitle>() {{
    addAll(titles);
    addAll(titles);
  }};
  
  // Default getSingleton should throw exception
  public final void testGetSingleton() {
    try {
      KbartFieldOrderComparator.getSingleton();
      fail("Should throw OperationNotSupportedException .");
    } catch (Exception e) {
      // Expected exception
    }
  }

  public final void testGetSingletonField() {
    for (Field f : Field.values()) {
      KbartFieldOrderComparator kbfoc = KbartFieldOrderComparator.getSingleton(f);
      // Test that the singleton for each field successfully maintains its own cache
      testCache(kbfoc);
      // Test that the comparison string comes from the correct field
      for (KbartTitle kbt : new KbartTitle[]{title1, title2, title3}) { 
        assertEquals(kbt.getField(f), kbfoc.getComparisonString(kbt));
      }
      // TODO check that the sorting fits the normalisation defaults of each field?
    }
  }
  
  private void testCache(KbartFieldOrderComparator kbfoc) {
    // We start with a new instance here and reuse it throughout this method
    Field f = kbfoc.sortField;
    CountingComparator cc = new CountingComparator(f);
    
    // Expected number of translations - by default, we expect a translation 
    // per distinct alphanumeric title field
    int exp = testFields.contains(f) && f.isAlphanumeric() ? titles.size() : 1;
    
    Collections.shuffle(titles);
    Collections.sort(titles, cc);
    assertEquals(exp, cc.xlateCnt);
    
    Collections.shuffle(titlesDuplicate);
    Collections.sort(titlesDuplicate, cc);
    assertEquals(exp, cc.xlateCnt);
  }
    
  // Wrapper for a comparator which counts translation calls
  static class CountingComparator extends KbartFieldOrderComparator {
    CountingComparator(Field f) { super(f); }
    int xlateCnt = 0;
    protected String xlate(String s) {
      xlateCnt++;
      return super.xlate(s);
    }
  }

}
