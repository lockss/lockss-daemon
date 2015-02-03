/*
 * $Id: ScUrlNormalizer.java,v 1.1 2015-02-03 03:07:30 thib_gc Exp $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair;

import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * <p>
 * URLs on Silverchair sites have inconsistent casing and ordering. The
 * canonical forms of various URLs are listed below.
 * </p>
 * <ul>
 * <li><code>http://www.example.com/article.aspx?articleid=1234444</code>
 * (all-lowercase "article" and "articleid"; no "atab")</li>
 * <li>http://www.example.com/Issue.aspx?issueid=926155&journalid=99
 * (capitalized "Issue", all-lowercase "journalid" and "issueid", "issueid"
 * first)</li>
 * </ul>
 */
public class ScUrlNormalizer implements UrlNormalizer {

  private static final Pattern ATAB_PATTERN = Pattern.compile("&atab=[^&]*", Pattern.CASE_INSENSITIVE);
  private static final String ATAB_CANONICAL = "";
  
  private static final Pattern ARTICLE_PATTERN = Pattern.compile("/article\\.aspx\\?articleid=(\\d+)", Pattern.CASE_INSENSITIVE);
  private static final String ARTICLE_CANONICAL = "/article.aspx?articleid=$1";

  private static final Pattern ISSUE_PATTERN = Pattern.compile("/Issue\\.aspx\\?(issueid=(\\d+)&journalid=(\\d+)|journalid=(\\d+)&issueid=(\\d+))", Pattern.CASE_INSENSITIVE);
  private static final String ISSUE_CANONICAL = "/Issue.aspx?issueid=$2$5&journalid=$3$4";
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    url = ATAB_PATTERN.matcher(url).replaceFirst(ATAB_CANONICAL);
    url = ARTICLE_PATTERN.matcher(url).replaceFirst(ARTICLE_CANONICAL);
    url = ISSUE_PATTERN.matcher(url).replaceFirst(ISSUE_CANONICAL);
    url = url.replace("\u2111", "&image"); // RU4998
    return url;
  }

}
