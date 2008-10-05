/**
 * 
 */
package org.lockss.repository.jcr;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.protocol.PeerIdentity;

/**
 * @author edwardsb
 *
 */
/**
 * A RepositoryNode is a collection of versions of RepositoryFiles.
 */
public interface RepositoryNode {
  /**
   * Returns the number of children at this node.
   * @param includeDeleted: If true, include the deleted nodes in the count.
   */
  public int getChildCount(/* includeDeleted = false */);
  public int getChildCount(boolean includeDeleted);
  
  /**
   * Returns the size of the current version of stored cache.  
   * @param mostRecentOnly: If true, then only consider the most recent version.  If false, then compute for ALL versions.
   * @param includeDeleted: If true, then include deleted values.  If false, then do not include deleted values.
   */
  public long getContentSize(/* mostRecentOnly = true , includeDeleted = false */);  
  public long getContentSize(boolean mostRecentOnly /*, includeDeleted = false */);  
  public long getContentSize(boolean mostRecentOnly, boolean includeDeleted);
  
  /**
   * Returns the size of the content tree under (and including) this cache in bytes.
   * @param filter: Whether to filter the URLs being searched.
   * @param calcIfUnknown: If true, then calculate what's not known. 
   * @param mostRecentOnly: If true, then only consider the most recent version.  If false, then compute for ALL versions.
   * @param includeDeleted: If true, then include deleted values.  If false, then do not include deleted values.
   */
  public long getTreeContentSize(CachedUrlSetSpec filter, boolean calcIfUnknown /*, boolean mostRecentOnly = true , boolean includeDeleted = false */); ;
  public long getTreeContentSize(CachedUrlSetSpec filter, boolean calcIfUnknown, boolean mostRecentOnly /*, boolean includeDeleted = false */); ;
  public long getTreeContentSize(CachedUrlSetSpec filter, boolean calcIfUnknown, boolean mostRecentOnly, boolean includeDeleted); ;
  
  /**
   * Asembles a list of immediate children, possibly filtered.  Sorted alphabetically by File.compareTo()
   * @param filter: a spec to filter on.  Null for no filtering.
   * @param includeInactive: true iff inactive nodes should be included.
   * @return the list of child RepositoryNodes.
   * 
   * Note from Brent: I do not understand why "listChildren" and "getNodeList" should both exist.
   */
  public List<RepositoryFile> getFileList(CachedUrlSetSpec filter /*, boolean includeDeleted = false */) ;
  public List<RepositoryFile> getFileList(CachedUrlSetSpec filter, boolean includeDeleted) ;

  /**
   * Return the URL represented by this node.
   */
  public String getNodeUrl() ;
  
  /**
   * Returns the current version.  This is the open version when writing, and the one accessed by getNodeInfo().
   * 
   * @return The current version.  If the node is inactive, return -1.
   */
  public int getCurrentVersion() ;
  
  /**
   * Retrun an array of RepositoryFile for all versions of 
   * content/props at this node.  The result is sorted from most
   * to least recent: the current version is the first element in the array.
   */
  public RepositoryFile[] getFiles(/* boolean includeDeleted = false */) ;
  public RepositoryFile[] getFiles(/* int maxVersion = Integer.MAX_VALUE, */ boolean includeDeleted) ;
  public RepositoryFile[] getFiles(int maxVersion /*, boolean includeDeleted = false */);
  public RepositoryFile[] getFiles(int maxVersion, boolean includeDeleted);
  
  /**
   * Get the list of Peer Identities that agree with this RepositoryNode.
   */
  public Set<PeerIdentity> getAgreeingPeerIdSet() ;

  /**
   * Set the list of Peer Identities that agree with this RepositoryNode.
   */
  public void setAgreeingPeerIdSet(Set<PeerIdentity> peerIds) ;

  /**
   * Creates a new version, and returns the associated RepositoryFile.
   */
  public RepositoryFile makeNewRepositoryFile() ;
  
  /**
   * Stores the properties for the full node.
   * @param newProps a <code>Properties</code> object containing the headers of the new version being cached.
   */
  public void setProperties(Properties newProps) ;
  
  /**
   * This method marks a whole node as deleted.  (It does not actually delete the node.)
   */
  public void delete() ;
  
  /**
   * Returns whether the whole of this node has been marked as deleted.
   */
  public boolean isDeleted() ;
  
  /**
   * If a whole node is marked as deleted, this method removes the mark.
   */
  public void undelete() ;
}
