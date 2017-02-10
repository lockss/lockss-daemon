/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.plugin.*;
import org.lockss.util.*;
import static org.lockss.crawler.CrawlerStatus.UrlErrorInfo;
import static org.lockss.crawler.CrawlerStatus.ReferrerType;

public class CrawlUrlsStatusAccessor implements StatusAccessor {
  static Logger log = Logger.getLogger("CrawlUrlsStatusAccessor");

  private static final String URL = "url";
  private static final String IX = "ix";
  private static final String CRAWL_ERROR = "crawl_error";
  private static final String CRAWL_SEVERITY = "crawl_severity";
  private static final String REFERRER = "referrer";
  private static final String REASON = "reason";

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
    ListUtil.list(new ColumnDescriptor(CRAWL_SEVERITY, "Severity",
				       ColumnDescriptor.TYPE_STRING,
				       "Errors and Fatal errors cause the crawl to fail, Warnings do not."),
		  new ColumnDescriptor(URL, "URL",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor(CRAWL_ERROR, "Error",
				       ColumnDescriptor.TYPE_STRING));

  private static List colDescsExcluded =
    ListUtil.list(new ColumnDescriptor(URL, "URL Excluded",
				       ColumnDescriptor.TYPE_STRING));

  private static List colDescsExcludedWithReason =
    ListUtil.list(new ColumnDescriptor(URL, "URL Excluded",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor(REASON, "Reason",
				       ColumnDescriptor.TYPE_STRING));

  private static ColumnDescriptor REFERRER_FIRST_COLDESC =
    new ColumnDescriptor(REFERRER, "First Referrer",
			 ColumnDescriptor.TYPE_STRING);

  private static ColumnDescriptor REFERRER_ALL_COLDESC =
    new ColumnDescriptor(REFERRER, "Referrers", ColumnDescriptor.TYPE_STRING);


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
    CrawlerStatus status;
    String tableStr;

    status = statusSource.getStatus().getCrawlerStatus(getStatusKeyFromTableKey(key));
    tableStr = getTableStrFromKey(key);
    if (status == null) {
      throw new StatusService.NoSuchTableException("Status info from that crawl is no longer available");
    }
    table.setDefaultSortRules(statusSortRules);
    setColumnDescriptors(table, getColDescs(tableStr, status), status);
    table.setTitle(getTableTitle(status, tableStr));
    boolean includeReferrers =
      table.isIncludeColumn(REFERRER) && status.hasReferrers();
    table.setRows(makeRows(status, tableStr, includeReferrers));
    table.setSummaryInfo(getSummaryInfo(table, status, tableStr));
  }

  void setColumnDescriptors(StatusTable table,
			    List<ColumnDescriptor> colDescs,
			    CrawlerStatus status) {
    List<ColumnDescriptor> cols =
      new ArrayList<ColumnDescriptor>(colDescs.size() + 1);
    List<String> defaultCols = new ArrayList<String>(colDescs.size());
    for (ColumnDescriptor desc : colDescs) {
      cols.add(desc);
      defaultCols.add(desc.getColumnName());
    }
    switch (status.getRecordReferrersMode()) {
    case First: 
      cols.add(REFERRER_FIRST_COLDESC);
      break;
    case All: 
      cols.add(REFERRER_ALL_COLDESC);
      break;
    }
    table.setColumnDescriptors(cols, defaultCols);
  }

  private String getTableTitle(CrawlerStatus status, String tableStr) {
    String auName = status.getAuName();
    if (FETCHED_TABLE_NAME.equals(tableStr)) {
      return "URLs fetched during crawl of "+auName;
    } else if (ERROR_TABLE_NAME.equals(tableStr)) {
      return "Errors during crawl of "+auName;
    } else if (NOT_MODIFIED_TABLE_NAME.equals(tableStr)) {
      return "URLs not modified during crawl of "+auName;
    } else if (PARSED_TABLE_NAME.equals(tableStr)) {
      return "URLs parsed during crawl of "+auName;
    } else if (PENDING_TABLE_NAME.equals(tableStr)) {
      return "URLs pending during crawl of "+auName;
    } else if (EXCLUDED_TABLE_NAME.equals(tableStr)) {
      return "URLs excluded during crawl of "+auName;
    } else if (MIMETYPES_TABLE_NAME.equals(getMtTableStr(tableStr))) {
      return "URLs found during crawl of "+auName;
         //   + " with Mime type value: "+ getMimeTypeStr(tableStr) ;
    }
     return "";
  }

  private String getTableStrFromKey(String key) {
    return key.substring(key.indexOf(".")+1);
  }
  private String getMimeTypeStr(String tableStr) {
    return tableStr.substring(tableStr.indexOf(":")+1);
  }
  private String getMtTableStr(String tableStr) {
    return tableStr.substring(0, tableStr.indexOf(":"));
  }
  private String getStatusKeyFromTableKey(String key) {
    return key.substring(0, key.indexOf("."));
  }

  private List<ColumnDescriptor> getColDescs(String tableStr,
					     CrawlerStatus status) {
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
      if (status.anyExcludedWithReason()) {
	  return colDescsExcludedWithReason;
	} else {
	  return colDescsExcluded;
	}
    } else if (MIMETYPES_TABLE_NAME.equals(getMtTableStr(tableStr))) {    
      return
	ListUtil.list(new ColumnDescriptor(URL, 
					   getMimeTypeStr(tableStr),
					   ColumnDescriptor.TYPE_STRING));
    }
    return null;
  }

  private List makeRows(CrawlerStatus status, String tableStr,
			boolean includeReferrers) {
    List rows = null;

    if (FETCHED_TABLE_NAME.equals(tableStr)) {
      rows = urlSetToRows(status.getUrlsFetched(), status, includeReferrers);
    } else if (NOT_MODIFIED_TABLE_NAME.equals(tableStr)) {
      rows = urlSetToRows(status.getUrlsNotModified(),
			  status, includeReferrers);
    } else if (PARSED_TABLE_NAME.equals(tableStr)) {
      rows = urlSetToRows(status.getUrlsParsed(), status, includeReferrers);
    } else if (PENDING_TABLE_NAME.equals(tableStr)) {
      rows = urlSetToRows(status.getUrlsPending(), status, includeReferrers);
    } else if (EXCLUDED_TABLE_NAME.equals(tableStr)) {
      Map<String,String> map = status.getUrlsExcludedMap();
      rows = new ArrayList(map.size());
      int ix = 1;
      for (Map.Entry<String,String> ent : map.entrySet()) {
 	rows.add(makeExcludedRow(ent.getKey(), ix++, ent.getValue(),
				 status, includeReferrers));
      }
    } else if (ERROR_TABLE_NAME.equals(tableStr)) {
      Map<String,UrlErrorInfo> errorMap = status.getUrlsErrorMap();
      rows = new ArrayList(errorMap.size());
      int ix = 1;
      for (Map.Entry<String,UrlErrorInfo> ent : errorMap.entrySet()) {
 	rows.add(makeErrorRow(ent.getKey(), ix++, ent.getValue(),
			      status, includeReferrers));
      }
    } else if (MIMETYPES_TABLE_NAME.equals(getMtTableStr(tableStr))) {
      rows = urlSetToRows(status.getUrlsOfMimeType(getMimeTypeStr(tableStr)),
			  status, includeReferrers);
    } 
    return rows;
  }

  /**
   * Take a set of URLs and make a row for each, where row{"URL"}=<url>
   */
  private List urlSetToRows(Collection urls,
			    CrawlerStatus status, boolean includeReferrers) {
    List rows = new ArrayList(urls.size());
    return urlSetToRows(rows, urls, status, includeReferrers);
  }

  /**
   * Take a set of URLs and make a row for each, where row{"URL"}=<url>
   */
  private List urlSetToRows(List rows, Collection urls,
			    CrawlerStatus status, boolean includeReferrers) {
    int ix = rows.size();
    for (Iterator it = urls.iterator(); it.hasNext();) {
      String url = (String)it.next();
      rows.add(makeRow(url, ix++, status, includeReferrers));
    }
    return rows;
  }

  private Map makeRow(String url, int ix,
		      CrawlerStatus status, boolean includeReferrers) {
    Map row = new HashMap();
    row.put(URL, url);
    row.put(IX, ix);
    if (includeReferrers) {
      Collection refs = status.getReferrers(url);
      if (refs.size() > 1) {
	row.put(REFERRER, 
		new StatusTable.DisplayedValue(refs)
		.setLayout(StatusTable.DisplayedValue.Layout.Column));
      } else {
	row.put(REFERRER, refs);
      }
    }
    return row;
  }

  private Map makeErrorRow(String url, int ix, CrawlerStatus.UrlErrorInfo ui,
			   CrawlerStatus status, boolean includeReferrers) {
    Map row = makeRow(url, ix, status, includeReferrers);
    row.put(CRAWL_ERROR, ui.getMessage());
    row.put(CRAWL_SEVERITY, ui.getSeverity().toString());
    return row;
  }

  private Map makeExcludedRow(String url, int ix, String reason,
			      CrawlerStatus status, boolean includeReferrers) {
    Map row = makeRow(url, ix, status, includeReferrers);
    if (!StringUtil.isNullString(reason)) {
      row.put(REASON, reason);
    }
    return row;
  }

  List getSummaryInfo(StatusTable table,
		      CrawlerStatus status,
		      String tableStr) {
    List summary = new ArrayList();
    boolean isExcluded = EXCLUDED_TABLE_NAME.equals(tableStr);
    if (isExcluded) {
      Collection excl = status.getUrlsExcluded();
      if (status.getNumExcludedExcludes() > 0) {
	String str = status.getNumExcludedExcludes() +
	  " or fewer additional off-site URLs were excluded but not listed";
	summary.add(new StatusTable.SummaryInfo(null,
						ColumnDescriptor.TYPE_STRING,
						str));
      }
    }
    if (table.isIncludeColumn(REFERRER)) {
      StatusTable.Reference link = 
	new StatusTable.Reference("Hide Referrers",
				  CrawlManagerImpl.CRAWL_URLS_STATUS_TABLE,
				  table.getKey());
      link.setProperty(StatusTable.PROP_COLUMNS, "");
      summary.add(new StatusTable.SummaryInfo(null,
					      ColumnDescriptor.TYPE_STRING,
					      link));
    } else {
      if (status.hasReferrersOfType(isExcluded ? ReferrerType.Excluded
				    : ReferrerType.Included)) {
	StatusTable.Reference link = 
	  new StatusTable.Reference("Show Referrers",
				    CrawlManagerImpl.CRAWL_URLS_STATUS_TABLE,
				    table.getKey());
	link.setProperty(StatusTable.PROP_COLUMNS, "all");
	summary.add(new StatusTable.SummaryInfo(null,
						ColumnDescriptor.TYPE_STRING,
						link));
      }
    }
    if (summary.isEmpty()) {
      return null;
    } else {
      return summary;
    }
  }


  public String getDisplayName() {
    throw new UnsupportedOperationException("No generic name for SingleCrawlStatus");
  }

  public boolean requiresKey() {
    return true;
  }
}
