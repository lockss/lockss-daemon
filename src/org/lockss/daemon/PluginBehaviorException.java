/*
 * $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

/** Exception thrown by daemon methods in response to illegal plugin
 * behavior, when no more specific checked exception is appropriate.  This
 * is NOT thrown by plugin code.  One common use is to turn a plugin-thrown
 * RuntimeException into a checked exception. */
public class PluginBehaviorException extends Exception {
    private Throwable nestedException;

  public PluginBehaviorException() {
    super();
  }

  public PluginBehaviorException(String msg) {
    super(msg);
  }

  public PluginBehaviorException(Throwable nested) {
    super(nested.toString());
    this.nestedException = nested;
  }

  public PluginBehaviorException(String msg, Throwable nested) {
    super(msg);
    this.nestedException = nested;
  }

  public Throwable getNestedException() {
    return nestedException;
  }
}
