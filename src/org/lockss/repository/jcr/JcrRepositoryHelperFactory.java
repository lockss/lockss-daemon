/*
 * $Id: JcrRepositoryHelperFactory.java,v 1.1.2.1 2009-09-30 23:02:32 edwardsb1 Exp $
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
 * @author edwardsb
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
  private int m_indexNextHelperRepository = 0;
  private LockssDaemon m_ld;
  // Only one copy of each JcrRepositoryHelper, please...
  private Map<String /* Directory */, JcrRepositoryHelper> m_mapdirjhr = new HashMap<String, JcrRepositoryHelper>();
  private long m_sizeWarcMax;
  
  private JcrRepositoryHelperFactory(long sizeWarcMax, IdentityManager idman, LockssDaemon ld) {
    m_sizeWarcMax = sizeWarcMax;
    m_idman = idman;
    m_ld = ld;
  }
  
  // Any suggested names for this method?  It must be called before the call
  // to getSingleton.
  
  public static void preconstructor(long sizeWarcMax, IdentityManager idman, LockssDaemon ld) 
  throws LockssRepositoryException {
    if (sm_jrhf == null) {
      sm_jrhf = new JcrRepositoryHelperFactory(sizeWarcMax, idman, ld);
    } else {
      logger.error("JcrRepositoryHelperFactory: the preconstructor was called multiple times.");
      throw new LockssRepositoryException("Don't call JcrRepositoryHelperFactory.preconstructor more than once.");
    }
  }
  
  
  public static boolean isPreconstructed() {
    return sm_jrhf != null;
  }
  
  
  public static JcrRepositoryHelperFactory getSingleton() throws LockssRepositoryException {
    if (sm_jrhf != null) {
      return sm_jrhf;
    }
    
    logger.error("JcrRepositoryHelperFactory: call the preconstructor before you get a singleton.");
    throw new LockssRepositoryException("Call preconstructor before you get the singleton.");
  }
  
  
  public void addHelperRepository(String strKey, JcrRepositoryHelper jhr) 
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
  
  
  /* This method chooses a helper repository, for when you have no preference which one to use. 
   * 
   * This is a simple round-robin method to choose a helper repository. 
   * */
  public JcrRepositoryHelper chooseHelperRepository() throws LockssRepositoryException {
    Collection<JcrRepositoryHelper> coljhrValues;
    JcrRepositoryHelper[] arjhrValues = new JcrRepositoryHelper[1];
    JcrRepositoryHelper jhrReturn;
    
    coljhrValues = m_mapdirjhr.values();
    arjhrValues = (JcrRepositoryHelper []) coljhrValues.toArray(arjhrValues);
    
    if (arjhrValues.length == 0) {
      logger.error("There are no JcrHelperRepositories to choose from!");
      throw new LockssRepositoryException("There are no JcrHelperRepositories to choose from.");
    }
    
    if (m_indexNextHelperRepository >= arjhrValues.length) {
      m_indexNextHelperRepository = m_indexNextHelperRepository % arjhrValues.length;
    }
    
    jhrReturn = arjhrValues[m_indexNextHelperRepository];
    m_indexNextHelperRepository++;
    
    return jhrReturn;
  }
  

  public JcrRepositoryHelper createHelperRepository(String strKey, File directory)
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
    
    jhr = new JcrRepositoryHelper(directory, m_sizeWarcMax, m_idman, m_ld);
    m_mapdirjhr.put(strKey, jhr);
    
    return jhr;
  }
  
  
  // IMPORTANT NOTE: This method does not create new helper repositories.
  // (An earlier version of this method did.)
  // However, given a helper repository that it does not know, it will choose and save a
  // currently-available one.
  // You will need to create helper repositories as part of the construction.
  
  public JcrRepositoryHelper getHelperRepository(String nameHelperRepository) throws LockssRepositoryException {
    JcrRepositoryHelper jhr;
    
    jhr = m_mapdirjhr.get(nameHelperRepository);
    
    if (jhr != null) {
      return jhr;
    }

    jhr = chooseHelperRepository();
    m_mapdirjhr.put(nameHelperRepository, jhr);
    
    return jhr;
  } 
  
  // Note: This method returns 'null' if no helper repository is found.
  public JcrRepositoryHelper getHelperRepositoryByDirectory(File dirLocation) {
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
  
  
  public IdentityManager getIdentityManager() {
    return m_idman;
  }
  
  
  public LockssDaemon getLockssDaemon() {
    return m_ld;
  }
  
  
  public long getSizeWarcMax() {
    return m_sizeWarcMax;
  }
  
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
