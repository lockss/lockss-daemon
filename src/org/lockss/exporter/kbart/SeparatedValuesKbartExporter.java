/*
 * $Id$
 */

/*

Copyright (c) 2010-2012 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.lockss.util.Logger;
import org.lockss.util.StreamUtil;
import org.lockss.util.StringUtil;

/**
 * Exports records as fields separated by a separator character. By default 
 * this is a tab. When emitting records using a comma separator, fields are 
 * quoted if necessary, and quotes are escaped.
 * 
 * @author Neil Mayo
 *
 */
public class SeparatedValuesKbartExporter extends KbartExporter {

  private static Logger log = Logger.getLogger("SeparatedValuesKbartExporter");

  protected static final String SEPARATOR_TAB = "\t";
  protected static final String SEPARATOR_COMMA = ",";

  /** The separator that will be used to separate fields in the output. */
  private final String SEPARATOR;

  /**
   * Default constructor takes a list of KbartTitles to be exported.
   * 
   * @param titles the titles which are to be exported
   */
  public SeparatedValuesKbartExporter(List<KbartTitle> titles, 
      OutputFormat format) {
    this(titles, format, SEPARATOR_TAB);
  }

  /**
   * Constructor which allows the separator to be defined.
   * 
   * @param titles the titles which are to be exported
   */
  public SeparatedValuesKbartExporter(List<KbartTitle> titles, 
      OutputFormat format, String sep) {
    super(titles, format);
    this.SEPARATOR = sep;
  }

  @Override
  protected void setup(OutputStream os) throws IOException {
    // allow the default setup, but write a header line
    super.setup(os);
    // Write a byte-order mark (BOM) for excel 
    StreamUtil.writeUtf8ByteOrderMark(os);
    //printWriter.println( constructRecord(KbartTitle.Field.getLabels()) );
    //printWriter.println( constructRecord(filter.getVisibleFieldOrder()) );
  }

  @Override
  protected void emitHeader() {
    printWriter.println( constructRecord(filter.getColumnLabels(scope)) );
  }

  @Override
  protected void emitRecord(List<String> values) throws IOException { 
    printWriter.println( constructRecord(values) );
  }
  
  /**
   * Construct a CSV-formatted record from a list of string values.
   * @param values list of field values
   * @return a properly formatted CSV row representing the data
   */
  protected String constructRecord(List<String> values) {
    // If using a comma, encode as CSV with appropriate quoting and escaping
    if (SEPARATOR == SEPARATOR_COMMA) {
      return StringUtil.csvEncodeValues(values);
      /*StringBuilder sb = new StringBuilder();
      // Build the string for those values which need the separator appended
      for (int i=0; i<values.size()-1; i++) {
        sb.append(StringUtil.csvEncode(values.get(i).toString()) + SEPARATOR);
      }
      // Add the last item
      sb.append(StringUtil.csvEncode(values.get(values.size()-1).toString()));
      return sb.toString();*/
    }
    
    // By default, just join the fields together
    return StringUtil.separatedString(values, SEPARATOR);
  }
  
}

