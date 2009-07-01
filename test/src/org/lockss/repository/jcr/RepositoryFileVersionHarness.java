/**
 * 
 */
package org.lockss.repository.jcr;

import javax.jcr.*;

import org.lockss.repository.*;
import org.lockss.repository.v2.*;

/**
 * @author edwardsb
 *
 */
public interface RepositoryFileVersionHarness extends RepositoryFileVersion {
  /**
   * You can set the content through input streams. You can also set the content
   * through byte arrays.
   */
  public void setContent(byte[] arbyContent) throws LockssRepositoryException;

  /**
   * You can retrieve the content as a byte array, as well.
   */
  public byte[] getContent() throws LockssRepositoryException;
  
  /**
   * This method is used by 'cleanRepositoryFile'.
   */
  public void clearTempContent();
}
