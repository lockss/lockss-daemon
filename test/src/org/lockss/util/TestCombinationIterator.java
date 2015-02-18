/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.util;

import org.lockss.test.*;

import java.math.*;
import java.util.*;

/**
 * CombinationIterator Tester.
 *
 * @author Claire Griffin
 * @since <pre>Sep 14, 2012</pre>
 * @version 1.0
 */
public class TestCombinationIterator extends LockssTestCase {
  Integer[]   m_ints   = { 1, 2, 3, 4, 5 };
  Integer[][] m_iCombi1 = {{1},{2},{3},{4},{5}};
  Integer[][] m_iCombi2 = {{1,2}, {1,3} ,{1,4}, {1,5},
                           {2,3}, {2,4}, {2,5}, {3,4}, {3,5},{4,5}};
  Integer[][] m_iCombi3 = {{1,2,3},{1,2,4},{1,2,5},{1,3,4},{1,3,5},{1,4,5},
                           {2,3,4},{2,3,5},{2,4,5},{3,4,5}};
  Integer[][] m_iCombi4 = {{1,2,3,4},{1,2,3,5},{1,2,4,5},{1,3,4,5},{2,3,4,5}};
  Integer[][] m_iCombi5 = {{1,2,3,4,5}};

  String[] m_strs   = { "One", "Two", "Three"};
  String[][] m_sCombi1 = {{"One"},{"Two"}, {"Three"}};
  String[][] m_sCombi2 = {{"One","Two"}, {"One","Three"}, {"Two","Three"}};
  String[][] m_sCombi3 = {{"One","Two","Three"}};


  List<Integer> m_iList = Arrays.asList(m_ints);
  List<String>  m_sList = Arrays.asList(m_strs);

  List<List<Integer>> m_iCombiList;
  List<List<String>> m_sCombiList;

  public void setUp() throws Exception {
    m_iList = Arrays.asList(m_ints);
    m_sList = Arrays.asList(m_strs);
  }

  public void tearDown() throws Exception {
  }

  /**
   *
   * Method: size(final int N, final int K)
   *
   */
  public void testSize() throws Exception {
    BigInteger expected = BigInteger.valueOf(m_iCombi3.length);

    assertEquals(expected, CombinationIterator.size(5, 3));
  }

  /**
   *
   * Method: hasNext()
   *
   */
  public void testHasNext() throws Exception {
    CombinationIterator<Integer> ci = new CombinationIterator<Integer>
        (m_iList, 3);
    assertTrue(ci.hasNext());
    // now call next until for result set time
    for(int i = 0; i < m_iCombi3.length; i++)
    {
      ci.next();
    }
    assertFalse(ci.hasNext());

  }

  /**
   *
   * Method: next()
   *
   */
  public void testNext() throws Exception {
     // test the int version
    checkIntLists(m_iList, 1, m_iCombi1);
    checkIntLists(m_iList, 2, m_iCombi2);
    checkIntLists(m_iList, 3, m_iCombi3);
    checkIntLists(m_iList, 4, m_iCombi4);
    checkIntLists(m_iList, 5, m_iCombi5);
     // test the string version
    checkStringLists(m_sList, 1, m_sCombi1);
    checkStringLists(m_sList, 2, m_sCombi2);
    checkStringLists(m_sList, 3, m_sCombi3);
  }

  /**
   *
   * Method: remove()
   *
   */
  public void testRemove() throws Exception {
    CombinationIterator<Integer> ci = new CombinationIterator<Integer>
        (m_iList, 3);

    try {
      ci.remove();
      assertTrue("Failed to throw exception", false);
    } catch (Exception ex) {
      assertTrue(ex instanceof UnsupportedOperationException);
    }
  }

  /**
   * check Null or Empty lists
   */
  public void testNullorEmptyList() throws Exception {
    // test a null list
    CombinationIterator<Integer> ci;
    try {
       ci = new CombinationIterator<Integer>(null, 1);
      assertTrue("Null set did not throw!", false);
    }
    catch(Exception ex)
    {
      assertTrue(ex instanceof IllegalArgumentException);

    }
    ArrayList<Integer> empty_list = new ArrayList<Integer>();
    try {
      ci = new CombinationIterator<Integer>(empty_list, 1);
      assertTrue("empty set did not throw!", false);
    }
    catch(Exception ex)
    {
      assertTrue(ex instanceof IllegalArgumentException);

    }
  }

  /**
   * given a list of integer from which to select all combinations of choose
   * size
   * Make sure that all combinations are found.
   * @param iList the list to generate combinations from
   * @param choose the number of items to choose from the list
   * @param combinations the expected combinations
   */
  private void checkIntLists(List<Integer> iList, int choose,
                      Integer[][] combinations)
  {
    List<List<Integer>> iCombiList = new ArrayList<List<Integer>>();
    for(int i=0; i<combinations.length; i++)
    {
      iCombiList.add(Arrays.asList(combinations[i]));
    }
    CombinationIterator<Integer> ci
        = new CombinationIterator<Integer>(iList,choose);
    List<Integer> li;

    while(ci.hasNext()) {
      li = ci.next();
      assertTrue(iCombiList.contains(li));
      iCombiList.remove(li);
    }
    assertTrue(iCombiList.isEmpty());
  }

  /**
   * given a list of strings from which to select all combinations of choose
   * size
   * Make sure that all combinations are found.
   * @param sList the list to generate combinations from
   * @param choose the number of items to choose from the list
   * @param combinations the expected combinations
   */
  private void checkStringLists(List<String> sList, int choose,
                                String[][] combinations)
  {
    List<List<String>> sCombiList = new ArrayList<List<String>>();
    for(int i=0; i<combinations.length; i++)
    {
      sCombiList.add(Arrays.asList(combinations[i]));
    }
    CombinationIterator<String> ci
        = new CombinationIterator<String>(sList,choose);
    List<String> li;

    while(ci.hasNext()) {
      li = ci.next();
      assertTrue(sCombiList.contains(li));
      sCombiList.remove(li);
    }
    assertTrue(sCombiList.isEmpty());
  }


} 
