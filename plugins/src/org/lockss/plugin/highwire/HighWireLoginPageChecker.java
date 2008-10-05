/*
 * $Id: HighWireLoginPageChecker.java,v 1.2 2006-07-19 16:44:44 thib_gc Exp $
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

/**
 * Checks to see if a page is a HighWire login page, by checking for a
 * "no-cache" header first and then looking for a specified string on the
 * resulting page
 */

package org.lockss.plugin.highwire;

import org.lockss.daemon.*;
import org.lockss.util.*;
import java.util.*;
import java.io.*;

public class HighWireLoginPageChecker implements LoginPageChecker {

  public static final String LOGIN_STRING =
    "<!-- login page comment for LOCKSS-->";

  public static final String NO_STORE_VALUE = "no-store";
  public static final String CACHE_CONTROL_HEADER = "cache-control";


  public boolean isLoginPage(Properties props, Reader reader)
      throws IOException {
    if (props == null) {
      throw new NullPointerException("Called with a null props");
    } else if (reader == null) {
      throw new NullPointerException("Called with a null reader");
    }

    if (NO_STORE_VALUE.equalsIgnoreCase((String)
					 props.get(CACHE_CONTROL_HEADER))) {
      return StringUtil.containsString(reader, LOGIN_STRING);
    }
    return false;
  }
}
