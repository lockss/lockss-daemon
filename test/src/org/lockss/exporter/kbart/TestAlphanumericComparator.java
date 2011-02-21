package org.lockss.exporter.kbart;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lockss.test.LockssTestCase;

import junit.framework.TestCase;

public class TestAlphanumericComparator extends LockssTestCase {

  private static final String name0 = "name 1";
  private static final String name1 = "Name 16";
  private static final String name2 = "Name 2";
  private static final String name3 = "Name 003";
  private static final String name4 = "100th name";
  private static final String name5 = "A name with 2 or 4 numbers.";
  private static final String name6 = "A name with 2 or 3 numbers.";
  private static final String name7a = "a1a09 a 09 09a a9"; // should tokenise to a,1,a,09, a ,09, ,09,a a,9
  private static final String name7b = "a1a09 a 08 09a a9"; // should tokenise to a,1,a,09, a ,08, ,09,a a,9
  private static final String nameTextOnly = "Text only.";
  private static final String nameNumOnly = "0123456789";
  private static final String nameEmpty = "";

  // Accented characters are sorted last by default - the accented name here should come first
  // if the accents are properly stripped.
  private static final String accentedName = "Sémiotique Appliquée 1";
  private static final String unaccentedName = "Semiotique Appliquee 2";
  
  // Unordered names
  List<String> names = new ArrayList<String>() {{
    add(name0);
    add(name1);
    add(name2);
    add(name3);
    add(name4);
    add(name5);
    add(name6);
    add(name7a);
    add(name7b);
    add(nameTextOnly);
    add(nameNumOnly);
    add(nameEmpty);
    add(accentedName);
    add(unaccentedName);
  }};
  // Should be ordered case-insensitively in natural alphabetical order, and by magnitude for number tokens
  final List<String> namesOrdered = new ArrayList<String>() {{
    add(nameEmpty);
    add(name4);
    add(nameNumOnly);
    add(name6);
    add(name5);
    add(name7b);
    add(name7a);
    add(name0);
    add(name2);
    add(name3);
    add(name1);
    add(accentedName);
    add(unaccentedName);
    add(nameTextOnly);
  }};  
  // Should be ordered with capitals before lower case, 
  final List<String> namesOrderedCaseSensitive = new ArrayList<String>() {{
    add(nameEmpty);
    add(name4);
    add(nameNumOnly);
    add(name6);
    add(name5);
    add(name2);
    add(name3);
    add(name1);
    add(accentedName);
    add(unaccentedName);
    add(nameTextOnly);
    add(name7b);
    add(name7a);
    add(name0);
  }};
  
  public void testCompare() {
    // Sort case-insensitively
    Collections.sort(names, new AlphanumericComparator<String>(false));
    for (int i = 0; i < names.size(); i++) {
      System.out.println(names.get(i));
    }
    for (int i = 0; i < names.size(); i++) {
      assertEquals("Sorting case-insensitively", namesOrdered.get(i), names.get(i));
    }
    
    // Sort case-sensitively
    Collections.shuffle(names);
    Collections.sort(names, new AlphanumericComparator<String>(true));
    for (int i = 0; i < names.size(); i++) {
      //System.err.println(names.get(i));
      assertEquals("Sorting case-sensitively", namesOrderedCaseSensitive.get(i), names.get(i));
    }

    // Case sensitive iterations
    int csit = 1;
    // Sort case-sensitively a few times
    for (int i=0; i<csit; i++) {
      Collections.shuffle(names);
      Collections.sort(names, new AlphanumericComparator<String>(true));
      assertEqualContent(namesOrderedCaseSensitive, names); 
    }
  }

  /**
   * Compare two lists to see if they contain the same elements
   * in the same order. Note that this uses <code>equals()</code>
   * so for predictable behaviour the list entries should 
   * implement {@link java.lang.Comparable}. 
   * 
   * @param l1 a list of items
   * @param l2 a list of items of the same type
   */
  public static <T> void assertEqualContent(List<T> l1, List<T> l2) {
    if (l1.size() != l2.size()) fail("Lists are of different size");
    for (int i=0; i<l1.size(); i++) {
      T item1 = l1.get(i);
      T item2 = l2.get(i);
      if (!item1.equals(item2)) {
	String msg = String.format("Different elements at position %d: '%s' '%s'\n", i, item1.toString(), item2.toString());
	failNotEquals( msg, item1, item2 );
      }
    }
  }
  
}
