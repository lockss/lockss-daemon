package org.lockss.protocol;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class DatedPeerIdSetBean {
  private String auid;
  private Set<String> rawSet = new HashSet<>();
  private long date;


  public DatedPeerIdSetBean() {
  }

  public DatedPeerIdSetBean(String auId, Set<String> rawSet, long date) {
    this.auid = auId;
    this.rawSet = rawSet;
    this.date = date;
  }

  public String getAuid() {
    return auid;
  }

  public void setAuid(String auid) {
    this.auid = auid;
  }

  public Set<String> getRawSet() {
    return rawSet;
  }

  public void setRawSet(Set<String> rawSet) {
    this.rawSet = rawSet;
  }

  public long getDate() {
    return date;
  }

  public void setDate(long date) {
    this.date = date;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DatedPeerIdSetBean that = (DatedPeerIdSetBean) o;

    return new EqualsBuilder().append(date, that.date)
        .append(auid, that.auid).append(rawSet, that.rawSet).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(auid).append(rawSet).append(date).toHashCode();
  }

  @Override
  public String toString() {
    return "DatedPeerIdSetBean{" +
        "auid='" + auid + '\'' +
        ", rawSet=" + rawSet +
        ", date=" + date +
        '}';
  }
}
