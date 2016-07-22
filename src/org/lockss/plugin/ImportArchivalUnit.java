/*

 Copyright (c) 2016 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Except as contained in this notice, the name of Stanford University shall not
 be used in advertising or otherwise to promote the sale, use or other dealings
 in this Software without prior written authorization from Stanford University.

 */
package org.lockss.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.LoginPageChecker;
import org.lockss.daemon.PermissionChecker;
import org.lockss.plugin.base.BaseArchivalUnit;
import org.lockss.util.Logger;

/**
 * The archival unit used to wrap imported files.
 */
public class ImportArchivalUnit extends BaseArchivalUnit {
	
  private static final Logger log = Logger.getLogger(ImportArchivalUnit.class);

  private String baseUrl = null;

  public ImportArchivalUnit(ImportPlugin plugin) {
    super(plugin);
  }

  // Called by ImportPlugin iff any config below ImportPlugin.PREFIX
  // has changed
  protected void setConfig(Configuration config,
			   Configuration prevConfig,
			   Configuration.Differences changedKeys) {
  }

  @Override
  public void loadAuConfigDescrs(Configuration auConfig)
      throws ConfigurationException {
    super.loadAuConfigDescrs(auConfig);
    baseUrl = auConfig.get(ConfigParamDescr.BASE_URL.getKey());
    if (log.isDebug3()) log.debug3("baseUrl = " + baseUrl);
  }

  /**
   * Provides a name for the Archival Unit.
   * 
   * @return The name for the Archival Unit.
   */
  @Override
  protected String makeName() {
    return "Import AU at '" + baseUrl + "'";
  }

  /**
   * return a string that points to the plugin registry page.
   * @return a string that points to the plugin registry page for
   * this registry.  This is just the base URL.
   */
  @Override
  public Collection<String> getStartUrls() {
    return new ArrayList<String>();
  }

  @Override
  public List<PermissionChecker> makePermissionCheckers() {
    return null;
  }

  @Override
  public int getRefetchDepth() {
    return 1;
  }

  @Override
  public LoginPageChecker getLoginPageChecker() {
    return null;
  }

  @Override
  public String getCookiePolicy() {
    return null;
  }
}
