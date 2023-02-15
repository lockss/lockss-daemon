/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang3.tuple.Pair;
import org.lockss.crawler.PermissionRecord.PermissionStatus;
import org.lockss.daemon.Crawler;
import org.lockss.daemon.PermissionChecker;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.CharsetUtil;
import org.lockss.util.CharsetUtil.InputStreamAndCharset;
import org.lockss.util.Logger;
import org.lockss.util.StreamUtil;

public class PermissionUrlConsumer extends SimpleUrlConsumer {
  static Logger logger = Logger.getLogger(PermissionUrlConsumer.class);
  protected PermissionMap permMap;
  protected String charset;
  protected InputStream fudis;
  protected boolean strmNeedsReset = false;
  
  protected enum PermissionLogic {
    OR_CHECKER, AND_CHECKER
  }
  
  public PermissionUrlConsumer(Crawler.CrawlerFacade crawlFacade,
      FetchedUrlData fud, PermissionMap permMap) {
    super(crawlFacade, fud);
    this.permMap = permMap;
    charset = AuUtil.getCharsetOrDefault(fud.headers);
  }
  
  public void consume() throws IOException {
    boolean permOk = false;
    // if we didn't find at least one required lockss permission - fail.
    fudis = StreamUtil.getResettableInputStream(fud.getInputStream());
    // allow us to reread contents if reasonable size
    fudis.mark(crawlFacade.permissionStreamResetMax());

    if (!checkPermission(permMap.getDaemonPermissionCheckers(),
        PermissionLogic.OR_CHECKER)) {
      logger.siteError("No (C)LOCKSS crawl permission on " + fud.fetchUrl);
      permMap.setPermissionResult(fud.fetchUrl,
          PermissionStatus.PERMISSION_NOT_OK);
    } else {
      Collection<PermissionChecker> pluginPermissionCheckers = 
          permMap.getPluginPermissionCheckers();
      if (pluginPermissionCheckers != null && 
          !pluginPermissionCheckers.isEmpty()) {
        if (!checkPermission(pluginPermissionCheckers,
            PermissionLogic.AND_CHECKER)) {
          logger.siteError("No plugin crawl permission on " + fud.fetchUrl);
          permMap.setPermissionResult(fud.fetchUrl,
              PermissionStatus.PERMISSION_NOT_OK);
        } else {
          permOk = true;
        }
      } else {
        permOk = true;
      }
    }
    if (permOk) {
      permMap.setPermissionResult(fud.fetchUrl,
				  PermissionStatus.PERMISSION_OK);
      fud.resetInputStream();
      super.consume();
    }
  }
  
  protected boolean checkPermission(Collection<PermissionChecker> permCheckers,
				    PermissionLogic logic)
      throws IOException {

    PermissionChecker checker;

    String contentEncoding =
      fud.headers.getProperty(CachedUrl.PROPERTY_CONTENT_ENCODING);

    // check the lockss checkers and find at least one checker that matches
    for (Iterator<PermissionChecker> it = permCheckers.iterator();
        it.hasNext(); ) {
      checker = it.next();
      // reset stream if not first time through
      if (strmNeedsReset) {
	try {
	  fudis.reset();
	} catch(IOException ex) {
	  //unable to reset for some reason
	  fud.resetInputStream();
	  fudis = StreamUtil.getResettableInputStream(fud.getInputStream());
	}
      } else {
	strmNeedsReset = true;
      }

      // decompress if necessary
      InputStream is =
	StreamUtil.getUncompressedInputStreamOrFallback(fudis,
							contentEncoding,
							fud.fetchUrl);

      // XXX Some PermissionCheckers close their stream.  This is a
      // workaround until they're fixed.
      Reader reader;
      if(CharsetUtil.inferCharset()) {
        InputStreamAndCharset isc = CharsetUtil.getCharsetStream(is, charset);
        charset = isc.getCharset();
        is = isc.getInStream();
      }

      reader = new InputStreamReader(new StreamUtil.IgnoreCloseInputStream(is),
				     charset);
      boolean perm = checker.checkPermission(crawlFacade, reader, fud.fetchUrl);

      if (perm) {
        if(logic == PermissionLogic.OR_CHECKER){
          logger.debug3("Found permission on "+checker);
          return true; //we just need one permission to be successful here
        }
      } else if(logic == PermissionLogic.AND_CHECKER) {
        //All permissions must be successful fail
        return false;
      }
      if (!it.hasNext() && logic == PermissionLogic.AND_CHECKER) {
        //All permissions have been successfull and we reached the end
        //An empty and checker will return true
        return true;
      }
    }
    //reached the end without finding permission
    return false; 
  }
}
