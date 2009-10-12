/*
 * $Id: JcrRepositoryHelperFactory.java,v 1.1.2.5 2009-10-12 20:28:58 edwardsb1 Exp $
 */
/*
 Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.repository.jcr;

import java.io.File;
import java.util.*;

import org.lockss.app.LockssDaemon;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.protocol.IdentityManager;
import org.lockss.repository.LockssRepositoryException;
import org.lockss.util.Logger;

/**
 * This class is a singleton.  It creates JcrHelperRepositories, and it returns the
 * ones that are already around.
 * 
 * @author Brent E. Edwards
 *
 */
public class JcrRepositoryHelperFactory {
  // Constants
  private static int k_maxHelpers = 8;
  
  // Static variables
  private static JcrRepositoryHelperFactory sm_jrhf = null;
  private static Logger logger = Logger.getLogger("JcrRepositoryHelperFactory");

  // Class variables
  private IdentityManager m_idman;
  private int m_indexNextRepositoryHelper = 0;
  private LockssDaemon m_ld;
  // Only one copy of each JcrRepositoryHelper, please...
  private Map<String /* Directory */, JcrRepositoryHelper> m_mapdirjhr = new HashMap<String, JcrRepositoryHelper>();
  private long m_sizeWarcMax;
  
  /**
   * The constructor.  JcrRepositoryHelperFactory is a singleton, so this 
   * method should only be called by the preconstructor. 
   * 
   * @param sizeWarcMax  How many characters per WARC file?
   * @param idman        The identity of a LOCKSS cache
   * @param ld           The daemon
   */
  private JcrRepositoryHelperFactory(long sizeWarcMax, IdentityManager idman, LockssDaemon ld) {
    m_sizeWarcMax = sizeWarcMax;
    m_idman = idman;
    m_ld = ld;
  }
  
  /**
   * Call this method exactly once before calling <code>getSingleton</code>
   *
   * Any suggested names for this method?  It must be called before the call
   * to getSingleton.
   * 
   * @param sizeWarcMax   How many characters per WARC file?
   * @param idman         The identity of a LOCKSS cache
   * @param ld            The daemon
   * @throws LockssRepositoryException
   */
  
  public static void preconstructor(long sizeWarcMax, IdentityManager idman, LockssDaemon ld) 
  throws LockssRepositoryException {
    if (sm_jrhf == null) {
      sm_jrhf = new JcrRepositoryHelperFactory(sizeWarcMax, idman, ld);
    } else {
      logger.error("JcrRepositoryHelperFactory: the preconstructor was called multiple times.");
      throw new LockssRepositoryException("Don't call JcrRepositoryHelperFactory.preconstructor more than once.");
    }
  }
  
  /**
   * Has the method <code>preconstructor</code> been called?
   * 
   * @return Whether the method <code>preconstructor</code> has been called.
   */
  public static boolean isPreconstructed() {
    return sm_jrhf != null;
  }
  
  
  /**
   * How other methods can get a copy of the JcrRepositoryHelperFactory.
   * 
   * @return The singleton for this class.
   * @throws LockssRepositoryException
   */
  public static JcrRepositoryHelperFactory getSingleton() throws LockssRepositoryException {
    if (sm_jrhf != null) {
      return sm_jrhf;
    }
    
    logger.error("JcrRepositoryHelperFactory: call the preconstructor before you get a singleton.");
    throw new LockssRepositoryException("Call preconstructor before you get the singleton.");
  }
  
  /**
   * Add one repository helper to this JRHF.
   * 
   * *** IMPORTANT ***
   * I assume that some other part of the code is responsible for knowing the links
   * between keys and their JcrRepositoryHelper.  This code does not maintain the
   * list between executions. 
   * 
   * @param strKey How to re-find the helper repository
   * @param jhr    The helper repository to add.
   * @throws LockssRepositoryException
   */
  public void addRepositoryHelper(String strKey, JcrRepositoryHelper jhr) 
  throws LockssRepositoryException {
    if (m_mapdirjhr.containsKey(strKey)) {
      logger.error("Duplicate helper repository name.");
      throw new LockssRepositoryException("Duplicate helper repository name.");
    }
    
    if (k_maxHelpers <= m_mapdirjhr.size()) {
      logger.error("Too many helper repositories.");
      throw new LockssRepositoryException("Too many helper repositories.");
    }
    
    m_mapdirjhr.put(strKey, jhr);
  }
  
  
  /**
   * This method chooses a helper repository, for when you have no preference which one to use. 
   * 
   * This is a simple round-robin method to choose a helper repository. 
   * 
   * @return An arbitrary JcrRepositoryHelper.
   */
  public JcrRepositoryHelper chooseRepositoryHelper() throws LockssRepositoryException {
    Collection<JcrRepositoryHelper> coljhrValues;
    JcrRepositoryHelper[] arjhrValues = new JcrRepositoryHelper[1];
    JcrRepositoryHelper jhrReturn;
    
    coljhrValues = m_mapdirjhr.values();
    arjhrValues = (JcrRepositoryHelper []) coljhrValues.toArray(arjhrValues);
    
    if (arjhrValues.length == 0) {
      logger.error("There are no JcrHelperRepositories to choose from!");
      throw new LockssRepositoryException("There are no JcrHelperRepositories to choose from.");
    }
    
    if (m_indexNextRepositoryHelper >= arjhrValues.length) {
      m_indexNextRepositoryHelper = m_indexNextRepositoryHelper % arjhrValues.length;
    }
    
    jhrReturn = arjhrValues[m_indexNextRepositoryHelper];
    m_indexNextRepositoryHelper++;
    
    return jhrReturn;
  }
  
  /**
   * Just create a new repository helper in a given directory.
   * 
   * @param strKey  What should I call it?
   * @param directory  Where should I put it?
   * @return A new JcrRepositoryHelper.  Unless an exception happens. 
   * @throws LockssRepositoryException
   */
  public JcrRepositoryHelper createRepositoryHelper(String strKey, File directory)
  throws LockssRepositoryException {
    JcrRepositoryHelper jhr;
    
    if (m_mapdirjhr.containsKey(strKey)) {
      logger.error("Duplicate helper repository name.");
      throw new LockssRepositoryException("Duplicate helper repository name.");
    }
    
    if (k_maxHelpers <= m_mapdirjhr.size()) {
      logger.error("Too many helper repositories.");
      throw new LockssRepositoryException("Too many helper repositories.");
    }
    
    jhr = new JcrRepositoryHelper(directory, m_ld);
    m_mapdirjhr.put(strKey, jhr);
    
    return jhr;
  }
  
  
  /**
   * Retrieve a repository helper by a previously-known name.
   * 
   * This method does not create new helper repositories.
   * However, given a helper repository that it does not know, it will choose and save a
   * currently-available one.
   * 
   * *** BUG: This method does not work across executions -- it does not save the
   * <code>m_mapdirjhr</code>.
   * 
   * @param nameRepositoryHelper
   * @return The old JcrRepositoryHelper that you had seen before.
   * @throws LockssRepositoryException
   */
  
  public JcrRepositoryHelper getRepositoryHelper(String nameRepositoryHelper) throws LockssRepositoryException {
    JcrRepositoryHelper jhr;
    
    jhr = m_mapdirjhr.get(nameRepositoryHelper);
    
    if (jhr != null) {
      return jhr;
    }

    jhr = chooseRepositoryHelper();
    m_mapdirjhr.put(nameRepositoryHelper, jhr);
    
    return jhr;
  } 
  

  /**
   * Return a JcrRepositoryHelper based on its directory.
   * 
   * @param dirLocation
   * @return The JcrRepositoryHelper from <code>dirLocation</code>
   */
  public JcrRepositoryHelper getRepositoryHelperByDirectory(File dirLocation) {
    Collection<JcrRepositoryHelper> colljhr;
    JcrRepositoryHelper jhrReturn = null;
    
    colljhr = m_mapdirjhr.values();
    
    // Implementation note:
    // This search can't be done faster than O(N) without preparation
    // (for example, maintaining the set of JcrHelperRepositories in a 
    // sorted list.)  A linear search is fine, unless this method gets
    // called frequently.
    if (colljhr.size() > 0) {
      for (JcrRepositoryHelper jhr : colljhr) {
        if (jhr.getDirectory().equals(dirLocation)) {
          jhrReturn = jhr;
          break;  // Out of the 'for' loop.
        }
      }
    } else {  // colljhr.size() <= 0
      logger.error("No JcrHelperRepositories in the factory.");
    }
    
    return jhrReturn;
  }
  
  /**
   * The identity manager for this Repository Helper Factory.
   * 
   * @return The identity manager from the constructor.
   */
  public IdentityManager getIdentityManager() {
    return m_idman;
  }
  
  /**
   * The Lockss daemon for this Repository Helper Factory.
   * 
   * @return The Lockss daemon from the constructor.
   */
  public LockssDaemon getLockssDaemon() {
    return m_ld;
  }
  
  /**
   * The maximum size of a WARC file.
   * 
   * @return The maximum WARC size.
   */
  public long getSizeWarcMax() {
    return m_sizeWarcMax;
  }
  
  /**
   * This method should only be used in testing; it clears the
   * variables in the factory.
   * 
   */
  static void reset() {
    Collection<JcrRepositoryHelper> colljhr;
    Set<JcrRepositoryHelper> setjhr;

    if (sm_jrhf != null) {
      // Reset all helper repositories...
      colljhr = sm_jrhf.m_mapdirjhr.values();
     
      // ...but just once each.
      if (colljhr.size() > 0) {
        setjhr = new HashSet<JcrRepositoryHelper>();
        setjhr.addAll(colljhr);
        
        for (JcrRepositoryHelper jhr : setjhr) {
          // I do not understand how jhr can remain null, but it has happened
          // with one test.
          if (jhr != null) {
            jhr.reset();
          }
        }
      }
    }
    
    sm_jrhf = null;
  }
}
