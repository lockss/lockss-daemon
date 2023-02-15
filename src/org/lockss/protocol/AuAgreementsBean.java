package org.lockss.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AuAgreementsBean {
  private String auid;

  private Map<String, PeerAgreements> rawMap;

  public AuAgreementsBean() {

  }

  public AuAgreementsBean(String auId, HashMap<String, PeerAgreements> rawMap) {
    this.auid = auId;
    this.rawMap = rawMap;
  }

  public String getAuid() {
    return auid;
  }

  public void setAuid(String auid) {
    this.auid = auid;
  }

  public Map<String, PeerAgreements> getRawMap() {
    return rawMap;
  }

  public void setRawMap(Map<String, PeerAgreements> rawMap) {
    this.rawMap = rawMap;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuAgreementsBean that = (AuAgreementsBean) o;
    return Objects.equals(auid, that.auid) &&
        Objects.equals(rawMap, that.rawMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(auid, rawMap);
  }

  @Override
  public String toString() {
    return "AuAgreementsBean{" +
        "auid='" + auid + '\'' +
        ", rawMap=" + rawMap +
        '}';
  }
}
