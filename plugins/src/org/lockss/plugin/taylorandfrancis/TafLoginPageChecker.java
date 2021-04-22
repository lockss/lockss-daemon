package org.lockss.plugin.taylorandfrancis;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.atypon.BaseAtyponLoginPageChecker;
import org.lockss.util.HeaderUtil;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

public class TafLoginPageChecker extends BaseAtyponLoginPageChecker {

  protected final String rajmJournalDenial = "class=\"accessDenied";

  @Override
  public boolean isLoginPage(Properties props,
                             Reader reader)
      throws IOException,
      PluginException {
    if ("text/html".equalsIgnoreCase(HeaderUtil.getMimeTypeFromContentType(props.getProperty("Content-Type")))) {
      String page = StringUtil.fromReader(reader);
      if (page.contains(getLoginString())) {
        return true;
      }
      return page.contains(rajmJournalDenial);
    }
    return false;
  }
}
