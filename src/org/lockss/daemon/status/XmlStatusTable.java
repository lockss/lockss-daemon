/*
 * $Id: XmlStatusTable.java,v 1.1 2004-02-20 05:28:17 eaalto Exp $
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
import java.net.InetAddress;
import java.text.SimpleDateFormat;

import org.lockss.util.*;

import org.w3c.dom.*;

public class XmlStatusTable {
  private static Logger logger = Logger.getLogger("XmlStatusTable");

  /**
   * The SimpleDateFormat pattern for Date entries.
   */
  public static final String DATE_FORMAT = "yyyy.MM.dd GGG HH:mm";

//XXX fix values
  XmlDomBuilder xmlBuilder = new XmlDomBuilder("sa",
      "http://lockss.stanford.edu/statusui", "1.0");

  StatusTable statusTable = null;
  Document tableDocument = null;

  public XmlStatusTable(StatusTable statusTable) {
    this.statusTable = statusTable;
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

      element = xmlBuilder.createElement(rootElem, XmlStatusConstants.NAME);
      if ((value = statusTable.getName()) != null) {
        xmlBuilder.addText(element, value);
      }

      element = xmlBuilder.createElement(rootElem, XmlStatusConstants.KEY);
      if ((value = statusTable.getKey()) != null) {
        xmlBuilder.addText(element, value);
      }

      element = xmlBuilder.createElement(rootElem, XmlStatusConstants.TITLE);
      if ((value = statusTable.getTitle()) != null) {
        xmlBuilder.addText(element, value);
      }

      /*
       * Column descriptor, row, and summary information
       */
      List colDesc = statusTable.getColumnDescriptors();
      addColumnDescriptors(rootElem, colDesc);
      addRows(rootElem, colDesc);
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

      Element element = xmlBuilder.createElement(cdElement,
          XmlStatusConstants.NAME);
      xmlBuilder.addText(element, cd.getColumnName());

      element = xmlBuilder.createElement(cdElement, XmlStatusConstants.TYPE);
      xmlBuilder.addText(element, Integer.toString(cd.getType()));

      element = xmlBuilder.createElement(cdElement, XmlStatusConstants.TITLE);
      if ((value = cd.getTitle()) != null) {
        xmlBuilder.addText(element, value);
      }

      element = xmlBuilder.createElement(cdElement, XmlStatusConstants.FOOTNOTE);
      if ((value = cd.getFootNote()) != null) {
        xmlBuilder.addText(element, value);
      }
    }
  }

  /**
   * Adds the table rows to the table Document
   * @param rootElem the table root {@link Element}
   * @param colList List
   */
  private void addRows(Element rootElem, List colList) {
    List rowList = statusTable.getSortedRows();
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
        Object object = rowMap.get(colNames[ii]);
        int type = colTypes[ii];

        if (object instanceof StatusTable.Reference) {
          // Reference
          StatusTable.Reference reference = (StatusTable.Reference)object;
          String value;

          Element referenceElement = xmlBuilder.createElement(rowElement,
              XmlStatusConstants.REFERENCE_ELEM);

          element = xmlBuilder.createElement(referenceElement,
              XmlStatusConstants.NAME);
          xmlBuilder.addText(element, reference.getTableName());

          element = xmlBuilder.createElement(referenceElement,
              XmlStatusConstants.KEY);
          if ((value = reference.getKey()) != null) {
            xmlBuilder.addText(element, value);
          }

          element = xmlBuilder.createElement(referenceElement,
              XmlStatusConstants.VALUE);
          object = reference.getValue();

          if (object instanceof StatusTable.DisplayedValue) {
            // A DisplayedValue - save display characteristics
            StatusTable.DisplayedValue dv = (StatusTable.DisplayedValue)object;

            String color = dv.getColor();
            if (color != null) {
              xmlBuilder.setAttribute(element, XmlStatusConstants.COLOR, color);
            }

            boolean bold = dv.getBold();
            if (bold) {
              xmlBuilder.setAttribute(element, XmlStatusConstants.BOLD, "true");
            }

            xmlBuilder.addText(element, formatByType(dv.getValue(), type));
          } else {
            // Standard value field
            xmlBuilder.addText(element, formatByType(object, type));
          }
          // done with Reference element
        } else {
          // Not a Reference element
          String color = null;
          boolean bold = false;
          // Check if a DisplayedValue?
          if (object instanceof StatusTable.DisplayedValue) {
            // set color, object appropriately
            StatusTable.DisplayedValue dv = (StatusTable.DisplayedValue)object;
            color = dv.getColor();
            bold = dv.getBold();
            object = dv.getValue();
          }
          // save display charactistics as required
          element = xmlBuilder.createElement(rowElement,
              XmlStatusConstants.STANDARD_ELEM);
          if (color != null) {
            xmlBuilder.setAttribute(element, XmlStatusConstants.COLOR, color);
          }
          if (bold) {
            xmlBuilder.setAttribute(element, XmlStatusConstants.BOLD, "true");
          }
          xmlBuilder.addText(element, formatByType(object, type));
        }
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

      Element element = xmlBuilder.createElement(siElement,
          XmlStatusConstants.TITLE);
      xmlBuilder.addText(element, si.getTitle());

      element = xmlBuilder.createElement(siElement, XmlStatusConstants.TYPE);
      xmlBuilder.addText(element, Integer.toString(si.getType()));

      element = xmlBuilder.createElement(siElement, XmlStatusConstants.VALUE);
      xmlBuilder.addText(element, formatByType(si.getValue(), si.getType()));
    }
  }

  /**
   * Convert row value (based on object type)
   * @param object the row value
   * @param type the object type
   * @return formatted String
   */
  static String formatByType(Object object, int type) {
    if (object == null) {
      return "";
    }

    try {
      switch (type) {
        case ColumnDescriptor.TYPE_STRING:
        case ColumnDescriptor.TYPE_FLOAT:
        case ColumnDescriptor.TYPE_INT:
          return object.toString();

        case ColumnDescriptor.TYPE_PERCENT:
          float fv = ((Number)object).floatValue();
          return Integer.toString(Math.round(fv * 100));

        case ColumnDescriptor.TYPE_DATE:
          Date date;

          if (object instanceof Number) {
            date = new Date(((Number)object).longValue());
          } else if (object instanceof Date) {
            date = (Date)object;
          } else {
            return object.toString();
          }
          SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
          return sdf.format(date);
        case ColumnDescriptor.TYPE_IP_ADDRESS:
          return ((InetAddress)object).getHostAddress();
        case ColumnDescriptor.TYPE_TIME_INTERVAL:
          long milli = ((Number)object).longValue();
          return StringUtil.timeIntervalToString(milli);
        default:
          logger.warning("Unanticipated data type found: " + type +
                         ", object = " + object.toString());
          return object.toString();
      }
    } catch (Exception e) {
      logger.error("Invalid format for type " + type + ", object = " +
                   object.toString(), e);
      return(XmlStatusConstants.UNKNOWN + " " + object.toString());
    }
  }
}
