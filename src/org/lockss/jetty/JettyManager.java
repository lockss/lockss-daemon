/*
 * $Id: JettyManager.java,v 1.3 2003-04-04 08:39:04 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.jetty;

import java.util.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.mortbay.util.Code;

/**
 * Abstract base class for LOCKSS managers that use/start Jetty services
 */
public abstract class JettyManager extends BaseLockssManager {
  static final String PREFIX = Configuration.PREFIX + "jetty.debug";

  static final String PARAM_JETTY_DEBUG = PREFIX;
  static final String PARAM_JETTY_DEBUG_PATTERNS = PREFIX + ".patterns";
  static final String PARAM_JETTY_DEBUG_VERBOSE = PREFIX + ".verbose";
//   static final String PARAM_JETTY_DEBUG_OPTIONS = PREFIX + ".options";

  private static Logger log = Logger.getLogger("JettyMgr");
  private static boolean jettyLogInited = false;

  public JettyManager() {
  }

  /**
   * start the manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    // install Jetty logger once only
    if (!jettyLogInited) {
      org.mortbay.util.Log.instance().add(new LoggerLogSink());
      jettyLogInited = true;
    }
  }

  // Set Jetty debug properties from config params
  protected void setConfig(Configuration config, Configuration prevConfig,
			   Set changedKeys) {
    Properties p = System.getProperties();
    Code.setDebug(config.getBoolean(PARAM_JETTY_DEBUG, false));
    Code.setDebugPatterns(config.get(PARAM_JETTY_DEBUG_PATTERNS));
    if (changedKeys.contains(PARAM_JETTY_DEBUG_VERBOSE)) {
      Code.setVerbose(config.getInt(PARAM_JETTY_DEBUG_VERBOSE, 0));
    }
  }
}
