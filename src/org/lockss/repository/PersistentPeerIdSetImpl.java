/*
 * $Id: PersistentPeerIdSetImpl.java,v 1.1 2008-02-19 23:33:10 edwardsb1 Exp $
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

package org.lockss.repository;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.lockss.protocol.IdentityManager;
import org.lockss.protocol.PeerIdentity;
import org.lockss.repository.LockssRepository;
import org.lockss.util.IdentityParseException;
import org.lockss.util.IDUtil;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.PlatformUtil;
import org.lockss.util.UrlUtil;

public class PersistentPeerIdSetImpl implements PersistentPeerIdSet {
  // Static constants 
  private static final String TEMP_EXTENSION = ".temp";

  // Internal variables
  private File m_filePeerId;
  private File m_filePeerIdTemp;
  private IdentityManager m_identityManager;
  private boolean m_isInMemory;
  private static Logger m_logger = Logger.getLogger("PersistentPeerIdSet");
  private Set<PeerIdentity> m_setPeerId;


  public PersistentPeerIdSetImpl(File filePeerId, IdentityManager identityManager) {
    m_filePeerId = filePeerId;
    m_filePeerIdTemp = new File(filePeerId.getAbsolutePath() + TEMP_EXTENSION);
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

  // store() does two things:
  //   1. Store the set
  //   2. Removes the set from memory.
  public void store() throws IOException {
    internalSave();   // This writes to m_filePeerId.
    m_isInMemory = false;
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

    if (! m_isInMemory) {
      internalLoad();
    }

    result = m_setPeerId.add(pi);
      
    if (! m_isInMemory) {
      internalSave();
      m_setPeerId = null;
    }
      
    return result;
  }


  public boolean addAll(Collection<? extends PeerIdentity> cpi) throws IOException {
    boolean result;

    if (! m_isInMemory) {
      internalLoad();
    }

    result = m_setPeerId.addAll(cpi);

    if (! m_isInMemory) {
      internalSave();
      m_setPeerId = null;
    }

    return result;
  }


  public void clear() throws IOException {
    if (! m_isInMemory) {
      internalLoad();
    }

    m_setPeerId.clear();

    if (! m_isInMemory) {
      internalSave();
      m_setPeerId = null;
    }
  }


  public boolean contains(Object o) throws IOException {
    boolean result;

    if (! m_isInMemory) {
      internalLoad();
    }

    result = m_setPeerId.contains(o);
    
    if (! m_isInMemory) {
      internalSave();
      m_setPeerId = null;
    }

    return result;
  }


  public boolean containsAll(Collection<?> co) throws IOException {
    boolean result;

    if (! m_isInMemory) {
      internalLoad();
    }

    result = m_setPeerId.containsAll(co);
      
    if (! m_isInMemory) {
      internalSave();
      m_setPeerId = null;
    }

    return result;
  }


  // One exception is equals.
  public boolean equals(Object o) {
    boolean result;

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
      if (! m_isInMemory) {
	internalLoad();
      }

      result = m_setPeerId.hashCode();

      if (! m_isInMemory) {
	internalSave();
	m_setPeerId = null;
      }
    } catch (IOException e) {
      m_logger.error("hashCode had an IOException: " + e.getMessage());
      result = 0;
    }

    return result;
  }


  public boolean isEmpty() throws IOException {
    boolean result;
    
    if (! m_isInMemory) {
      internalLoad();
    }

    result = m_setPeerId.isEmpty();

    if (! m_isInMemory) {
      internalSave();
      m_setPeerId = null;
    }

    return result;
  }


  // Another exception is iterator:

  public Iterator<PeerIdentity> iterator() {
    if (! m_isInMemory) {
      throw new UnsupportedOperationException("An iterator can only be returned when the set is in memory." +
          "To fix this error, please call 'load()' before you call 'iterator()', and call 'store' when " +
          "you're done with the iterator.");
    } else {
      return m_setPeerId.iterator();
    }
  }


  public boolean remove(Object o) throws IOException {
    boolean result;

    if (! m_isInMemory) {
      internalLoad();
    }

    result = m_setPeerId.remove(o);

    if (! m_isInMemory) {
      internalSave();
      m_setPeerId = null;
    }

    return result;
  }


  public boolean removeAll(Collection<?> c) throws IOException {
    boolean result;

    if (! m_isInMemory) {
      internalLoad();
    }

    result = m_setPeerId.removeAll(c);

    if (! m_isInMemory) {
      internalSave();
      m_setPeerId = null;
    }

    return result;
  }


  public boolean retainAll(Collection<?> c) throws IOException {
    boolean result;

    if (! m_isInMemory) {
      internalLoad();
    }

    result = m_setPeerId.retainAll(c);

    if (! m_isInMemory) {
      internalSave();
      m_setPeerId = null;
    }

    return result;
  }


  public int size() throws IOException {
    int result;

    if (! m_isInMemory) {
      internalLoad();
    }

    result = m_setPeerId.size();
    
    if (! m_isInMemory) {
      internalSave();
      m_setPeerId = null;
    }

    return result;
  }


  public Object[] toArray() throws IOException {
    Object[] result;
    
    if (! m_isInMemory) {
      internalLoad();
    }

    result = m_setPeerId.toArray();

    if (! m_isInMemory) {
      internalSave();
      m_setPeerId = null;
    }

    return result;
  }


//  public <T> T[] toArray(T[] a) throws IOException {
//    T[] result;
//
//    if (! m_isInMemory) {
//      internalLoad();
//    }
//
//    result = m_setPeerId.toArray(a);
//
//    if (! m_isInMemory) {
//      internalSave();
//      m_setPeerId = null;
//    }
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

  private synchronized void internalLoad() throws IOException {
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
    } catch (Exception e) {
      m_logger.error("Error loading agreement history", e); 
      throw new LockssRepository.RepositoryStateException("Couldn't load agreement file."); 
    } finally { 
      IOUtil.safeClose(is); 
    }
  }


  /*
   * This method is based on RepositoryNodeImpl.storeAgreementHistory().
   *
   * Store the list of agreement histories.
   */

  private synchronized void internalSave() throws IOException {
    DataOutputStream dos = null;
    Iterator<PeerIdentity> it;

    try {
      // Loop until there are no IdentityParseExceptions
      boolean errors = false;
      
      outer:
      do {
        dos = new DataOutputStream(new FileOutputStream(m_filePeerIdTemp));

        if (m_setPeerId != null) {
          for (it = m_setPeerId.iterator(); it.hasNext(); ) {
            PeerIdentity key = it.next();
          
            try {
              if (key != null) {
                dos.write(IDUtil.encodeTCPKey(key.getIdString()));
              } else {  // key is null
                m_logger.error("An identity key is null.  Ignoring.");
                m_setPeerId.remove(key);
              }
            } catch (IdentityParseException ex) {
              m_logger.error("Unable to store identity key: " + key + ".  Identity Parse Exception: " + ex.getMessage());
              //  Close the errored file.
              IOUtil.safeClose(dos);
              // Set the error flag
              errors = true;
              // Delete the offending, un-storable key
              m_setPeerId.remove(key);
              break outer;
            }
          }      
          errors = false;
          if (!PlatformUtil.updateAtomically(m_filePeerIdTemp, m_filePeerId)) {
            m_logger.error("Unable to rename temporary agreement history file " +
                m_filePeerIdTemp);
          }
        } 
     } while (errors);
     
    } finally {
      IOUtil.safeClose(dos);
    }
  }

  /** Consume the input stream, decoding peer identity keys  */
  /* Corresponds to RepositoryNodeImpl.decodeAgreementHistory(). */
  private Set<PeerIdentity> decode(DataInputStream is)
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


  /* Rename a potentially corrupt agreement history file */
  /* Corresponds to RepositoryNodeImpl.backupAgreementHistoryFile. */
  private void backup()
  {
    try {
      PlatformUtil.updateAtomically(m_filePeerId,
                                    new File(m_filePeerId.getCanonicalFile() + ".old"
));
    } catch (IOException ex) {
      // This would only be caused by getCanonicalFile() throwing IOException.
      // Worthy of a stack trace.
      m_logger.error("Unable to back-up suspect agreement history file:", ex);
    }
  }

}


