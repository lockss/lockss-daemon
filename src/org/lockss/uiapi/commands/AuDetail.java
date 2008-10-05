/*
 * $Id: AuDetail.java,v 1.2 2005-10-11 05:47:42 tlipkis Exp $
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

package org.lockss.uiapi.commands;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.remote.*;
import org.lockss.servlet.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;

import org.lockss.uiapi.servlet.*;
import org.lockss.uiapi.util.*;

/**
 * Fetch Archival Unit detailed information.
 *
 * We simply extend the generic status accessor <code>GetTable</code> class.
 */
public class AuDetail extends GetTable {

  private static String NAME  = "AuDetail";
  private static Logger log   = Logger.getLogger(NAME);

  public AuDetail() {
    super();
  }

  /**
   * Request AU details (summary information or file list)
   * @return true on success
   */
  public boolean doCommand() throws XmlException {
    String skipCount = getParameter(AP_E_SKIPROWS);
     /*
      * If "file list" parameters are present, save them for GetTable
      */
    if (!StringUtil.isNullString(skipCount)) {
      setRequestOption(AP_E_SKIPROWS, skipCount); // Where to start
      setRequestOption(AP_E_NUMROWS, "100");      // Files to display
    }
    return super.doCommand();
  }
}
