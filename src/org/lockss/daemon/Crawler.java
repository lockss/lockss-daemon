/*
 * $Id: Crawler.java,v 1.10 2003-06-20 22:34:50 claire Exp $
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

package org.lockss.daemon;

import java.io.IOException;
import java.util.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * This interface is implemented by the generic LOCKSS daemon.
 * The plug-ins use it to call the crawler to actually fetch
 * content.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */
public interface Crawler {

  public static final int NEW_CONTENT = 0;
  public static final int REPAIR = 1;
  public static final int BACKGROUND = 2;

  /**
   * Initiate a crawl starting with all the urls in urls
   * @return true if the crawl was successful
   * @param deadline maximum time to spend on this crawl
   */
  public boolean doCrawl(Deadline deadline);

  /**
   * Return the number of urls that have been fetched by this crawler
   * @return number of urls that have been fetched by this crawler
   */
  public long getNumFetched();

  /**
   * Return the number of urls that have been parsed by this crawler
   * @return number of urls that have been parsed by this crawler
   */
  public long getNumParsed();

  /**
   * Return the time at which this crawl began
   * @return time at which this crawl began or -1 if it hadn't yet
   */
  public long getStartTime();

  /**
   * Return the time at which this crawl ended
   * @return time at which this crawl ended or -1 if it hadn't yet
   */
  public long getEndTime();

  /**
   * Return the AU that this crawler is crawling within
   * @return the AU that this crawler is crawling within
   */
  public ArchivalUnit getAU();

  /**
   * Returns the type of crawl
   * @return crawl type
   */
  public int getType();

  /**
   * Returns the starting urls for this crawler
   * @return starting urls for this crawler
   */
  public List getStartUrls();

}
