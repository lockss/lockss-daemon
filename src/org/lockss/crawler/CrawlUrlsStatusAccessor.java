/*
 * $Id: CrawlUrlsStatusAccessor.java,v 1.1 2006-10-17 04:36:49 adriz Exp $
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

package org.lockss.crawler;
import java.util.*;
import org.lockss.daemon.status.*;
import org.lockss.daemon.*;
// import org.lockss.app.*;
import org.lockss.plugin.*;
// import org.lockss.plugin.base.*;
import org.lockss.util.*;

public class CrawlUrlsStatusAccessor implements StatusAccessor {

  private static final String URL = "url";
  private static final String IX = "ix";
  private static final String CRAWL_ERROR = "crawl_error";

  private static final String FETCHED_TABLE_NAME = "fetched";
  private static final String ERROR_TABLE_NAME = "error";
  private static final String NOT_MODIFIED_TABLE_NAME = "not-modified";
  private static final String PARSED_TABLE_NAME = "parsed";
  private static final String PENDING_TABLE_NAME = "pending";
  private static final String EXCLUDED_TABLE_NAME = "excluded";
  private static final String MIMETYPES_TABLE_NAME = "mime-type";
   
  private static List colDescsFetched =
    ListUtil.list(new ColumnDescriptor(URL, "URL Fetched",
				       ColumnDescriptor.TYPE_STRING));

  private static List colDescsNotModified =
    ListUtil.list(new ColumnDescriptor(URL, "URL Not-Modified",
				       ColumnDescriptor.TYPE_STRING));

  private static List colDescsParsed =
    ListUtil.list(new ColumnDescriptor(URL, "URL Parsed",
				       ColumnDescriptor.TYPE_STRING));

 
  private static List colDescsPending =
    ListUtil.list(new ColumnDescriptor(URL, "URL Pending",
                                       ColumnDescriptor.TYPE_STRING));

  private static List colDescsError =
    ListUtil.list(new ColumnDescriptor(URL, "URL",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor(CRAWL_ERROR, "Error",
				       ColumnDescriptor.TYPE_STRING));

  private static List colDescsExcluded =
    ListUtil.list(new ColumnDescriptor(URL, "URL Excluded",
				       ColumnDescriptor.TYPE_STRING));

  private List colDescsMimeTypeUrls; 
 // =    ListUtil.list(new ColumnDescriptor(URL, "URL Found for the Mime-Type",
 //                                      ColumnDescriptor.TYPE_STRING));

  
  private static final List statusSortRules =
    ListUtil.list(new StatusTable.SortRule(IX, true));

  private CrawlManager.StatusSource statusSource;

  public CrawlUrlsStatusAccessor(CrawlManager.StatusSource statusSource) {
    this.statusSource = statusSource;
  }

  public void populateTable(StatusTable table)
      throws StatusService.NoSuchTableException{
    if (table == null) {
      throw new IllegalArgumentException("Called with null table");
    } else if (table.getKey() == null) {
      throw new IllegalArgumentException("CrawlUrlsStatusAccessor requires a key");
    }
    String key = table.getKey();
    Crawler.Status status;
    String tableStr;

      status = statusSource.getStatus().getCrawlStatus(getStatusKeyFromTableKey(key));
      tableStr = getTableStrFromKey(key);
     /* try {    } catch (Exception e) {
      throw new StatusService.NoSuchTableException("Malformed table key: " +
						   key);
    }*/
    if (status == null) {
      throw new StatusService.NoSuchTableException("Status info from that crawl is no longer available");
    }
    table.setDefaultSortRules(statusSortRules);
    table.setColumnDescriptors(getColDescs(tableStr, status));
    table.setTitle(getTableTitle(status, tableStr));
    table.setRows(makeRows(status, tableStr));
  }

  private String getTableTitle(Crawler.Status status, String tableStr) {
    ArchivalUnit au = status.getAu();
    if (FETCHED_TABLE_NAME.equals(tableStr)) {
      return "URLs fetched during crawl of "+au.getName();
    } else if (ERROR_TABLE_NAME.equals(tableStr)) {
      return "Errors during crawl of "+au.getName();
    } else if (NOT_MODIFIED_TABLE_NAME.equals(tableStr)) {
      return "URLs not modified during crawl of "+au.getName();
    } else if (PARSED_TABLE_NAME.equals(tableStr)) {
      return "URLs parsed during crawl of "+au.getName();
    } else if (PENDING_TABLE_NAME.equals(tableStr)) {
      return "URLs pending during crawl of "+au.getName();
    } else if (EXCLUDED_TABLE_NAME.equals(tableStr)) {
      return "URLs excluded during crawl of "+au.getName();
    } else if (MIMETYPES_TABLE_NAME.equals(getMtTableStr(tableStr))) {
      return "URLs found during crawl of "+au.getName();
         //   + " with Mime type value: "+ getMimeTypeStr(tableStr) ;
    }
     return "";
  }


  private String getTableStrFromKey(String key) {
    return key.substring(0, key.indexOf("."));
  }

  private String getMimeTypeStr(String tableStr) {
    return tableStr.substring(0, tableStr.indexOf(":"));
  }
  private String getMtTableStr(String tableStr) {
    return tableStr.substring(tableStr.indexOf(":")+1);
  }
  private String getStatusKeyFromTableKey(String key) {
    return key.substring(key.indexOf(".")+1);
  }

  private List getColDescs(String tableStr, Crawler.Status status) {
    if (FETCHED_TABLE_NAME.equals(tableStr)) {
      return colDescsFetched;
    } else if (ERROR_TABLE_NAME.equals(tableStr)) {
      return colDescsError;
    } else if (NOT_MODIFIED_TABLE_NAME.equals(tableStr)) {
      return colDescsNotModified;
    } else if (PARSED_TABLE_NAME.equals(tableStr)) {
      return colDescsParsed;
    } else if (PENDING_TABLE_NAME.equals(tableStr)) {
      return colDescsPending;
    } else if (EXCLUDED_TABLE_NAME.equals(tableStr)) {
      return colDescsExcluded;
    }else if (MIMETYPES_TABLE_NAME.equals(getMtTableStr(tableStr))) {    
        colDescsMimeTypeUrls = ListUtil.list(new ColumnDescriptor(URL, 
                                             getMimeTypeStr(tableStr),
                                             ColumnDescriptor.TYPE_STRING));
      return colDescsMimeTypeUrls;
    }
    return null;
  }

  private List makeRows(Crawler.Status status, String tableStr) {
    List rows = null;

    if (FETCHED_TABLE_NAME.equals(tableStr)) {
      rows = urlSetToRows(status.getUrlsFetched());
    } else if (NOT_MODIFIED_TABLE_NAME.equals(tableStr)) {
      rows = urlSetToRows(status.getUrlsNotModified());
    } else if (PARSED_TABLE_NAME.equals(tableStr)) {
      rows = urlSetToRows(status.getUrlsParsed());
    } else if (PENDING_TABLE_NAME.equals(tableStr)) {
      rows = urlSetToRows(status.getUrlsPending());
    } else if (EXCLUDED_TABLE_NAME.equals(tableStr)) {
      rows = urlSetToRows(status.getUrlsExcluded());
    } else if (MIMETYPES_TABLE_NAME.equals(getMtTableStr(tableStr))) {
      rows = urlSetToRows( status.getUrlsOfMimeType(getMimeTypeStr(tableStr)) );
    } else if (ERROR_TABLE_NAME.equals(tableStr)) {
      Map errorMap = status.getUrlsWithErrors();
      Set errorUrls = errorMap.keySet();
      rows = new ArrayList(errorUrls.size());
      int ix = 1;
      for (Iterator it = errorUrls.iterator(); it.hasNext();) {
	String url = (String)it.next();
 	rows.add(makeRow(url, ix++, (String)errorMap.get(url)));
      }
    }
    return rows;
  }

  /**
   * Take a set of URLs and make a row for each, where row{"URL"}=<url>
   */
  private List urlSetToRows(Collection urls) {
    List rows = new ArrayList(urls.size());
    int ix = 1;
    for (Iterator it = urls.iterator(); it.hasNext();) {
      String url = (String)it.next();
      rows.add(makeRow(url, ix++));
    }
    return rows;
  }

  private Map makeRow(String url, int ix) {
    Map row = new HashMap();
    row.put(URL, url);
    row.put(IX, new Integer(ix));
    return row;
  }

  private Map makeRow(String url, int ix, String error) {
    Map row = makeRow(url, ix);
    row.put(CRAWL_ERROR, error);
    return row;
  }

  public String getDisplayName() {
    throw new UnsupportedOperationException("No generic name for SingleCrawlStatus");
  }

  public boolean requiresKey() {
    return true;
  }
}
