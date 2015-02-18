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

package org.lockss.uiapi.util;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import org.apache.xerces.dom.*;
import org.apache.xml.serialize.*;

import org.lockss.daemon.status.*;
import org.lockss.util.*;

public class RepositoryUtils {
  private static Logger log = Logger.getLogger("RepositoryUtils");

  /*
   * Constructors
   */
  private RepositoryUtils() {
  }

  /**
   * Get repository details
   */
  public static List getRepositoryDetail(StatusService statusService)
                                         throws StatusService.NoSuchTableException,
                                                XmlDomBuilder.XmlDomException {
    XmlUtils        statusUtils;
    Element         statusRoot;
    StatusTable     statusTable;
    XmlStatusTable  xmlStatusTable;
    NodeList        rowList;
    ArrayList       resultList;


    resultList  = new ArrayList();
    statusUtils = ParseUtils.getStatusXmlUtils();

    /*
     * Get information for each repository on the system
     */
    statusTable     = statusService.getTable("RepositorySpace", null);
    xmlStatusTable  = new XmlStatusTable(statusTable);
    statusRoot      = xmlStatusTable.getTableDocument().getDocumentElement();
    rowList         = statusUtils.getElementList(statusRoot,
                                                 XmlStatusConstants.ROW);

    for (int i = 0; i < rowList.getLength(); i++) {
      String    name, size, free, inUse;
      NodeList  cellList;

      cellList = statusUtils.getElementList(statusRoot,
                                            XmlStatusConstants.CELL);

      /*
       * Get (and save) the repository name, total size, used and free space
       */
      name  = getCellItem(statusUtils, cellList, "repo");
      size  = getCellItem(statusUtils, cellList, "size");
      inUse = getCellItem(statusUtils, cellList, "used");
      free  = getCellItem(statusUtils, cellList, "free");

      resultList.add(new Detail(name, size, inUse, free));
    }
    return resultList;
  }

  /**
   * Get one named item from a status "cell"
   * @param statusUtils An XmlUtils object for the status response
   * @param cellList a list of cell nodes
   * @param itemName The item to lookup
   * @return the text of the requested iten (null if none)
   */
  private static String getCellItem(XmlUtils statusUtils,
                                    NodeList cellList, String itemName) {

    for (int i = 0; i < cellList.getLength(); i++) {
      String cellName = ParseUtils.getText(statusUtils,
                                           (Element) cellList.item(i),
                                           XmlStatusConstants.COLUMN_NAME);

      if (itemName.equalsIgnoreCase(cellName)) {
        return ParseUtils.getText(statusUtils,
                                  (Element) cellList.item(i),
                                  XmlStatusConstants.VALUE);
      }
    }
    return null;
  }

  /**
   * Repository details (returned as list components, essentially a structure)
   */
  public static class Detail {
    private String    name;
    private String    size;
    private String    inUse;
    private String    free;

    private Detail() {
    }

    public Detail(String name, String size, String inUse, String free) {
      this.name   = name;
      this.size   = size;
      this.inUse  = inUse;
      this.free   = free;
    }

    /**
     * Get repository name
     * @return the name of this repository
     */
    public String getName() {
      return name;
    }

    /**
     * Get repository size
     * @return the total size of this repository
     */
    public String getSize() {
      return size;
    }

    /**
     * Get the space used by this repository
     * @return Repository space in use
     */
    public String getSpaceInUse() {
      return inUse;
    }

    /**
     * Get space available to this repository
     * @return Repository free space
     */
    public String getAvailableSpace() {
      return free;
    }
  }
}
