/*
 * $Id: XmlStatusTable.java,v 1.7 2004-05-04 22:18:01 tlipkis Exp $
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

package org.lockss.daemon.status;

import java.util.*;

import org.lockss.util.*;
import org.lockss.servlet.DaemonStatus;

import org.w3c.dom.*;

public class XmlStatusTable {
  private static Logger logger = Logger.getLogger("XmlStatusTable");

  XmlDomBuilder xmlBuilder = new XmlDomBuilder(XmlStatusConstants.NS_PREFIX,
                                               XmlStatusConstants.NS_URI,
                                               XmlDomBuilder.XML_VERSIONNAME);

  StatusTable statusTable = null;
  Document tableDocument = null;

  public XmlStatusTable(StatusTable statusTable) {
    this.statusTable = statusTable;
  }

  /**
   * Returns the {@link XmlDomBuilder} being used.
   * @return the XmlDomBuilder
   */
  public XmlDomBuilder getXmlDomBuilder() {
    return xmlBuilder;
  }

  /**
   * Returns the document form of the StatusTable
   * @return Document
   * @throws XmlDomBuilder.XmlDomException
   */
  public Document getTableDocument() throws XmlDomBuilder.XmlDomException {
    if (tableDocument==null) {
      createDocument();
    }
    return tableDocument;
  }

  /**
   * Builds the Document from the StatusTable.
   * @return the Document
   * @throws XmlDomBuilder.XmlDomException
   */
  public Document createDocument() throws XmlDomBuilder.XmlDomException {
    tableDocument = xmlBuilder.createDocument();

    Element rootElem = null;
    Element element = null;

    try {
      String value;
      // create root element
      rootElem = xmlBuilder.createRoot(tableDocument,
          XmlStatusConstants.TABLE);

      addTextElement(rootElem, XmlStatusConstants.NAME, statusTable.getName());
      addTextElement(rootElem, XmlStatusConstants.KEY, statusTable.getKey());
      addTextElement(rootElem, XmlStatusConstants.TITLE,
		     statusTable.getTitle());
      /*
       * Column descriptor, row, and summary information
       */
      List colDesc = statusTable.getColumnDescriptors();
      if (colDesc != null) {
	addColumnDescriptors(rootElem, colDesc);
	addRows(rootElem, colDesc);
      }
      addSummaryInformation(rootElem, statusTable.getSummaryInfo());
      return tableDocument;
    } catch (Exception e) {
      throw new XmlDomBuilder.XmlDomException(e.getMessage());
    }
  }

  /**
   * Adds the column descriptors to the table document
   * @param rootElem the table root {@link Element}
   * @param colList the list of columns
   */
  private void addColumnDescriptors(Element rootElem, List colList) {
    Iterator colIter = colList.iterator();

    String value;
    while (colIter.hasNext()) {
      ColumnDescriptor cd = (ColumnDescriptor)colIter.next();
      Element cdElement = xmlBuilder.createElement(rootElem,
          XmlStatusConstants.COLUMNDESCRIPTOR);

      addTextElement(cdElement, XmlStatusConstants.NAME, cd.getColumnName());
      addTextElement(cdElement, XmlStatusConstants.TYPE,
		     Integer.toString(cd.getType()));
      addTextElement(cdElement, XmlStatusConstants.TITLE, cd.getTitle());
      addTextElement(cdElement, XmlStatusConstants.FOOTNOTE, cd.getFootnote());
    }
  }

  /**
   * Adds the table rows to the table Document
   * @param rootElem the table root {@link Element}
   * @param colList List
   */
  private void addRows(Element rootElem, List colList) {
    List rowList = statusTable.getSortedRows();
    if (rowList == null) {
      return;
    }
    Iterator rowIter = rowList.iterator();

    int colSize = colList.size();
    String[] colNames = new String[colSize];
    int[] colTypes = new int[colSize];
    for (int ii=0; ii<colList.size(); ii++) {
      ColumnDescriptor cd = (ColumnDescriptor)colList.get(ii);
      colNames[ii] = cd.getColumnName();
      colTypes[ii] = cd.getType();
    }

    while (rowIter.hasNext()) {
      Map rowMap = (Map)rowIter.next();
      Element element;

      // create row element
      Element rowElement = xmlBuilder.createElement(rootElem,
          XmlStatusConstants.ROW);

      for (int ii=0; ii<colNames.length; ii++) {
        // get the value for each column
        String colName = colNames[ii];
        int type = colTypes[ii];
        Object object = rowMap.get(colName);
	logger.info("Object: " + object);

	addValueElement(rowElement, object, colName, type);
      }
    }
  }

  /**
   * Add summary information to status table Element.
   * @param rootElem the table root {@link Element}
   * @param summaryInfo the List of summary info
   */
  private void addSummaryInformation(Element rootElem, List summaryInfo) {
    if (summaryInfo == null) {
      // This should not be necessary in the final implementation
      logger.info("StatusTable.getSummaryInfo() returned null");
      return;
    }

    Iterator sumIter = summaryInfo.iterator();
    while (sumIter.hasNext()) {
      StatusTable.SummaryInfo si = (StatusTable.SummaryInfo)sumIter.next();
      Element siElement = xmlBuilder.createElement(rootElem,
          XmlStatusConstants.SUMMARYINFO);

      addTextElement(siElement, XmlStatusConstants.TITLE, si.getTitle());
      addTextElement(siElement, XmlStatusConstants.TYPE,
		     Integer.toString(si.getType()));
      addDisplayValueElement(siElement, si.getValue(), si.getType());
    }
  }

  Element addTextElement(Element parent, String name, String value) {
    Element element =
      xmlBuilder.createElement(parent, name);
    if (value != null) {
      xmlBuilder.addText(element, value);
    }
    return element;
  }

  Element addValueElement(Element parent, Object value,
			  String colName, int type) {
    Element element = addReferenceValueElement(parent, value, type);
    addTextElement(element, XmlStatusConstants.COLUMN_NAME, colName);
    return element;
  }

  Element addReferenceValueElement(Element parent, Object value,
				   int type) {
    if (value instanceof StatusTable.Reference) {
      // Reference
      Element element =
	xmlBuilder.createElement(parent, XmlStatusConstants.REFERENCE_ELEM);
      StatusTable.Reference reference = (StatusTable.Reference)value;

      addTextElement(element, XmlStatusConstants.NAME,
		     reference.getTableName());
      addTextElement(element, XmlStatusConstants.KEY, reference.getKey());
      addDisplayValueElement(element, reference.getValue(), type);
      return element;
    } else {
      Element element =
	xmlBuilder.createElement(parent, XmlStatusConstants.STANDARD_ELEM);
      addDisplayValueElement(element, value, type);
      return element;
    }
  }

  Element addDisplayValueElement(Element parent, Object value, int type) {
    Element element =
      xmlBuilder.createElement(parent, XmlStatusConstants.VALUE);
    if (value instanceof StatusTable.DisplayedValue) {
      // A DisplayedValue - save display characteristics
      StatusTable.DisplayedValue dv = (StatusTable.DisplayedValue)value;
      String color = dv.getColor();
      if (color != null) {
	xmlBuilder.setAttribute(element, XmlStatusConstants.COLOR, color);
      }
      if (dv.getBold()) {
	xmlBuilder.setAttribute(element, XmlStatusConstants.BOLD, "true");
      }
      value = dv.getValue();
    }
    xmlBuilder.addText(element, formatByType(value, type));
    return element;
  }

  static String formatByType(Object object, int type) {
    return DaemonStatus.convertDisplayString(object, type);
  }

}
