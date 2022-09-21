/*
 * $Id$
 */

/*

Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.casalini;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.util.Logger;


/**
 * This class is used to remove "man-made" "?unique_record_id="
 * some MARC record field part
 */
public class CasaliniJournalUrlNormalizer implements UrlNormalizer {

  protected static Logger log = Logger.getLogger(CasaliniJournalUrlNormalizer.class);


  private static final String TRUNCATATION = "?unique_record_id=";

  @Override
  public String normalizeUrl(String url, ArchivalUnit au)
          throws PluginException {
    if (url.contains(TRUNCATATION)) {

      log.debug3("Truncation before: url = " + url);

      url = url.substring(0,url.indexOf(TRUNCATATION) + 1);

      log.debug3("Truncation after: url = " + url);
    }
    return(url);
  }

}