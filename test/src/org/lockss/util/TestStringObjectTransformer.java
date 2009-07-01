/**

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

import org.lockss.test.*;

import junit.framework.TestCase;

/**
 * @author edwardsb
 * 
 */
public class TestStringObjectTransformer extends TestCase {
  // Constants
  private static final String k_strParameter = "foobar";

  /* This class gets serialized and deserialized. */
  class TransformerTestObject implements Serializable {
    private static final long serialVersionUID = 1; // Required for Serializable.
    private static final String k_strTestable = "test message";

    private String m_strParameter;
    private String m_strTestable;

    public TransformerTestObject(String parameter) {
      m_strTestable = k_strTestable;
      m_strParameter = parameter;
    }

    public boolean verify(String parameter) {
      boolean result;

      result = m_strTestable.equals(k_strTestable);
      result = result && m_strParameter.equals(parameter);

      return result;
    }
  }

  protected void setUp() throws Exception {
    super.setUp();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Test method for
   * {@link org.lockss.jackrabbit.StringObjectTransformer#transformObjectToString(java.lang.Object)}.
   */
  public void testTransformObjectToString() {
    String strTto;
    StringObjectTransformer sot = new StringObjectTransformer();
    TransformerTestObject tto = new TransformerTestObject(k_strParameter);
    TransformerTestObject tto2;

    strTto = sot.transformObjectToString(tto);
    tto2 = (TransformerTestObject) sot.transformStringToObject(strTto);

    assertTrue(tto2.verify(k_strParameter));
  }
  
  // How well does TransformObjectToString work with threads?
  // Test 1.
  
  private static final int k_numThreads = 10;
  private static final int k_inCount = 1000;
  // How long do we expect each run to take?
  private static final int k_msecWait = 10;  
  
  private static Random ms_random = new Random();
  
  private TransformerTestObject[] m_arttoThreads;
  
  private class TransformerThreadTest1 implements Runnable {
    int m_iThreadNumber;
    
    public TransformerThreadTest1(int iThreadNumber) {
      m_iThreadNumber = iThreadNumber;
    }
    
    public void run() {
      int i;
      StringObjectTransformer sot = new StringObjectTransformer();
      String strTto;
      
      // This variable is unique to each thread.  It is null unless
      // there is a problem.
      m_arttoThreads[m_iThreadNumber] = null;
      
      for (i=0; i<k_inCount; i++) {
        TransformerTestObject tto = new TransformerTestObject(k_strParameter);
        TransformerTestObject tto2;

        strTto = sot.transformObjectToString(tto);
        tto2 = (TransformerTestObject) sot.transformStringToObject(strTto);

        if (!tto2.verify(k_strParameter)) {
          m_arttoThreads[m_iThreadNumber] = tto2;
          break;
        }
      }
    }
  }
  
  
  public void testThreads1() throws InterruptedException {
    int iThread;
    Thread[] arth;
    
    // Test one, using TransformerTestThread1
    arth = new Thread[k_numThreads];
    m_arttoThreads = new TransformerTestObject[k_numThreads];
    
    for (iThread = 0; iThread < k_numThreads; iThread++) {
      arth[iThread] = new Thread(new TransformerThreadTest1(iThread));
      arth[iThread].start();
    }
    
    for (iThread = 0; iThread < k_numThreads; iThread++) {
      // Idea taken from http://java.sun.com/docs/books/
      // tutorial/essential/concurrency/simple.html
      arth[iThread].join(2 * k_inCount * k_msecWait);
      
      if (arth[iThread].isAlive()) {
        fail("Thread " + iThread + " did not end on time.");
      }
      
      if (m_arttoThreads[iThread] != null) {
        fail("Thread " + iThread + " failed to pass.");
      }
    }
  }
  
  // This second test also examines the StringObjectTransformer against many threads.
 
  private class Event {
    private TransformerTestObject m_tto1;
    private TransformerTestObject m_tto2;
    private String m_strTto;
    
    public Event(TransformerTestObject tto1, TransformerTestObject tto2, String strTto) {
      m_tto1 = tto1;
      m_tto2 = tto2;
      m_strTto = strTto;
    }
    
    public TransformerTestObject tto1() {
      return m_tto1;
    }
    
    public TransformerTestObject tto2() {
      return m_tto2;
    }
    
    public String str() {
      return m_strTto;
    }
  }
  
  private List<Event> m_lievents;
  private SimpleBinarySemaphore m_sema;
  
  // The thread that we're working with.
  private class TransformerThreadTest2 implements Runnable {
    public void run() {
      Event eventResult = null;
      int i;
      StringObjectTransformer sot = new StringObjectTransformer();
      String strTto;
          
      for (i=0; i<k_inCount; i++) {
        TransformerTestObject tto = new TransformerTestObject(k_strParameter);
        TransformerTestObject tto2;

        strTto = sot.transformObjectToString(tto);
        tto2 = (TransformerTestObject) sot.transformStringToObject(strTto);

        if (!tto2.verify(k_strParameter)) {
          eventResult = new Event(tto, tto2, strTto);
          break;
        }
      }
      
      m_lievents.add(eventResult);
      m_sema.give();
    }
  }
  
  public void testThreads2() {
    Thread[] arth;
    int i;
    
    // Start threads
    arth = new Thread[k_numThreads];
    m_lievents = new Vector<Event>();  // A vector is a synchronized list.
    m_sema = new SimpleBinarySemaphore();
    
    for (i = 0; i < k_numThreads; i++) {
      arth[i] = new Thread(new TransformerThreadTest2());
      arth[i].start();
    }
    
    // Wait for everyone to be done...
    while (m_lievents.size() < k_numThreads) {
      m_sema.take();
    }
    
    // Examine the results.
    for (Event ev : m_lievents) {
      if (ev != null) {
        fail("testThreads2: A thread failed.");
      }
    }
  }
}

