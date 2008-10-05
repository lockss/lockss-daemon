/*
 * $Id: CommonParameters.java,v 1.2 2005-10-11 05:47:56 tlipkis Exp $
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

package org.lockss.uiapi.util;

import org.lockss.util.XmlDomBuilder;
import org.lockss.daemon.status.XmlStatusConstants;


public interface CommonParameters {
  /*
   * Common parameters
   *
   * Agent name and version (used as HTTP User-Agent)
   */
  final static String   COM_USER_AGENT      = "UIAGENT/1.0";
  /*
   * UI, API, and Cluster Control version information
   */
  final static String   COM_UI_VERSION      = "0.1";
  /*
   * XML version attribute name
   */
  final static String   COM_XML_VERSIONNAME = XmlDomBuilder.XML_VERSIONNAME;
  /*
   * Status Element, Attributes
   */
  final static String   COM_E_STATUS        = "status";
  final static String   COM_E_MESSAGE       = "message";
  final static String   COM_E_DETAIL        = "detail";

  final static String   COM_A_SUCCESS       = "success";
  final static String   COM_A_COMMAND       = "command";
  final static String   COM_A_DATE          = "date";
  final static String   COM_A_SYSTEM        = "system";

  /*
   * Some common values
   */
  final static String   COM_VALUE_UNKNOWN   = "*unknown*";
  final static String   COM_VALUE_UNKNOWN_NUMBER
                                            = "**";

  final static String   COM_TRUE            = "true";
  final static String   COM_FALSE           = "false";

  final static String   COM_CR              = "\r";
  final static String   COM_LF              = "\n";
}
