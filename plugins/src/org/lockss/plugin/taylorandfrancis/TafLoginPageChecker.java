package org.lockss.plugin.taylorandfrancis;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.atypon.BaseAtyponLoginPageChecker;
import org.lockss.util.HeaderUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

public class TafLoginPageChecker extends BaseAtyponLoginPageChecker {


  private static final Logger log = Logger.getLogger(TafLoginPageChecker.class);
  protected final String journalDenial = "class=\"accessDenied";
  protected final String journalDenial2 = "id=\"accessDenialWidget";

  @Override
  public boolean isLoginPage(Properties props,
                             Reader reader)
      throws IOException,
      PluginException {
    if ("text/html".equalsIgnoreCase(HeaderUtil.getMimeTypeFromContentType(props.getProperty("Content-Type")))) {
      String page = StringUtil.fromReader(reader);
      return (
          page.contains(getLoginString()) ||
          page.contains(journalDenial) ||
          page.contains(journalDenial2)
      );
    }
    return false;
  }
}
