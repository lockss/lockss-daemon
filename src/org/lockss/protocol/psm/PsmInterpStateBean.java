package org.lockss.protocol.psm;

import org.lockss.util.*;

/**
 * <p>
 * Maintain the serializable state for an instance of a PsmInterp.
 * </p>
 */
public class PsmInterpStateBean implements LockssSerializable {
  private String lastRestorableStateName;
  
  public String getLastRestorableStateName() {
    return lastRestorableStateName;
  }
  
  public void setLastRestorableStateName(String lastRestorableStateName) {
    this.lastRestorableStateName = lastRestorableStateName;
  }
}
