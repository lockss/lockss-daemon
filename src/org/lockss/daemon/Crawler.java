/*
 * $Id: Crawler.java,v 1.6 2003-02-24 22:13:41 claire Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
  /**
   * Initiate a crawl starting with all the urls in urls
   * @param au ArchivalUnit that we are doing this crawl for
   * @param urls urls to start the crawl at; these will be refetched even
   * if they already exist
   * @param followLinks if true, we'll parse fetched urls to harvest more
   * @param deadline maximum time to spend on this crawl
   */

  public void doCrawl(ArchivalUnit au, List urls,
		      boolean followLinks, Deadline deadline);
}
