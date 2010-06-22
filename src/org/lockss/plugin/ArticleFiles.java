/*
 * $Id: ArticleFiles.java,v 1.2 2010-06-22 08:59:59 tlipkis Exp $
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
 * Describes the files comprising one <i>article</i> in an AU, or a subset
 * of those files.  An <i>article</i> is defined as the smallest object
 * that has metadata.
 */
public class ArticleFiles {

  /** Role for CU that holds metadata */
  public static final String ROLE_ARTICLE_METADATA = "ArticleMetadata";
  /** Role for CU that holds metadata for larger entity such as issue */
  public static final String ROLE_ISSUE_METADATA = "IssueMetadata";
  /** Role for handle (name, index, etc.) to article metadata within issue
   * metadata file */
  public static final String ROLE_ARTICLE_HANDLE = "ArticleHandle";

  protected CachedUrl fullTextCu;

//   protected Map<String,CachedUrl> roleCus = new HashMap<String,CachedUrl>();
  protected Map<String,Object> roles = new HashMap<String,Object>();

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

  /** Set the CachedUrl associated with an article role
   * @param key the name of the role
   * @param cu the CachedUrl for the file filling that role
   */
  public void setRoleCu(String key, CachedUrl cu) {
    roles.put(key, cu);
  }

  /** Get the CU associated with the role
   * @param key the name of the role
   */
  public CachedUrl getRoleCu(String key) {
    return (CachedUrl)roles.get(key);
  }

  /** Set the String associated with an article role
   * @param key the name of the role
   * @param str the String filling that role
   */
  public void setRoleString(String key, String str) {
    roles.put(key, str);
  }

  /** Get the String associated with the role
   * @param key the name of the role
   */
  public String getRoleString(String key) {
    return (String)roles.get(key);
  }

  /** Set the Object associated with an article role
   * @param key the name of the role
   * @param obj the Object to associate with the role
   */
  public void setRole(String key, Object obj) {
    roles.put(key, obj);
  }

  /** Get the Object associated with the role
   * @param key the name of the role
   */
  public Object getRole(String key) {
    return roles.get(key);
  }

  /** Get the URL associated with the role
   * @param key the name of the role
   */
  public String getRoleUrl(String key) {
    return urlOrNull(getRoleCu(key));
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[af: ft=");
    sb.append(getFullTextCu());
    sb.append(" map=");
    sb.append(roles);
    sb.append("]");
    return sb.toString();
  }
}
