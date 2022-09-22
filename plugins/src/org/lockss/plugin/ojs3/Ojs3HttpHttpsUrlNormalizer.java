package org.lockss.plugin.ojs3;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.HttpHttpsParamUrlNormalizer;

public class Ojs3HttpHttpsUrlNormalizer extends HttpHttpsParamUrlNormalizer {

  public Ojs3HttpHttpsUrlNormalizer() {
    super(ConfigParamDescr.BASE_URL.getKey());
  }

}
