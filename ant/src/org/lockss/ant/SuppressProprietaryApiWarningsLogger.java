/*
 * $Id: SuppressProprietaryApiWarningsLogger.java,v 1.1.2.1 2010-02-22 06:39:59 tlipkis Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.ant;

import java.util.regex.*;
import org.apache.tools.ant.*;

/** Ant logger that filters out Sun proprietary API warnings, which can't
 * be disabled and interfere massively with development in emacs using
 * 'next-error'.  The proprietary usage should be removed, of course, but
 * that will take some time.  Also includes the behavior of Ant's
 * NoBannerLogger.  To use this, build lockss-ant.jar, copy it into Ant's
 * lib dir, and pass <code>-logger
 * org.lockss.ant.SuppressProprietaryApiWarningsLogger</code> to ant. */

public class SuppressProprietaryApiWarningsLogger extends NoBannerLogger {

  private int skipLines = 0;

  Pattern pat = Pattern.compile(".* is Sun proprietary API .*");

  /** Sole constructor. */
  public SuppressProprietaryApiWarningsLogger() {
  }

  /**
   * Logs a message for a target if it is of an appropriate
   * priority, also logging the name of the target if this
   * is the first message which needs to be logged for the
   * target.
   *
   * @param event A BuildEvent containing message information.
   *              Must not be <code>null</code>.
   */
  public void messageLogged(BuildEvent event) {

    String flg = System.getenv("SHOW_SUN_API_WARNINGS");
    if (flg == null || flg.length() == 0) {
      if (skipLines > 0) {
	skipLines--;
	return;
      }
      
      if (pat.matcher(event.getMessage()).matches()) {
	// If matches, skip this and the nect two lines (the code and the
	// caret)
	skipLines = 2;
	return;
      }
    }
    super.messageLogged(event);
  }
}
