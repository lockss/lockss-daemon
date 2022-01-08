/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.protocol;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

import org.lockss.plugin.AuUtil;
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

  /**
   * Creates and provides a new instance with the named peers.
   *
   * @param auId The au id needed to fill out bean.
   * @return a PersistentPeerIdSetImpl with the newly created object.
   */
  public DatedPeerIdSetBean getBean(String auId) {
    Set<String> rs = new HashSet<>();;
    for (PeerIdentity pid : m_setPeerId) {
      rs.add(pid.getKey());
    }
    DatedPeerIdSetBean res = new DatedPeerIdSetBean(auId, rs, m_date);
    return res;
  }
}
