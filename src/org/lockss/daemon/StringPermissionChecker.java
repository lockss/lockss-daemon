/*
 * $Id: StringPermissionChecker.java,v 1.2 2004-08-19 00:02:19 clairegriffin Exp $
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

import java.io.*;
import java.util.*;

import org.lockss.plugin.*;
import org.lockss.util.*;

public class StringPermissionChecker implements PermissionChecker {
  public static final int IGNORE_CASE = 0;
  String m_matchString;
  BitSet m_flags = new BitSet(8);
  FilterRule m_filter;
  String m_encoding;
  static Logger m_logger = Logger.getLogger("PermissionCheck");
  private static final int PERM_BUFFER_MAX = 20000;

  public StringPermissionChecker(String matchString, FilterRule filterRule) {
      m_matchString = matchString;
      m_filter = filterRule;
      m_encoding = Constants.DEFAULT_ENCODING;
  }

  public StringPermissionChecker(String matchString) {
    this(matchString, null);
  }

  public void setFlag(int bitNumber, boolean setOn) {
    if(setOn) {
      m_flags.set(bitNumber);
    }
    else {
      m_flags.clear(bitNumber);
    }
  }

  public void setEncoding(String encoding) {
    m_encoding = encoding;
  }

  public boolean checkPermission(Reader reader) {
    boolean permission_ok = false;
    String matchstr = m_matchString;
    int ch;
    int p_index = 0;

    if (m_flags.get(IGNORE_CASE)) {
      matchstr = matchstr.toLowerCase();
    }
    try {
      if (m_filter != null) {
        reader = m_filter.createFilteredReader(reader);
      }
      do {
        ch = reader.read();
        if(m_flags.get(IGNORE_CASE) && ch != -1) {
          ch = Character.toLowerCase((char)ch);
        }
        char nextChar = matchstr.charAt(p_index);

        if (nextChar == ch) {
          if (++p_index == m_matchString.length()) {
            return true;
          }
        }
        else {
          p_index = 0;
        }
      }
      while (ch != -1); // while not eof
    }
    catch (IOException ex) {
      m_logger.warning("Exception occured while checking for permission: "
                       + ex.toString());
    }
    return permission_ok;
  }

}
