package org.lockss.protocol.psm;

import java.io.Serializable;

/**
 * <p>
 * Maintain the serializable state for an instance of a PsmInterp.
 * </p>
 */
public class PsmInterpStateBean implements Serializable {
  private String lastRestorableStateName;

  public String getLastRestorableStateName() {
    return lastRestorableStateName;
  }

  public void setLastRestorableStateName(String lastRestorableStateName) {
    this.lastRestorableStateName = lastRestorableStateName;
  }
}
