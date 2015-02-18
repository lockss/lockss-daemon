/*
 * $Id$
 *

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

/** LockssWatchdog describes a watchdog facility that can be started,
 * stopped and poked.  This is a separate interface because the code that
 * must poke the watchdog is often not directly part of the thread in which
 * it's running, and esp. in test code it's awkward to have to pass in the
 * thread.
 * @see LockssThread
 * @see LockssRunnable
 */
public interface LockssWatchdog {
  /** Start a watchdog timer that will expire if not poked for interval
   * milliseconds.
   * @param interval milliseconds after which watchdog will go off.
   */
  void startWDog(long interval);

  /** Stop the watchdog so that it will not trigger. */
  void stopWDog();

  /** Refresh the watchdog for another interval milliseconds. */
  void pokeWDog();

  /** Return the length of the interval after which the watchdog will trip
   * if it hadn't been poked.  -1 means the watchdog is inactive. */
  long getWDogInterval();
}
