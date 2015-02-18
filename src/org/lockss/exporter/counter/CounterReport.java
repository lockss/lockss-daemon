/*
 * $Id$
 */

/*

 Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * A representation of a COUNTER report.
 * 
 * @version 1.0
 * 
 */
package org.lockss.exporter.counter;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public interface CounterReport {

  /**
   * Provides the name of a report file.
   * 
   * @param extension
   *          A String with the file extension to be used.
   * @return a String with the report file name. The format is
   *         'Report_Name-YYYYStart_MMStart-YYYYEnd_MMEnd-YYYYNow-MMNow-DDNow'.
   */
  String getReportFileName(String extension);

  /**
   * Saves the report in a CSV-formatted file.
   * 
   * @return a File with the report output file.
   * @throws SQLException
   * @throws IOException
   * @throws Exception
   */
  File saveCsvReport() throws SQLException, IOException, Exception;

  /**
   * Saves the report in a TSV-formatted file.
   * 
   * @return a File with the report output file.
   * @throws SQLException
   * @throws IOException
   * @throws Exception
   */
  File saveTsvReport() throws SQLException, IOException, Exception;

  /**
   * Writes the report to the log. For debugging purposes only.
   * 
   * @throws SQLException
   * @throws Exception
   */
  void logReport() throws SQLException, Exception;
}
