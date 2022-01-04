/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ojs3;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.util.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ojs3UrlNormalizer implements UrlNormalizer {

  protected static Logger log = Logger.getLogger(Ojs3UrlNormalizer.class);

  // https://jcofarts.uobaghdad.edu.iq/index.php/jcofarts/article/download/478/335/1792
  // should be
  // https://jcofarts.uobaghdad.edu.iq/index.php/jcofarts/article/download/478/335
  protected static final Pattern PDF_DOWNLOAD_PATTERN = Pattern.compile("/article/download/\\d+/\\d+/\\d+$");

  @Override
  public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {


    Matcher isPdfWExtra = PDF_DOWNLOAD_PATTERN.matcher(url);

    if (isPdfWExtra.find()) {
      // found a matching extraneous integer, strip it
      url = url.substring(0, url.lastIndexOf("/"));
    }
    return url;
  }
}
