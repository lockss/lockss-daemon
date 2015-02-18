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
package org.lockss.plugin;

public class AuEvent {

  /** Describes the event that caused the AuEventHandler to be invoked.
   * May provide more detail than the AuEventHandler method name. */
  public enum Type {
    /** AU created via UI. */
    Create,
    /** AU deleted via UI. */
    Delete,
    /** Previously created AU started at daemon startup. */
    StartupCreate,
    /** AU deactivated via UI. */
    Deactivate,
    /** AU reactivated via UI.  (Not currently used.) */
    Reactivate,
    /** AU briefly deleted as part of a restart operation. */
    RestartDelete,
    /** AU recreated as part of a restart operation. */
    RestartCreate,
    /** AU config changed (non-def params only, doesn't happen in normal use).
     */
    Reconfig,
    /** AU's content chaged. */
    ContentChanged
  };

  private Type type;
  private boolean inBatch;

  public AuEvent(Type type, boolean inBatch) {
    this.type = type;
    this.inBatch = inBatch;
  }

  public Type getType() {
    return type;
  }

  public boolean isInBatch() {
    return inBatch;
  }
}
