/*
 * $Id: HtmlKbartExporter.java,v 1.12 2011-08-19 10:36:18 easyonthemayo Exp $
 */

/*

Copyright (c) 2010-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.exporter.kbart;

import java.util.Collections;
import java.util.List;
import java.io.OutputStream;
import java.io.IOException;

import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.util.StringUtil;
import org.lockss.util.Logger;

/**
 * An exporter that simply writes the records to an HTML table.
 * 
 * @author Neil Mayo
 */
public class HtmlKbartExporter extends KbartExporter {

  private static Logger log = Logger.getLogger("HtmlKbartExporter");

  /** 
   * Width in pixels of columns with variable content - namely 
   * publication_title and publisher_name. 
   */
  //private int variableContentColumnWidth = 120;
  /** A switch to keep track of whether a line is odd or even. */
  private boolean odd = false;
  /** The table header will be printed every <code>headerInterval</code> rows. */
  private int headerInterval = 20;
  /** A header row for the table. */
  private String header;
  /** Summary of the export for display. */
  private String exportSummary;
  
  /** 
   * The index of the issn field. Will have the td.issn style applied to its 
   * table cells to stop it wrapping. 
   */
  private int issnFieldIndex;
  /** 
   * The index of the eissn field. Will have the td.issn style applied to its 
   * table cells to stop it wrapping. 
   */
  private int eissnFieldIndex;
  /** 
   * The index of the issnl field. Will have the td.issn style applied to its 
   * table cells to stop it wrapping. 
   */
  private int issnlFieldIndex;
  
  /**
   * Default constructor takes a list of KbartTitles to be exported.
   * The exporter then does some reasonably costly processing, iterating 
   * through the titles in order to establish which columns are entirely 
   * empty.
   * 
   * @param titles the titles which are to be exported
   * @param format the output format
   */
  public HtmlKbartExporter(List<KbartTitle> titles, OutputFormat format) {
    super(titles, format);
  }
  
  public void sortByField(Field f) {
    Collections.sort(titles, KbartTitleComparatorFactory.getComparator(f));
  }
  
  @Override
  public void setFilter(KbartExportFilter filter) {
    super.setFilter(filter);
    // Use the filter to sort the titles for custom output 
    filter.sortTitlesByFirstTwoFields();
  }
  
  @Override
  protected void emitHeader() {
    printWriter.printf(this.header);
  }
  
  @Override
  protected void emitRecord(List<String> values) {
    odd = !odd;
    // Print a header every few rows
    if (exportCount % headerInterval == 0) {
      printWriter.printf(this.header); 
    }
    //printWriter.println( "<tr><td>" + StringUtil.separatedString(title.fieldValues(), SEPARATOR) + "</td></tr>" );
    printWriter.println("<tr class=\"" + (odd?"odd":"even") + "\">");
    
    // Add an index column at the start
    printWriter.printf("<td>%s</td>", this.exportCount);
    
    for (int i=0; i<values.size(); i++) {
      String val = values.get(i);
      if (StringUtil.isNullString(val)) val = "&nbsp;";
      // Add appropriate style to issn fields
      String cssClassParam = "";
      if (i==issnFieldIndex || i==eissnFieldIndex || i==issnlFieldIndex) {
	 cssClassParam = " class=\"issn\"";
      }
      printWriter.printf("<td%s>%s</td>", cssClassParam, val);
    }
    printWriter.println("</tr>");
  }

  @Override
  protected void setup(OutputStream os) throws IOException {
    super.setup(os);
    // Construct a title and summary
    this.exportSummary = String.format("Export of %s titles created on %s by %s " +
	"| Exported %d KBART titles from %d TDB titles.",
	scope, getDate(), getHostName(), titles.size(), tdbTitleTotal);
    this.header = makeHeader();
    this.issnFieldIndex = findFieldIndex(Field.PRINT_IDENTIFIER);
    this.eissnFieldIndex = findFieldIndex(Field.ONLINE_IDENTIFIER);
    this.issnlFieldIndex = findFieldIndex(Field.TITLE_ID);
    // Write html and head tags, including a metatag declaring the content type UTF-8
    printWriter.println("<html><head><meta http-equiv=\"Content-Type\" " +
	"content=\"text/html; charset="+DEFAULT_ENCODING+"\"/>");
    printWriter.printf("<title>%s</title>", this.exportSummary);
    printWriter.printf("%s</head><body>", css);
    if (getHtmlCustomForm()!=null) printWriter.println(getHtmlCustomForm());
    // Initial attempt to get a static header on the page:
    //printWriter.printf("<div class=\"header\"><table>%s</table></div>", this.header);
    printWriter.println("<table>");
    //printWriter.printf(this.header);
  }

  /**
   * Find the index of the named field in the current output. If the field
   * cannot be found, returns -1. 
   * @param field a field
   * @return the index ofthe named field, or -1
   */
  private int findFieldIndex(Field field) {
    List<String> labs = getFieldLabels();
    String name = field.getLabel();
    for (int i=0; i<labs.size(); i++) {
      if (labs.get(i).equals(name)) return i;
    }
    return -1;
  }
  
  private String makeHeader() {
    return String.format("<tr><th>Index</th><th>%s</th></tr>", 
	StringUtil.separatedString(filter.getColumnLabels(scope), "</th><th>")	
    );
  }

  @Override
  protected void clearup() throws IOException {
    printWriter.println("</table>");
    printWriter.printf("<br/><b><i>%s</i></b><br/>%s<br/>%s<br/>%s<br/><br/>", 
	this.exportSummary, 
	this.getOmittedTitlesSummary(),
	this.getOmittedFieldsSummary(), 
	this.getEmptySummary()
    );
    printWriter.println("</body></html>");
    // Finally let superclass clear up
    super.clearup();
  }

  /** Some CSS for the output table. */
  private static final String css;
  static {
    StringBuilder sb = new StringBuilder();
    sb.append("<style type=\"text/css\">");
    sb.append("div.header { position: fixed; top: 0; left: 0; width: 100%; z-index: 99; background-color: navy; color: white; }");
    sb.append("table, th, tr, td { border: 1px solid black; }");
    sb.append("th { font-size: x-small; }");
    sb.append("tr.odd { background-color: #cce; }");
    sb.append("tr.even { background-color: #aac; }");
    sb.append("td.issn { white-space: nowrap; }");
    sb.append("</style>");
    css = sb.toString();
  };
 
}
