/*
 * $Id: JcrHelperRepositoryFactory.java,v 1.1.2.1 2009-09-23 02:03:02 edwardsb1 Exp $
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
public class JcrHelperRepositoryFactory {
  // Static variables
  private static JcrHelperRepositoryFactory sm_jhrf = null;
  private static Logger logger = Logger.getLogger("JcrHelperRepositoryFactory");

  // Class variables
  private IdentityManager m_idman;
  private int m_indexNextHelperRepository = 0;
  private LockssDaemon m_ld;
  // Only one copy of each JcrHelperRepository, please...
  private Map<String /* Directory */, JcrHelperRepository> m_mapdirjhr = new HashMap<String, JcrHelperRepository>();
  private long m_sizeWarcMax;
  
  private JcrHelperRepositoryFactory(long sizeWarcMax, IdentityManager idman, LockssDaemon ld) {
    m_sizeWarcMax = sizeWarcMax;
    m_idman = idman;
    m_ld = ld;
  }
  
  
  public static void preconstructor(long sizeWarcMax, IdentityManager idman, LockssDaemon ld) 
  throws LockssRepositoryException {
    if (sm_jhrf == null) {
      sm_jhrf = new JcrHelperRepositoryFactory(sizeWarcMax, idman, ld);
    } else {
      logger.error("JcrHelperRepositoryFactory: the preconstructor was called multiple times.");
      throw new LockssRepositoryException("Don't call JcrHelperRepositoryFactory.preconstructor more than once.");
    }
  }
  
  
  public static boolean isPreconstructed() {
    return sm_jhrf != null;
  }
  
  
  public static JcrHelperRepositoryFactory constructor() throws LockssRepositoryException {
    if (sm_jhrf != null) {
      return sm_jhrf;
    }
    
    logger.error("JcrHelperRepositoryFactory: call the preconstructor before you call the constructor.");
    throw new LockssRepositoryException("Call preconstructor before you call the constructor.");
  }
  
  
  public void addHelperRepository(String strKey, JcrHelperRepository jhr) 
  throws LockssRepositoryException {
    if (m_mapdirjhr.containsKey(strKey)) {
      logger.error("Duplicate helper repository name.");
      throw new LockssRepositoryException("Duplicate helper repository name.");
    }
    
    m_mapdirjhr.put(strKey, jhr);
  }
  
  
  /* This method chooses a helper repository, for when you have no preference which one to use. 
   * 
   * This is a simple round-robin method to choose a helper repository. 
   * */
  public JcrHelperRepository chooseHelperRepository() throws LockssRepositoryException {
    Collection<JcrHelperRepository> coljhrValues;
    JcrHelperRepository[] arjhrValues = new JcrHelperRepository[1];
    JcrHelperRepository jhrReturn;
    
    coljhrValues = m_mapdirjhr.values();
    arjhrValues = (JcrHelperRepository []) coljhrValues.toArray(arjhrValues);
    
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
  

  public JcrHelperRepository createHelperRepository(String strKey, File directory)
  throws LockssRepositoryException {
    JcrHelperRepository jhr;
    
    if (m_mapdirjhr.containsKey(strKey)) {
      logger.error("Duplicate helper repository name.");
      throw new LockssRepositoryException("Duplicate helper repository name.");
    }
    
    jhr = new JcrHelperRepository(directory, m_sizeWarcMax, m_idman, m_ld);
    m_mapdirjhr.put(strKey, jhr);
    
    return jhr;
  }
  
  
  // IMPORTANT NOTE: This method does not create new helper repositories.
  // (An earlier version of this method did.)
  // However, given a helper repository that it does not know, it will choose and save a
  // currently-available one.
  // You will need to create helper repositories as part of the construction.
  
  public JcrHelperRepository getHelperRepository(String nameHelperRepository) throws LockssRepositoryException {
    JcrHelperRepository jhr;
    
    jhr = m_mapdirjhr.get(nameHelperRepository);
    
    if (jhr != null) {
      return jhr;
    }

    jhr = chooseHelperRepository();
    m_mapdirjhr.put(nameHelperRepository, jhr);
    
    return jhr;
  } 
  
  // Note: This method returns 'null' if no helper repository is found.
  public JcrHelperRepository getHelperRepositoryByDirectory(File dirLocation) {
    Collection<JcrHelperRepository> colljhr;
    JcrHelperRepository jhrReturn = null;
    
    colljhr = m_mapdirjhr.values();
    
    // Implementation note:
    // This search can't be done faster than O(N) without preparation
    // (for example, maintaining the set of JcrHelperRepositories in a 
    // sorted list.)  A linear search is fine, unless this method gets
    // called frequently.
    if (colljhr.size() > 0) {
      for (JcrHelperRepository jhr : colljhr) {
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
  
  static public void reset() {
    Collection<JcrHelperRepository> colljhr;
    Set<JcrHelperRepository> setjhr;

    if (sm_jhrf != null) {
      // Reset all helper repositories...
      colljhr = sm_jhrf.m_mapdirjhr.values();
     
      // ...but just once each.
      if (colljhr.size() > 0) {
        setjhr = new HashSet<JcrHelperRepository>();
        setjhr.addAll(colljhr);
        
        for (JcrHelperRepository jhr : setjhr) {
          // I do not understand how jhr can remain null, but it has happened
          // with one test.
          if (jhr != null) {
            jhr.reset();
          }
        }
      }
    }
    
    sm_jhrf = null;
  }
}
