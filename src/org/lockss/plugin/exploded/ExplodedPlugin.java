/*
 * $Id: ExplodedPlugin.java,v 1.4.14.1 2009-11-03 23:44:52 edwardsb1 Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.exploded;

import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.definable.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;

/**
 * <p>ExplodedPlugin: A plugin for AUs full of files ingested by
 * explosion rather than crawling.
 * @version 1.0
 * @author David Rosenthal
 */

public class ExplodedPlugin extends DefinablePlugin {
  protected static final Logger log = Logger.getLogger("ExplodedPlugin");

  public static final String PREFIX =
    Configuration.PREFIX + "plugin.exploded.";

  public ExplodedPlugin() {
  }

  protected ArchivalUnit createAu0(Configuration auConfig)
      throws ArchivalUnit.ConfigurationException {
    // create a new archival unit
    ArchivalUnit au = new ExplodedArchivalUnit(this, definitionMap);

    // Now configure it.
    au.setConfiguration(auConfig);

    return au;
  }
}
