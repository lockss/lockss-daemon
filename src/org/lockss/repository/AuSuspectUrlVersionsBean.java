package org.lockss.repository;

import java.util.HashSet;
import java.util.Set;

public class AuSuspectUrlVersionsBean {
  private String auid;

  private Set<AuSuspectUrlVersions.SuspectUrlVersion> suspectVersions =
      new HashSet<AuSuspectUrlVersions.SuspectUrlVersion>();

  public AuSuspectUrlVersionsBean(String auId, Set<AuSuspectUrlVersions.SuspectUrlVersion> versions) {
    this.auid = auId;
    this.suspectVersions = versions;
  }

  public void setAuid(String auid) {
    this.auid = auid;
  }

  public void setSuspectVersions(Set<AuSuspectUrlVersions.SuspectUrlVersion> suspectVersions) {
    this.suspectVersions = suspectVersions;
  }
}
