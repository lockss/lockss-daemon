/*
 * $Id: LockssPlugin.java,v 1.1 2003-01-31 09:47:19 claire Exp $
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

package org.lockss.plugin;

import org.lockss.daemon.ArchivalUnit;

/**
 * Interface required by a plugin to be used by the lockss daemon
 * @author Claire Griffin
 * @version 1.0
 */

public interface LockssPlugin {

  /**
   * called after plugin is loaded to give the plugin time to perform any
   * needed initializations
   */
  public void init();

  /**
   * called after the plugin has been registered with the plugin manager.
   * The plugin can now make requests of other services.
   */
  public void start();

  /**
   * called when the application is stopping to allow the plugin to perform
   * any necessary tasks needed to cleanly halt
   */
  public void stop();

  /**
   * Returns the Archival Unit that is being handled by this plugin.
   * @return ArchivalUnit the archival unit
   */
  public ArchivalUnit getArchivalUnit();

  /**
   * return a string that represents the identity of this plugin
   * @return a string that identifies this plugin
   */
  public String getPluginId();


}