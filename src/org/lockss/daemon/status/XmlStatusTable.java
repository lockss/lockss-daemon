/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
import java.text.*;

import org.apache.commons.lang3.time.FastDateFormat;

import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.servlet.*;

import org.w3c.dom.*;

public class XmlStatusTable {
  
  private static final Logger logger = Logger.getLogger(XmlStatusTable.class);

  /** Date/time format in XML output with outputVersion=1.  "Local" is
   * localtime, "GMT" or "UTC" are GMT, else should be a legal
   * SimpleDateFormat spec. */
  public static final String PARAM_XML_DATE_FORMAT =
    Configuration.PREFIX + "admin.xmlDataFormat";
  public static final String DEFAULT_XML_DATE_FORMAT = "Local";

  XmlDomBuilder xmlBuilder = new XmlDomBuilder(XmlStatusConstants.NS_PREFIX,
                                               XmlStatusConstants.NS_URI,
                                               "2.0");

  StatusTable statusTable = null;
  Document tableDocument = null;
  int outputVersion = 1;
  Format dateFmt;

  public XmlStatusTable(StatusTable statusTable) {
    this.statusTable = statusTable;
    Configuration config = CurrentConfig.getCurrentConfig();
    dateFmt = getDateFormat(config.get(PARAM_XML_DATE_FORMAT,
				       DEFAULT_XML_DATE_FORMAT));
  }

  Format getDateFormat(String spec) {
    if ("GMT".equalsIgnoreCase(spec) || "UTC".equalsIgnoreCase(spec)) {
      return DisplayConverter.TABLE_DATE_FORMATTER_GMT;
    } else if ("local".equalsIgnoreCase(spec)) {
      return DisplayConverter.TABLE_DATE_FORMATTER_LOCAL;
    } else {
      try {
	return FastDateFormat.getInstance(spec);
      } catch (RuntimeException e) {
	logger.warning("org.lockss.admin.xmlDateFormat invalid: " + spec, e);
	return getDateFormat(DEFAULT_XML_DATE_FORMAT);
      }
    }
  }

  public void setOutputVersion(int ver) {
    outputVersion = ver;
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
      tableDocument = createDocument();
    }
    return tableDocument;
  }

  /**
   * Builds the Document from the StatusTable.
   * @return the Document
   * @throws XmlDomBuilder.XmlDomException
   */
  Document createDocument() throws XmlDomBuilder.XmlDomException {
    Document doc = XmlDomBuilder.createDocument();

    Element rootElem = null;
    Element element = null;

    try {
      String value;
      // create root element
      rootElem = xmlBuilder.createRoot(doc,
				       XmlStatusConstants.TABLE);

      addTextElement(rootElem, XmlStatusConstants.NAME, statusTable.getName());
      addTextElement(rootElem, XmlStatusConstants.KEY, statusTable.getKey());
      addTextElement(rootElem, XmlStatusConstants.TITLE,
		     statusTable.getTitle());
      if (statusTable.getTitleFootnote() != null) {
	addTextElement(rootElem, XmlStatusConstants.FOOTNOTE,
		       statusTable.getTitleFootnote());
      }
      /*
       * Column descriptor, row, and summary information
       */
      List colDesc = statusTable.getColumnDescriptors();
      if (colDesc != null) {
	addColumnDescriptors(rootElem, colDesc);
	addRows(rootElem, colDesc);
      }
      addSummaryInformation(rootElem, statusTable.getSummaryInfo());
      return doc;
    } catch (Exception e) {
      logger.warning("Error building XML status table", e);
      throw new XmlDomBuilder.XmlDomException("Error building XML status table");
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
      Element cdElement =
	xmlBuilder.createElement(rootElem,
				 XmlStatusConstants.COLUMNDESCRIPTOR);

      addTextElement(cdElement, XmlStatusConstants.NAME, cd.getColumnName());
      addTextElement(cdElement, XmlStatusConstants.TYPE,
		     Integer.toString(cd.getType()));
      addTextElement(cdElement, XmlStatusConstants.TITLE, cd.getTitle());
      if (cd.getFootnote() != null) {
	addTextElement(cdElement,
		       XmlStatusConstants.FOOTNOTE, cd.getFootnote());
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
    if (rowList == null) {
      return;
    }
    int colSize = colList.size();
    String[] colNames = new String[colSize];
    int[] colTypes = new int[colSize];
    for (int ii=0; ii<colList.size(); ii++) {
      ColumnDescriptor cd = (ColumnDescriptor)colList.get(ii);
      colNames[ii] = cd.getColumnName();
      colTypes[ii] = cd.getType();
    }

    Iterator rowIter = rowList.iterator();
    while (rowIter.hasNext()) {
      Map rowMap = (Map)rowIter.next();

      // create row element
      Element rowElement =
	xmlBuilder.createElement(rootElem, XmlStatusConstants.ROW);

      if (rowMap.get(StatusTable.ROW_SEPARATOR) != null) {
	xmlBuilder.createElement(rowElement, XmlStatusConstants.ROW_SEPARATOR);
      }
      for (int ii=0; ii<colNames.length; ii++) {
        // get the value for each column
        String colName = colNames[ii];
        Object object = rowMap.get(colName);
	if (object != null) {
	  addCellElement(rowElement, object, colName, colTypes[ii]);
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
    if (summaryInfo == null || summaryInfo.isEmpty()) {
      return;
    }

    Iterator sumIter = summaryInfo.iterator();
    while (sumIter.hasNext()) {
      StatusTable.SummaryInfo si = (StatusTable.SummaryInfo)sumIter.next();
      Element siElement =
	xmlBuilder.createElement(rootElem, XmlStatusConstants.SUMMARYINFO);

      addTextElement(siElement, XmlStatusConstants.TITLE, si.getTitle());
      addTextElement(siElement, XmlStatusConstants.TYPE,
		     Integer.toString(si.getType()));
      if (si.getHeaderFootnote() != null) {
	addTextElement(siElement,
		       XmlStatusConstants.FOOTNOTE, si.getHeaderFootnote());
      }
      if (si.getValue() != null) {
	addValueElement(siElement, si.getValue(), si.getType());
      }
    }
  }

  /** Add element of type to parent, with text value  */
  void addTextElement(Element parent, String type, String value) {
    Element element = xmlBuilder.createElement(parent, type);
    if (value != null) {
      XmlDomBuilder.addText(element, value);
    }
  }

  /** Add cell element to parent, with embedded value */
  void addCellElement(Element parent, Object value,
		      String colName, int type) {
    Element element =
      xmlBuilder.createElement(parent, XmlStatusConstants.CELL);
    addTextElement(element, XmlStatusConstants.COLUMN_NAME, colName);
    addValueElement(element, value, type);
  }

  /** Add value element to parent.  If list, add multiple values  */
  void addValueElement(Element parent, Object value, int type) {
    // XXX List can now be embedded in DisplayedValue.  Handle specially at
    // top level until fully integrated.
    if (value instanceof StatusTable.DisplayedValue) {
      StatusTable.DisplayedValue dv = (StatusTable.DisplayedValue)value;
      if (dv.getValue() instanceof Collection) {
	addValueElement(parent, dv.getValue(), type);
	return;
      }
    }
    if (value instanceof Collection) {
      for (Iterator iter = ((List)value).iterator(); iter.hasNext(); ) {
	addLinkValueElement(parent, iter.next(), type);
      }
    } else {
      addLinkValueElement(parent, value, type);
    }
  }

  /** Add value element to parent, possibly embedding in reference element */
  void addLinkValueElement(Element parent, Object value, int type) {
    if (value instanceof StatusTable.Reference) {
      addReferenceValueElement(parent, (StatusTable.Reference)value, type);
//     } else if (value instanceof StatusTable.SrvLink) {
//       addSrvLinkValueElement(parent, (StatusTable.SrvLink)value, type);
    } else if (value instanceof StatusTable.LinkValue) {
      // A LinkValue type we don't know about.  Just display its embedded
      // value.
      addNonLinkValueElement(parent, StatusTable.getActualValue(value), type);
    } else {
      addNonLinkValueElement(parent, value, type);
    }
  }

  void addReferenceValueElement(Element parent, StatusTable.Reference refVal, int type) {
    Element element =
      xmlBuilder.createElement(parent, XmlStatusConstants.VALUE);
    Element refElement =
      xmlBuilder.createElement(element, XmlStatusConstants.REFERENCE_ELEM);
    addTextElement(refElement, XmlStatusConstants.NAME, refVal.getTableName());
    if (refVal.getKey() != null) {
      addTextElement(refElement, XmlStatusConstants.KEY, refVal.getKey());
    }
    Properties refProps = refVal.getProperties();
    if (refVal.getPeerId() != null) {
      refElement.setAttribute(XmlStatusConstants.PEERID,
			      refVal.getPeerId().getIdString());
      // XXX 8081 shouldn't be hardwired
      refElement.setAttribute(XmlStatusConstants.URL_STEM,
			      refVal.getPeerId().getUiUrlStem(8081/*reqURL.getPort()*/));
    }
    if (refProps != null) {
      for (Iterator iter = refProps.entrySet().iterator(); iter.hasNext(); ) {
	Map.Entry ent = (Map.Entry)iter.next();
	Element propElement =
	  xmlBuilder.createElement(refElement, XmlStatusConstants.PROPERTY);
	propElement.setAttribute(XmlStatusConstants.PROP_NAME,
				 ent.getKey().toString());
	XmlDomBuilder.addText(propElement, formatByType(ent.getValue(), type));
      }
    }
    addNonLinkValueElement(refElement, refVal.getValue(), type);
  }

  /** Add value element to parent, with any display attributes */
  void addNonLinkValueElement(Element parent, Object value, int type) {
    Object dval = getValueToDisplay(value);
    if (dval != StatusTable.NO_VALUE) {
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
	switch (outputVersion) {
	case 1:
	default:
	  if (dv.hasDisplayString()) {
	    // If an explicit display string was supplied, use it w/out formatting
	    XmlDomBuilder.addText(element, dv.getDisplayString());
	    return;
	  }
	case 2:
	  break;			// fall thru
	}
      }
      XmlDomBuilder.addText(element, formatByType(dval, type));
    }
  }

  Object getValueToDisplay(Object value) {
    if (value instanceof StatusTable.DisplayedValue) {
      StatusTable.DisplayedValue dv = (StatusTable.DisplayedValue)value;
      switch (outputVersion) {
      case 1:
      default:
	if (dv.hasDisplayString()) {
	  return dv.getDisplayString();
	} else {
	  return dv.getValue();
	}
      case 2:
	return dv.getValue();
      }
    } else {
      return value;
    }
  }

  String formatByType(Object object, int type) {
    String str;
    switch (outputVersion) {
    case 1:
    default:
      str = getDisplayConverter().convertDisplayString(object, type);
      break;
    case 2:
      if (object instanceof Date) {
	str = Long.toString(((Date)object).getTime());
      } else if (object instanceof Deadline) {
	str = Long.toString(((Deadline)object).getExpirationTime());
      } else 
	str = object.toString();
      break;
    }
//     if (type == ColumnDescriptor.TYPE_STRING) {
//       str = StringEscapeUtils.escapeXml(str); 
//     }
    return str;
  }

  DisplayConverter dispConverter;

  private DisplayConverter getDisplayConverter() {
    if (dispConverter == null) {
      dispConverter = new XmlDisplayConverter();
    }
    return dispConverter;
  }

  class XmlDisplayConverter extends DisplayConverter {
    protected Format getTableDateFormat() {
      return dateFmt;
    }
  }

}
