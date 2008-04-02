/**
 * 
 */
package org.lockss.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;

import org.lockss.util.IOUtil;

/**
 * @author edwardsb
 *
 */
public class DatedPeerIdSetImpl extends PersistentPeerIdSetImpl implements
    DatedPeerIdSet {

  public static final long k_dateDefault = -1;
  
  private long m_date;
  
  /**
   * @param filePeerId
   * @param identityManager
   */
  public DatedPeerIdSetImpl(File filePeerId, IdentityManager identityManager) {
    super(filePeerId, identityManager);

    m_date = k_dateDefault;
  }

  /** (non-Javadoc)
   * @see org.lockss.protocol.DatedPeerIdSet#getDate()
   */
  public long getDate() throws IOException {
    loadIfNecessary();
    return m_date;
  }

  /* (non-Javadoc)
   * @see org.lockss.protocol.DatedPeerIdSet#setDate(java.lang.Long)
   */
  public void setDate(long l) throws IOException {
    loadIfNecessary();
    m_date = l;
    storeIfNecessary();
  }
  
  
  protected void internalLoad() throws IOException {
    DataInputStream is = null;
    try {
      if (m_filePeerId.exists()) {
        is = new DataInputStream(new FileInputStream(m_filePeerId));
        m_date = is.readLong();
        m_setPeerId = decode(is);
      } else {
        /* In the future, I recommend that this routine be allowed to
           throw a FileNotFound exception.  */
        m_date = k_dateDefault;
        m_setPeerId = new HashSet<PeerIdentity>();
      } 
    } catch (IOException e) {
      m_setPeerId = new HashSet<PeerIdentity>();
    } finally { 
      IOUtil.safeClose(is); 
    }
  }


  protected void encode(DataOutputStream dos, File filePeerIdTemp) throws IOException {
    dos.writeLong(m_date);
    super.encode(dos, filePeerIdTemp);
  }
  
}
