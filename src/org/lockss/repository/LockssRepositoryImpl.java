/*
 * $Id: LockssRepositoryImpl.java,v 1.2 2002-10-31 01:53:30 aalto Exp $
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

package org.lockss.repository;
import java.io.*;
import java.util.*;
import java.net.*;
import org.lockss.util.StringUtil;

/**
 * LockssRepository is used to organize the urls being cached.
 */
public class LockssRepositoryImpl implements LockssRepository {
  /**
   * Name of top directory in which the urls are cached.
   */
  public static final String CACHE_ROOT_NAME = "cache";

  private String rootLocation;

  public LockssRepositoryImpl(String rootPath) {
    rootLocation = rootPath;
    if (!rootLocation.endsWith(File.separator)) {
      rootLocation += File.separator;
    }
  }

  public RepositoryNode getRepositoryNode(String url) throws MalformedURLException {
//XXX cache
    String cacheLocation = rootLocation + mapUrlToCacheLocation(url);
    File entryDir = new File(cacheLocation);
    if (!entryDir.exists() || !entryDir.isDirectory()) {
      return null;
    }
    File leafFile = new File(entryDir, LeafNodeImpl.LEAF_FILE_NAME);
    if (leafFile.exists()) {
      return new LeafNodeImpl(url, cacheLocation);
    } else {
      return new InternalNodeImpl(url, cacheLocation, rootLocation);
    }
  }

  public LeafNode createLeafNode(String url) throws MalformedURLException {
    String cacheLocation = rootLocation + mapUrlToCacheLocation(url);
    return new LeafNodeImpl(url, cacheLocation);
  }

  /**
   * mapUrlToCacheFileName() is the name mapping method used by the LockssRepository.
   * This maps a given url to a cache file location.
   * It creates directories under a CACHE_ROOT location which mirror the html string.
   * So 'http://www.journal.org/issue1/index.html' would be cached in the file:
   * CACHE_ROOT/www.journal.org/http/issue1/index.html
   * @param urlStr the url to translate
   * @return the file cache location
   * @throws java.net.MalformedURLException
   */
  public static String mapUrlToCacheLocation(String urlStr) throws MalformedURLException {
    URL url = new URL(urlStr);
    StringBuffer buffer = new StringBuffer(CACHE_ROOT_NAME);
    buffer.append(File.separator);
    buffer.append(url.getHost());
    buffer.append(File.separator);
    buffer.append(url.getProtocol());
    buffer.append(StringUtil.replaceString(url.getPath(), "/", File.separator));
    return buffer.toString();
  }

}
