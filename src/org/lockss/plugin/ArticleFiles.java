/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.commons.collections.map.UnmodifiableMap;
import org.lockss.util.StringUtil;


/**
 * Describes the files comprising one <i>article</i> in an AU, or a subset
 * of those files.  An <i>article</i> is defined as the smallest object
 * that has metadata.
 */
public class ArticleFiles {

  /** Role for the CU representing the article's abstract, if
   * applicable */
  public static final String ROLE_ABSTRACT = "Abstract";
  
  /** Role for the CU representing the article's references,
   * if different from the full text HTML */
  public static final String ROLE_REFERENCES = "References";
  
  /** Role for the CU representing a citation to this article,
   * If applicable, use as a prefix if multiple types are for
   * this role are present. Keep the suffix shorter than 20 characters
   * to avoid hitting the database field limit of 32
   * */
  public static final String ROLE_CITATION = "Citation";
  
  /* Role for the CU representing a citation to this article in the Bibtex format */
  public static final String ROLE_CITATION_BIBTEX = "CitationBibtex";
  
  /* Role for the CU representing a citation to this article in the RIS format */  
  public static final String ROLE_CITATION_RIS = "CitationRis";
  
  /* Role for the CU representing a citation to this article in the Endnote format */
  public static final String ROLE_CITATION_ENDNOTE = "CitationEndnote";
  
  /** Role for the CU representing the article's supplementary
   * materials, if applicable */
  public static final String ROLE_SUPPLEMENTARY_MATERIALS = "SupplementaryMaterials";
  
  /** Role for the CU representing the article's figures and tables,
   * if there is a single CU just for them */
  public static final String ROLE_FIGURES_TABLES = "FiguresTables";
  
  /** Role for the CU representing the article's full text HTML */
  public static final String ROLE_FULL_TEXT_HTML = "FullTextHtml";
  
  /** Role for the CU representing the article's full text XML */
  public static final String ROLE_FULL_TEXT_XML = "FullTextXml";
  
  /** Role for the CU representing the article's full text EPUB
   * @since 1.60 */
  public static final String ROLE_FULL_TEXT_EPUB = "FullTextEpub";
  
  /** Role for the CU representing the article's full text Mobile
   * @since 1.61 */
  public static final String ROLE_FULL_TEXT_MOBILE = "FullTextMobile";

  /** Role for the CU representing an HTML page containing or
   * otherwise linking to the article's full text HTML, if
   * applicable */
  public static final String ROLE_FULL_TEXT_HTML_LANDING_PAGE = "FullTextHtmlLanding";
  
  /** Role for the CU representing the article's full text PDF file;
   * must be the CU of an actual PDF file */
  public static final String ROLE_FULL_TEXT_PDF = "FullTextPdfFile";
  
  /** Role for the CU representing an HTML page containing or
   * otherwise linking to the article's full text PDF, if
   * applicable */
  public static final String ROLE_FULL_TEXT_PDF_LANDING_PAGE = "FullTextPdfLanding";
  
  /** Role for CU that holds article-level metadata */
  public static final String ROLE_ARTICLE_METADATA = "ArticleMetadata";
  
  /** Role for CU that holds issue-level metadata if applicable */
  public static final String ROLE_ISSUE_METADATA = "IssueMetadata";
  
  /** Role for an object (String, array, list, etc.) used to identify
   * the article inside aggregate metadata (e.g. issue-level metadata)
   * by identifier, index, or any other scheme applicable */
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
    return (CachedUrl)getRole(key);
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
    return (String)getRole(key);
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

  /** Return true if no URLs have been stored */
  public boolean isEmpty() {
    return fullTextCu == null && roles.isEmpty();
  }

  /** Return an unmodifiable view of the role map */
  public Map<String,Object> getRoleMap() {
    return (Map<String,Object>)UnmodifiableMap.decorate(roles);
  }

  /**
   * Returns a string version of the value of the given key. If null, returns
   * null; if a CachedUrl, returns the URL string it represents; otherwise
   * calls {@link Object#toString()}.
   * @param key A role key.
   * @return A string (or null).
   */
  public String getRoleAsString(String key) {
    Object obj = getRole(key);
    if (obj == null) {
      return null;
    }
    if (obj instanceof CachedUrl) {
      return ((CachedUrl)obj).getUrl();
    }
    return obj.toString();
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

  /** Return a pretty printed String */
  public String ppString(int indent) {
    StringBuilder sb = new StringBuilder();
    sb.append(StringUtil.tab(indent));
    sb.append("ArticleFiles\n");
    String tab = StringUtil.tab(indent + 2);
    sb.append(tab);
    sb.append("Full text CU:  ");
    sb.append(getFullTextUrl());
    sb.append("\n");
    for (String role : StringUtil.caseIndependentSortedSet(roles.keySet())) {
      sb.append(tab);
      sb.append(role);
      sb.append(":  ");
      sb.append(getRoleAsString(role));
      sb.append("\n");
    }
    return sb.toString();
  }

}
