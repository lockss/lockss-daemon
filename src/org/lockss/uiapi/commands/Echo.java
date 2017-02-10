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

package org.lockss.uiapi.commands;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import org.lockss.uiapi.util.*;

/**
 * Implements the "echo" command
 */
public class Echo extends ApiActivityBase {

  /**
   * Populate the response body (return the sender's comment text)
   * @return true
   */
  public boolean doCommand() {

    XmlUtils xmlUtils = getXmlUtils();

    /*
     * Get sender's comment
     */
    Element element = xmlUtils.getElement(getRequestRoot(), AP_E_COMMENT);
    String  comment = XmlUtils.getText(element);
    /*
     * Return it as a comment in the response document
     */
    element = xmlUtils.createElement(getResponseRoot(), AP_E_COMMENT);
    XmlUtils.addText(element, comment);

    return true;
  }
}

