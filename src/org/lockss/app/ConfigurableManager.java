/*
 * $Id$
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.app;

import org.lockss.config.Configuration;

/** Managers that implement this interface will have a
 * Configuration.Callback registered for them which calls {@link
 * #setConfig(Configuration, Configuration, Configuration.Differences)} */
public interface ConfigurableManager {

  /** ConfigurableManagers must implement this method.  It is called once
   * at app init time (during initService()) and again whenever the current
   * configuration changes.  This method should not invoke other services
   * unless isAppInited() is true.
   * @param newConfig the new {@link Configuration}
   * @param prevConfig the previous {@link Configuration}
   * @param changedKeys the keys whose values have changed
   */
  void setConfig(Configuration newConfig,
		 Configuration prevConfig,
		 Configuration.Differences changedKeys);
}

