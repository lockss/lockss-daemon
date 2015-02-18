/*
 * $Id$
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.util.MissingResourceException;

import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.util.Logger;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

/**
 * A utility class for I18N features.
 *
 * @author Neil Mayo
 */
public class I18nUtil {

  // Avoid circular loading dependencies.
  protected static Logger log =
    Logger.getLoggerWithInitialLevel("I18nUtil",
				     Logger.getInitialDefaultLevel());

  /** The name of the default backup bundle. This should always be in the build. */
  private static final String defaultBundle = "DefaultBundle";

  // -------------------------- LOCKSS CONFIG PARAMS --------------------------
  static final String PREFIX = Configuration.PREFIX + "i18n.";
  /**
   * Enable i18n using the gettext-commons-generated packages.
   * This is a System property, not a LOCKSS config param.  It should be
   * set on the command line with <tt>-D</tt>.
   */
  public static final String PARAM_ENABLE_I18N = PREFIX + "enabled";
  public static final boolean DEFAULT_ENABLE_I18N = false;

  /**
   * Get a gettext-commons I18n object suitable for performing
   * localisation of strings. If the gettext bundles are not available,
   * or the config param is set to false,
   * it will use the defaultBundle, which does no translation but does
   * perform formatting for strings with arguments.
   * <p>
   * The gettext-commons library internally caches the I18n objects.
   * 
   * @param clazz the class of the caller
   * @return an I18n object
   */
  public static I18n getI18n(Class clazz) {
    boolean enableI18n = isEnabled();
    I18n i18n = null;
    // Try loading the full i18n if enabled
    if (enableI18n) try {
      i18n = I18nFactory.getI18n(clazz);
    } catch (MissingResourceException ex) {
      log.error("Cannot initialize i18n for " + clazz + ex.toString());
      enableI18n = false;
    }

    // If i18n disabled (manually or through failure), load the default bundle
    if (!enableI18n) try {
      i18n = I18nFactory.getI18n(clazz, defaultBundle);
      log.warning("i18n is disabled: using "+defaultBundle);
    } catch (MissingResourceException e) {
      log.critical("Cannot initialize "+defaultBundle+" for "+clazz, e);
      i18n = null;
      // This shouldn't happen if the DefaultBundle is incorporated into the
      // build. Is the omission a serious enough build error to stop the daemon?
    }

    return i18n;
  }

  static boolean isEnabled() {
    String param = System.getProperty(PARAM_ENABLE_I18N,
				      Boolean.toString(DEFAULT_ENABLE_I18N));
    if ("true".equalsIgnoreCase(param)) {
      return true;
    }
    if ("false".equalsIgnoreCase(param)) {
      return false;
    }
    return DEFAULT_ENABLE_I18N;
  }
}
