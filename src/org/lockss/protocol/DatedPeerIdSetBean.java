package org.lockss.protocol;

import java.util.HashSet;
import java.util.Set;

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
}
