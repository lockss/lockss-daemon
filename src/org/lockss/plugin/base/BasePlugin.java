/*
 * $Id: BasePlugin.java,v 1.1 2003-02-20 22:28:19 tal Exp $
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

package org.lockss.plugin.base;

import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * Abstract base class for Plugins.  Plugins are encouraged to extend this
 * class to get some common Plugin functionality.
 */
public abstract class BasePlugin implements Plugin {
  protected Map auMap = new HashMap();

  /**
   * Must invoke this constructor in plugin subclass.
   */
  protected BasePlugin() {
  }

  // These are here so overriding methods in subclasses can call super(),
  // in case some common functionality is needed in the future.

  public void initPlugin() {
  }

  public void stopPlugin() {
  }

  public ArchivalUnit getAU(String auId) {
    return (ArchivalUnit)auMap.get(auId);
  }

  public Collection getAllAUs() {
    return auMap.values();
  }

  public ArchivalUnit configureAU(Configuration config)
      throws ArchivalUnit.ConfigurationException {
    String auId = getAUIdFromConfig(config);
    ArchivalUnit au = getAU(auId);
    if (au != null) {
      au.setConfiguration(config);
    } else {
      au = createAU(config);
      auMap.put(auId, au);
    }
    return au;
  }

}
