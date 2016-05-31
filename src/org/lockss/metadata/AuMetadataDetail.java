/*

 Copyright (c) 2016 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The extracted metadata for an Archival Unit.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class AuMetadataDetail {
  private List<ArticleMetadataDetail> articles =
      new ArrayList<ArticleMetadataDetail>();

  public List<ArticleMetadataDetail> getArticles() {
    return articles;
  }

  public void setArticles(List<ArticleMetadataDetail> articles) {
    this.articles = articles;
  }

  @Override
  public String toString() {
    return "[AuMetadataDetail articles=" + articles + "]";
  }

  public class ArticleMetadataDetail {
    private Map<String, String> scalarMap = new HashMap<String, String>();
    private Map<String, List<String>> listMap =
	new HashMap<String, List<String>>();

    private Map<String, Map<String, String>> mapMap =
	new HashMap<String, Map<String, String>>();

    public ArticleMetadataDetail() {
      
    }

    public Map<String, String> getScalarMap() {
      return scalarMap;
    }

    public void setScalarMap(Map<String, String> scalarMap) {
      this.scalarMap = scalarMap;
    }

    public Map<String, List<String>> getListMap() {
      return listMap;
    }

    public void setListMap(Map<String, List<String>> listMap) {
      this.listMap = listMap;
    }

    public Map<String, Map<String, String>> getMapMap() {
      return mapMap;
    }

    public void setMapMap(Map<String, Map<String, String>> mapMap) {
      this.mapMap = mapMap;
    }

    @Override
    public String toString() {
      return "[ArticleMetadataDetail scalarMap=" + scalarMap + ", listMap="
	  + listMap + ", mapMap=" + mapMap + "]";
    }
  }
}
