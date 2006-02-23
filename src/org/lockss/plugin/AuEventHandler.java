/*
 * $Id: AuEventHandler.java,v 1.2 2006-02-23 06:57:02 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.config.*;

/**
 * Handler for AU transition events such as created, deleted, reconfigured.
 * Classes that wish to be notified of a subset of these events will
 * probably want to extend {@link AuEventHandler.Base}, which provides a
 * null implementation, and define only the handlers they need.
 */
public interface AuEventHandler {
  /** Called after the AU is created (either by user action or at daemon
   * start time */
  void auCreated(ArchivalUnit au);
  /** Called before the AU is deleted */
  void auDeleted(ArchivalUnit au);
  /** Called after an existing AU's configuration is changed */
  void auReconfigured(ArchivalUnit au, Configuration oldAuConf);

  /** Convenience class with null handlers for all AuEventHandler events.
   * Specialize this and override the events of interest */
  public class Base implements AuEventHandler {
    public void auCreated(ArchivalUnit au) {}
    public void auDeleted(ArchivalUnit au) {}
    public void auReconfigured(ArchivalUnit au, Configuration oldAuConf) {}
  }
}
