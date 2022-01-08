package org.lockss.protocol;

import java.util.HashMap;
import java.util.Map;

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

}
