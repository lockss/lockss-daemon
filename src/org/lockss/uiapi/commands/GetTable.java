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
import java.text.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;

import org.lockss.uiapi.util.*;

/**
 * Implements the "get status table" command
 */
public class GetTable extends ApiActivityBase {

  private static Logger log = Logger.getLogger("GetTable");

  /*
   * Daemon status table, table options (name=value pairs supplied by caller)
   */
  private StatusTable statusTable;
  private HashMap     requestOptions;


  public GetTable() {
    super();
    requestOptions = new HashMap();
  }


  /**
   * Set a named status table request option
   * @param name Option name
   * @param value Option value
   */
  protected void setRequestOption(String name, String value) {

    if (StringUtil.isNullString(name) || StringUtil.isNullString(value)) {
      throw new ResponseException
                    ("Requested status table option (name or value) is null");
    }
    requestOptions.put(name, value);
  }

  /**
   * Get a named status table option
   * @param name Option name
   * @return Option value (null if none)
   */
  protected String getRequestOption(String name) {
    return (String) requestOptions.get(name);
  }

  /**
   * Get an interator to all status table request options
   * @return Option table EntrySet Iterator
   */
  protected Iterator getRequestOptionEntrySetIterator() {
    return requestOptions.entrySet().iterator();
  }

  /**
   * Populate the response body with information from the requested table
   * @return true on success
   */
  public boolean doCommand() throws XmlException {

    XmlStatusTable  xmlStatusTable;
    BitSet          statusOptions;
    Element         element;
    String          name;
    String          key;
    String          option;
    Iterator        iterator;

    try {
      String value;
      /*
       * Get requested table name and key
       */
      element = getXmlUtils().getElement(getRequestRoot(), AP_E_NAME);
      ParseUtils.verifyMandatoryElement(element, AP_E_NAME);
      name    = XmlUtils.getText(element);

      element = getXmlUtils().getElement(getRequestRoot(), AP_E_KEY);
      key     = XmlUtils.getText(element);

      element = getXmlUtils().getElement(getRequestRoot(), AP_E_OPTION);
      option  = XmlUtils.getText(element);
      /*
       * Set up an empty status table, get any requested "bit set" options ...
       */
      statusTable   = new StatusTable(name, key);
      statusOptions = new BitSet();

      if ("norows".equalsIgnoreCase(option)) {
        statusOptions.set(StatusTable.OPTION_NO_ROWS);
      }
      statusTable.setOptions(statusOptions);
      /*
       * Get any name=value option pairs as well ...
       */
      iterator = getRequestOptionEntrySetIterator();
      while (iterator.hasNext()) {
        Map.Entry entry = (Map.Entry) iterator.next();

	      statusTable.setProperty((String) entry.getKey(),
                                (String) entry.getValue());
      }
      /*
       * Fill the status table and format the result as XML
       */
      getStatusService().fillInTable(statusTable);
      xmlStatusTable = new XmlStatusTable(statusTable);
      /*
       * Copy the status table XML into our response document verbatim
       */
      XmlUtils.copyDocument(xmlStatusTable.getTableDocument(),
                                 getResponseDocument());
      return true;

    } catch (Exception exception) {
      throw new ResponseException(exception.toString());
    }
  }
}
