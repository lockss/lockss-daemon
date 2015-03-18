/*
 * $Id$
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

package org.lockss.plugin.projmuse;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/*
 * Trims this http://muse.jhu.edu/template2011/css/detail.css?v=1.1
 * to this http://muse.jhu.edu/template2011/css/detail.css
 */

public class ProjectMuseBooksUrlNormalizer extends ProjectMuseUrlNormalizer {
  
  protected static final String VERSION_SUFFIX = "?v=";

  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
    return StringUtils.substringBeforeLast(super.normalizeUrl(url, au), VERSION_SUFFIX);
  }

}
