/*
 * $Id: ArticleFiles.java,v 1.1 2010-06-17 18:47:19 tlipkis Exp $
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.util.*;


/**
 * Describes the files comprising one article in an AU.
 */
public class ArticleFiles {

  protected CachedUrl fullTextCu;

  protected Map<String,CachedUrl> roleCus = new HashMap<String,CachedUrl>();

  /** Set the primary full text URL
   * @param cu the CachedUrl for the primary full text file
   */
  public void setFullTextCu(CachedUrl cu) {
    fullTextCu = cu;
  }

  /** Get the primary full text CU
   */
  public CachedUrl getFullTextCu() {
    return fullTextCu;
  }

  /** Get the primary full text URL
   */
  public String getFullTextUrl() {
    return urlOrNull(fullTextCu);
  }

  private String urlOrNull(CachedUrl cu) {
    if (cu == null) {
      return null;
    }
    return cu.getUrl();
  }

  /** Set the URL associated with an article role
   * @param key the name of the role
   * @param cu the CachedUrl for the file filling that role
   */
  public void setRoleCu(String key, CachedUrl cu) {
    roleCus.put(key, cu);
  }

  /** Get the CU associated with the role
   * @param key the name of the role
   */
  public CachedUrl getRoleCu(String key) {
    return roleCus.get(key);
  }

  /** Get the URL associated with the role
   * @param key the name of the role
   */
  public String getRoleUrl(String key) {
    return urlOrNull(getRoleCu(key));
  }

  /** Return the role map; used by equals() */
  Map<String,CachedUrl> getRoleCuMap() {
    return roleCus;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[af: ft=");
    sb.append(getFullTextCu());
    sb.append(" map=");
    sb.append(roleCus);
    sb.append("]");
    return sb.toString();
  }
}
