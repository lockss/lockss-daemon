package org.lockss.util;

//~--- non-JDK imports --------------------------------------------------------

import org.lockss.test.*;

import java.util.*;

//~--- JDK imports ------------------------------------------------------------

/**
 * PowerSetIterator Tester.
 *
 * @author Claire Griffin
 * @since <pre>Sep 14, 2012</pre>
 * @version One.0
 */
public class TestPowerSetIterator extends LockssTestCase {
    Integer[]   m_ints   = { 1, 2, 3 };
    Integer[][] m_intSet = {
        {}, { 1 }, { 2 }, { 3 }, { 2, 3 }, { 1, 2 }, { 1, 3 }, { 1, 2, 3 }
    };
    String[]    m_strs   = { "One", "Two", "Three" };
    String[][]  m_strSet = {
        {}, { "One" }, { "Two" }, { "Three" }, { "Two", "Three" }, { "One", "Two" }, { "One", "Three" },
        { "One", "Two", "Three" }
    };
    List<Integer>       m_iSet;
    List<String>        m_sSet;
    List<List<Integer>> m_iPowerset;
    List<List<String>>  m_sPowerset;

    public void setUp() throws Exception {
        m_iSet = new ArrayList<Integer>();

        for (Integer i_val : m_ints) {
            m_iSet.add(i_val);
        }

        m_iPowerset = new ArrayList<List<Integer>>();

        for (Integer[] list : m_intSet) {
            List il = new ArrayList<Integer>();

            for (Integer i_val : list) {
                il.add(i_val);
            }

            m_iPowerset.add(il);
        }

        m_sSet = new ArrayList<String>();

        for (String s_val : m_strs) {
            m_sSet.add(s_val);
        }

        m_sPowerset = new ArrayList<List<String>>();

        for (String[] list : m_strSet) {
            List sl = new ArrayList<String>();

            for (String s_val : list) {
                sl.add(s_val);
            }

            m_sPowerset.add(sl);
        }
    }

    public void tearDown() throws Exception {}

    /**
     *
     * Method: resultSize(final int N)
     *
     */
    public void testResultSize() throws Exception {
        long expected = m_iPowerset.size();

        assertEquals(expected, PowerSetIterator.resultSize(m_ints.length));
    }


    /**
     *
     * Method: hasNext()
     *
     */
    public void testHasNext() throws Exception {
      PowerSetIterator<Integer> psi = new PowerSetIterator<Integer>(m_iSet);
      assertTrue(psi.hasNext());
      // now call next until for result set time
      for(int i = 0; i < m_intSet.length; i++)
      {
        psi.next();
      }
      assertFalse(psi.hasNext());
    }

    /**
     *
     * Method: next()
     *
     */
    public void testNext() throws Exception {
     // test the int version
      PowerSetIterator<Integer> psi = new PowerSetIterator<Integer>(m_iSet);
      while(psi.hasNext())
      {
        List<Integer> li = psi.next();
        assertTrue(m_iPowerset.contains(li));
        m_iPowerset.remove(li);
      }
      assertTrue(m_iPowerset.isEmpty());

     // test the string version
      PowerSetIterator<String> pss = new PowerSetIterator<String>(m_sSet);
      while(pss.hasNext())
      {
        List<String> ls = pss.next();
        assertTrue(m_sPowerset.contains(ls));
        m_sPowerset.remove(ls);
      }
      assertTrue(m_sPowerset.isEmpty());


    }

    public void testNullorEmpty() throws Exception {
            // test a null list
      PowerSetIterator<Integer> ps_iter;
      try {
        ps_iter =
          new PowerSetIterator<Integer>(null);
        assertTrue("Null set did not throw!", false);
      }
      catch(Exception ex)
      {
        assertTrue(ex instanceof IllegalArgumentException);
      }

      // an empty list should return an empty list
      ArrayList<Integer> empty_list = new ArrayList<Integer>();
      ps_iter = new PowerSetIterator<Integer>(empty_list);
      assertTrue(ps_iter.hasNext());
      List<Integer> li = ps_iter.next();
      assertTrue(li.isEmpty());
      assertFalse(ps_iter.hasNext());
    }



    /**
     *
     * Method: remove()
     *
     */
    public void testRemove() throws Exception {
        PowerSetIterator<Integer> psi = new PowerSetIterator<Integer>(m_iSet);

        try {
            psi.remove();
            assertTrue("Failed to throw exception", false);
        } catch (Exception ex) {
            assertTrue(ex instanceof UnsupportedOperationException);
        }
    }

}
