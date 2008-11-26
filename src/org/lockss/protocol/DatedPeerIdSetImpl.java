/**
 * 
 */
package org.lockss.protocol;

import java.io.*;
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
    if (m_date != l) {
      m_date = l;
      m_changed = true;
      storeIfNecessary();
    }
  }
  
  @Override
  protected void readData(DataInputStream is) throws IOException {
    m_date = is.readLong();
    super.readData(is);
  }

  @Override
  protected void newData() throws IOException {
    m_date = k_dateDefault;
    super.newData();
  }

  @Override
  protected void writeData(DataOutputStream dos) throws IOException {
    dos.writeLong(m_date);
    super.writeData(dos);
  }
}
