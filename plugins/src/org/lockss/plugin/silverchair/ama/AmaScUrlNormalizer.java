/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair.ama;

import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * <p>
 * URLs on Silverchair sites have param ?linkid=12694817, which can be removed
 * </p>
 * <ul>
 * <li><code>http://jamanetwork.com/journals/jama/fullarticle/2174029?linkid=12694817</code></li>
 * <li>http://jamanetwork.com/journals/jama/fullarticle/2174029</li>
 * </ul>
 */
public class AmaScUrlNormalizer implements UrlNormalizer {

  private static final Pattern LINKID_PATTERN = Pattern.compile("[?]linkid=\\d+$", Pattern.CASE_INSENSITIVE);
  private static final String LINKID_CANONICAL = "";
  
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    url = LINKID_PATTERN.matcher(url).replaceFirst(LINKID_CANONICAL);
    return url;
  }
  
}
