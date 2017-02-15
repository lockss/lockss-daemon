/*
 * $Id$
 */

/*

 Copyright (c) 2013-2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.entities;

/**
 * A wrapper for the parameters used to request a COUNTER report.
 */
public class CounterReportParams {
  private String id;
  private Integer startMonth;
  private Integer startYear;
  private Integer endMonth;
  private Integer endYear;
  private String type;
  private String format;

  /**
   * Provides the identifier of the requested report.
   * 
   * @return a String with the identifier.
   */
  public String getId() {
    return id;
  }

  /**
   * Provides the month at the start of the requested report period.
   * 
   * @return an Integer with the month (January = 1, December = 12).
   */
  public Integer getStartMonth() {
    return startMonth;
  }

  /**
   * Provides the year at the start of the requested report period.
   * 
   * @return an Integer with the year.
   */
  public Integer getStartYear() {
    return startYear;
  }

  /**
   * Provides the month at the end of the requested report period.
   * 
   * @return an Integer with the month (January = 1, December = 12).
   */
  public Integer getEndMonth() {
    return endMonth;
  }

  /**
   * Provides the year at the end of the requested report period.
   * 
   * @return an Integer with the year.
   */
  public Integer getEndYear() {
    return endYear;
  }

  /**
   * Provides the type of the requested report.
   * 
   * @return a String with the type (book or journal).
   */
  public String getType() {
    return type;
  }

  /**
   * Provides the format of the requested report.
   * 
   * @return a String with the format (CSV or TSV).
   */
  public String getFormat() {
    return format;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setStartMonth(Integer startMonth) {
    this.startMonth = startMonth;
  }

  public void setStartYear(Integer startYear) {
    this.startYear = startYear;
  }

  public void setEndMonth(Integer endMonth) {
    this.endMonth = endMonth;
  }

  public void setEndYear(Integer endYear) {
    this.endYear = endYear;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("CounterReportParams [id=");
    builder.append(id);
    builder.append(", startMonth=");
    builder.append(startMonth);
    builder.append(", startYear=");
    builder.append(startYear);
    builder.append(", endMonth=");
    builder.append(endMonth);
    builder.append(", endYear=");
    builder.append(endYear);
    builder.append(", type=");
    builder.append(type);
    builder.append(", format=");
    builder.append(format);
    builder.append("]");
    return builder.toString();
  }
}
