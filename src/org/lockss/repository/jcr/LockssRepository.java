/**
 * 
 */
package org.lockss.repository.jcr;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;

import org.lockss.app.LockssAuManager;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.repository.RepositoryNode;
import org.lockss.state.AuState;
import org.lockss.state.DamagedNodeSet;
import org.lockss.state.NodeState;

/**
 * @author edwardsb
 *
 */
public interface LockssRepository extends LockssAuManager {
  /**
   * This method sets a "deleted" marker on the URL.  It is not actually deleted. 
   */
  public void deteteNode(String url) throws MalformedURLException;

  /**
   * This method unsets the "deleted" marker on the URL.   
   */
  public void undeleteNode(String url) throws MalformedURLException;

  /**
   * Returns a {@link RepositoryNode} that represents the URL in question.
   * This method only returns URLs that exist in the cache.  It's null otherwise.
   */
  public RepositoryNode getNode(String url, boolean create) throws MalformedURLException;
  
  /**
   * Traverses the node hierarchy and tests internal state consistency for each
   * node.  Corrects and logs any correctable errors it encounters.
   */
  public void checkConsistency();

  /**
   * from RepositoryManager.queueSizeCalc: 
   * "engqueue a size calculation for the node"
   */
  public void queueSizeCalc(RepositoryNode node);

  /**
   * This method sets the value of a node, creating it if necessary.
   */
  public void setNode(String url, RepositoryNode node);

  /**
   * Return the date/time the au was created.
   * 
   * Note from BEE: this method returns a long, probably the number of seconds since the epoch.
   */
  public long getAuCreationTime();

  /**
   * Return AuStateFile for this AU
   */
  public File getAuStateFile();

  /**
   * Both loads and returns the AuState for this ArchivalUnit.
   */
  public AuState loadAuState();

  /**
   * Puts the AuState onto the disk.
   */
  public void storeAuState(AuState auState);

  /**
   * Loads and returns the damaged node set for this operation.
   */
  public DamagedNodeSet loadDamagedNodeSet();

  /**
   * Stores the DamagedNodeSet for this ArchivalUnit.
   */
  public void storeDamagedNodeSet(DamagedNodeSet dns);

  /**
   * This method returns the current Identity Agreement file.
   * If one wasn't set, this method returns NULL.
   */
  public File getIdentityAgreementFile();

  /**
   * This method loads and returns the identity list.
   * 
   * Note: I have no clue what type is the list.
   */
  public List loadIdentityAgreements();

  /**
   * Stores IdentityAgreement list.
   */
  public void storeIdentityAgreements(List identAgreements);

  /**
   * Loads the current node state info, except the histories.  Returns a new NodeState if none found.
   */
  public NodeState loadNodeState(CachedUrlSet cus);

  /**
   * Stores the current node state info, except the histories.
   */
  public void storeNodeState(NodeState nodeState);

  /**
   * Loads the poll histories into the given NodeState.
   */
  public void loadPollHistories(NodeState nodeState);
}
