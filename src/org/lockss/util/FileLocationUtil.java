/*
 * $Id: FileLocationUtil.java,v 1.1 2002-12-31 00:14:02 aalto Exp $
 *

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.util.*;
import org.lockss.daemon.ArchivalUnit;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Create a list of Object from a call list. */
public class FileLocationUtil {
    /**
     * Don't construct. */
    private FileLocationUtil() {
    }

    /**
     * mapAuToCacheLocation() is the name mapping method used by the
     * LockssRepository and HistoryRepository to resolve {@link ArchivalUnit}s
     * into directory names. This maps a given au to directories, using the
     * cache root as the base.  Given an AU with PluginId of 'plugin' and AuId
     * of 'au', it would return the string '<rootLocation>/plugin/au/'.
     * @param rootLocation the file root of the cache
     * @param au the ArchivalUnit to translate
     * @return the file cache location
     */
    public static String mapAuToFileLocation(String rootLocation, ArchivalUnit au) {
      StringBuffer buffer = new StringBuffer(rootLocation);
      if (!rootLocation.endsWith(File.separator)) {
        buffer.append(File.separator);
      }
      buffer.append(au.getPluginId());
      if (!au.getAUId().equals("")) {
        buffer.append(File.separator);
        buffer.append(au.getAUId());
      }
      buffer.append(File.separator);
      return buffer.toString();
    }

    /**
     * mapUrlToCacheLocation() is the method used to resolve urls into file names.
     * This maps a given url to a cache file location, using the cache root as
     * the base.  It creates directories which mirror the html string, so
     * 'http://www.journal.org/issue1/index.html' would be cached in the file:
     * <rootLocation>/www.journal.org/http/issue1/index.html
     * @param rootLocation the file root of the cache
     * @param urlStr the url to translate
     * @return the file cache location
     * @throws java.net.MalformedURLException
     */
    public static String mapUrlToFileLocation(String rootLocation, String urlStr)
        throws MalformedURLException {
      int totalLength = rootLocation.length() + urlStr.length();
      URL url = new URL(urlStr);
      StringBuffer buffer = new StringBuffer(totalLength);
      buffer.append(rootLocation);
      if (!rootLocation.endsWith(File.separator)) {
        buffer.append(File.separator);
      }
      buffer.append(url.getHost());
      buffer.append(File.separator);
      buffer.append(url.getProtocol());
      buffer.append(StringUtil.replaceString(url.getPath(), "/", File.separator));
      return buffer.toString();
    }

}
