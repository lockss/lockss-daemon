/**
 * 
 */
package org.lockss.repository.jcr;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Set;

import org.lockss.protocol.PeerIdentity;
import org.lockss.state.NodeState;

/**
 * @author edwardsb
 *
 */

public interface RepositoryFile {
  /**
   * Stores PollHistories for a given NodeState.
   */
  public void storePollHistories(NodeState nodeState) ;
  
  /**
   * Returns the value within the RepositoryFile.
   */
  public InputStream getInputStream() ;
  
  /**
   * Return an <code>OutputStream</code> object which writes to a new version in the cache.
   * @return an <code>OutputStream</code> to which the new contents can be written.
   * @see RepositoryNode#makeNewVersion()
   */
  public OutputStream getOutputStream() ;
  
  /**
   * Does the node have stored content?
   */
  public boolean hasContent() ;
  
  /**
   * Determines whether this node is deleted.  Deleted nodes may have old content or children, but will appear in the list of nodes only when explicitly asked for.
   */
  public boolean isDeleted() ;
  
  /**
   * Mark a node as deleted.  To reactivate, call <code>undelete</code> .
   */
  public void delete() ;
  
  /**
   * This method marks the node as no longer deleted.  This method also reactivates content.
   */
  public void undelete() ;
  
  /**
   * After "getOutputStream()" has been called and the output stream has been filled, this operation saves the file in the storage.  In previous versions, this method was called "seal".
   * See also: <code>rollback</code>.
   */
  public void commit() ;
  
  /**
   * After "getOutputStream()" has been called, this method undoes the changes and returns to the last saved version.
   * See also: <code>commit</code>.
   */
  public void rollback() ;
  
  /**
   * Returns the size of the current version of stored cache. 
   */
  public long getContentSize() ;
  
  /**
   * Returns the properties within a RepositoryFile.
   */
  public Properties getProperties() ;
  
  /**
   * Returns the list of peer id sets that agree with a particular file.
   */
  public Set<PeerIdentity> getAgreeingPeerIdSet() ;
  
  /**
   * Returns the version number of this version.
   * @return the current version
   */
  public int getVersion() ;
}
