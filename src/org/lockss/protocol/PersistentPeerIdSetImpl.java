/*
 * $Id: PersistentPeerIdSetImpl.java,v 1.4 2008-08-17 08:46:35 tlipkis Exp $
 */

/*
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

/* This class acts like a Set<PeerIdentity>, except that everything is
 * stored in a file.

 * There are two ways to use the class:

 * 1. If you use the class as a set, then it will load before every 
 *    operation, and save after every operation.
 * 2. If you call 'load', then it will work on the set in memory until 
 *    you call 'save'.
 */ 

/* TO DO:
 * 1. Write test cases for this class.
 * 2. Change RepositoryNodeImpl.java to use this class.
 */

/* Because this class uses files, it is inherently not thread-safe. */

package org.lockss.protocol;

import java.io.*;
import java.util.*;

import org.lockss.util.*;

public class PersistentPeerIdSetImpl implements PersistentPeerIdSet {
  // Static constants 
  protected static final String TEMP_EXTENSION = ".temp";

  // Internal variables
  protected File m_filePeerId;
  private IdentityManager m_identityManager;
  private boolean m_isInMemory;
  private static Logger m_logger = Logger.getLogger("PersistentPeerIdSet");
  protected Set<PeerIdentity> m_setPeerId;


  public PersistentPeerIdSetImpl(File filePeerId, IdentityManager identityManager) {
    m_filePeerId = filePeerId;
    m_identityManager = identityManager;
    m_isInMemory = false;
    m_setPeerId = null;
  }

  public void load() throws IOException {
    if (!m_isInMemory) {
      internalLoad();     // This sets m_setPeerId.
      m_isInMemory = true;
    }
    // No else.  It's fine to call 'load()' multiple times.
  }
  
  
  // checkpoint() just stores the set.  It does not remove the set from memory.
  public void checkpoint() throws IOException {
    if (m_isInMemory) {
      internalStore();
    }
  }

  // store() does two things:
  //   1. Store the set
  //   2. Removes the set from memory.
  public void store() throws IOException {
    if (m_isInMemory) {
      internalStore();   // This writes to m_filePeerId.
      m_isInMemory = false;
    }
    
    m_setPeerId = null;
  }

  // ... most set functions are the same: 
  //   if the set isn't in memory: 
  //      load the set
  //      perform the operation in memory
  //      save the set
  //   else (the set is in memory):
  //      perform the operation in memory
  //   end-if

  public boolean add(PeerIdentity pi) throws IOException {
    boolean result;

    loadIfNecessary();
    result = m_setPeerId.add(pi);
    storeIfNecessary();
      
    return result;
  }

  public boolean addAll(Collection<? extends PeerIdentity> cpi) throws IOException {
    boolean result;

    loadIfNecessary();
    result = m_setPeerId.addAll(cpi);
    storeIfNecessary();

    return result;
  }


  public void clear() throws IOException {
    loadIfNecessary();
    m_setPeerId.clear();
    storeIfNecessary();
  }


  public boolean contains(Object o) throws IOException {
    boolean result;

    loadIfNecessary();
    result = m_setPeerId.contains(o);
    // saveIfNecessary();  // Not needed -- it makes no change.

    return result;
  }


  public boolean containsAll(Collection<?> co) throws IOException {
    boolean result;

    loadIfNecessary();
    result = m_setPeerId.containsAll(co);
    // saveIfNecessary();  // Not needed -- it makes no change

    return result;
  }


  // One exception is equals.
  public boolean equals(Object o) {
    if (o instanceof PersistentPeerIdSetImpl) {
      PersistentPeerIdSetImpl ppis = (PersistentPeerIdSetImpl) o;

      return m_filePeerId.equals(ppis.m_filePeerId);
    } else {
      return false;
    }
  }


  /* A hash code must always return a value; it cannot throw an IOException. */
  public int hashCode() {
    int result;

    try {
      loadIfNecessary();
      result = m_setPeerId.hashCode();
      // saveIfNecessary();  // Not needed.
    } catch (IOException e) {
      m_logger.error("hashCode had an IOException: " + e.getMessage());
      result = 0;
    }

    return result;
  }


  public boolean isEmpty() throws IOException {
    boolean result;
    
    loadIfNecessary();
    result = m_setPeerId.isEmpty();
    // saveIfNecessary();  // Not needed.

    return result;
  }


  // Another exception is iterator:

  public Iterator<PeerIdentity> iterator() {
    if (! m_isInMemory) {
      throw new UnsupportedOperationException("An iterator can only be returned when the set is in memory." +
          "To fix this error, please call 'load()' before you call 'iterator()', and call 'store()' when " +
          "you're done with the iterator.");
    } else {
      return m_setPeerId.iterator();
    }
  }


  public boolean remove(Object o) throws IOException {
    boolean result;

    loadIfNecessary();
    result = m_setPeerId.remove(o);
    storeIfNecessary();

    return result;
  }


  public boolean removeAll(Collection<?> c) throws IOException {
    boolean result;

    loadIfNecessary();
    result = m_setPeerId.removeAll(c);
    storeIfNecessary();

    return result;
  }


  public boolean retainAll(Collection<?> c) throws IOException {
    boolean result;

    loadIfNecessary();
    result = m_setPeerId.retainAll(c);
    storeIfNecessary();

    return result;
  }


  public int size() throws IOException {
    int result;

    loadIfNecessary();
    result = m_setPeerId.size();
    // saveIfNecessary();  // Not needed.

    return result;
  }


  public Object[] toArray() throws IOException {
    Object[] result;
    
    loadIfNecessary();
    result = m_setPeerId.toArray();
    // saveIfNecessary();  // Not needed.

    return result;
  }


//  public <T> T[] toArray(T[] a) throws IOException {
//    T[] result;
//
//    loadIfNecessary();
//    result = m_setPeerId.toArray(a);
//    saveIfNecessary();
//
//    return result;
//  }

    

  // ---- Internal methods

  /* 
   * This method is based on RepositoryNodeImpl.loadAgreementHistory() 
   *
   * 
   * Return a set of PeerIdentity keys that have agreed with this node.
   *
   */

  protected void internalLoad() throws IOException {
    DataInputStream is = null;
    try {
      if (m_filePeerId.exists()) {
        is = new DataInputStream(new FileInputStream(m_filePeerId));
        m_setPeerId = decode(is);
      } else {
	/* In the future, I recommend that this routine be allowed to
	   throw a FileNotFound exception.  */
	m_setPeerId = new HashSet<PeerIdentity>();
      } 
    } catch (IOException e) {
      m_setPeerId = new HashSet<PeerIdentity>();
    } finally { 
      IOUtil.safeClose(is); 
    }
  }

  
  /**
   * @throws IOException
   */
  protected void loadIfNecessary() throws IOException {
    if (! m_isInMemory) {
      internalLoad();
    }
  }


  /*
   * This method is based on RepositoryNodeImpl.storeAgreementHistory().
   *
   * Store the list of agreement histories.
   */

  protected void internalStore() throws IOException {
    DataOutputStream dos = null;
    File filePeerIdTemp =
      FileUtil.createTempFile(m_filePeerId.getName(), TEMP_EXTENSION,
			      m_filePeerId.getParentFile());
    try {
      // Loop until there are no IdentityParseExceptions
      OutputStream fileOs = new FileOutputStream(filePeerIdTemp);
      dos = new DataOutputStream(new BufferedOutputStream(fileOs));
      encode(dos);
      dos.close();

      if (!PlatformUtil.updateAtomically(filePeerIdTemp, m_filePeerId)) {
        m_logger.error("Unable to rename temporary agreement history file " +
            filePeerIdTemp);
      }

    } finally {
      IOUtil.safeClose(dos);
      filePeerIdTemp.delete();
    }
  }

  /**
   * @param dos
   * @throws IOException
   */
  protected void encode(DataOutputStream dos) throws IOException {
    byte [] TCPKey = null;
    boolean errors = false;
    
    outer:
    do {
      if (m_setPeerId != null) {
        for (PeerIdentity key : m_setPeerId) {
          if (key == null) {
            m_logger.error("Null key among the peer set.");
            continue;
          }
          
          try {
            TCPKey = IDUtil.encodeTCPKey(key.getIdString());
          } catch (IdentityParseException ex) {
            m_logger.error("Unable to store identity key: " + key + ".  Identity Parse Exception: " + ex.getMessage());
            // Delete the offending, un-storable key
            m_setPeerId.remove(key);
            break outer;
          }
          
          if (key != null) {
            dos.write(TCPKey);
          } else {  // key is null
            m_logger.error("An identity key is null.  Ignoring.");
            m_setPeerId.remove(key);
          }
        }      
        errors = false;
      } 
   } while (errors);
  }

  /**
   * @throws IOException
   */
  protected void storeIfNecessary() throws IOException {
    if (! m_isInMemory) {
      internalStore();
      m_setPeerId = null;
    }
  }

  
  /** Consume the input stream, decoding peer identity keys  */
  /* Corresponds to RepositoryNodeImpl.decodeAgreementHistory(). */
  protected Set<PeerIdentity> decode(DataInputStream is)
  {
    Set<PeerIdentity> history = new HashSet<PeerIdentity>();
    String id;
    PeerIdentity pi;
    try {
      while ((id = IDUtil.decodeOneKey(is)) != null) {
        pi = m_identityManager.findPeerIdentity(id);
        if (pi != null) {
          history.add(pi);
        } else {
          // This error can only happen in tests.  It is not possible when using real identity managers.
          m_logger.error("Finding error while trying to find argument " + id + " in the identity manager.  Did you include idmgr.addPeerIdentity(...) for this id?");
        }
      }
    } catch (IdentityParseException ex) {
      // IDUtil.decodeOneKey will do its best to leave us at the
      // start of the next key, but there's no guarantee.  All we can
      // do here is log the fact that there was an error, and try
      // again.
      m_logger.error("Parse error while trying to decode agreement " +
                   "history file " + m_filePeerId + ": " + ex);
    }
    return history;
  }
}


