package org.lockss.repository;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AuSuspectUrlVersionsBean that = (AuSuspectUrlVersionsBean) o;

    return new EqualsBuilder().append(auid, that.auid)
        .append(suspectVersions, that.suspectVersions).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(auid).append(suspectVersions).toHashCode();
  }

  @Override
  public String toString() {
    return "AuSuspectUrlVersionsBean{" +
        "auid='" + auid + '\'' +
        ", suspectVersions=" + suspectVersions +
        '}';
  }
}
