/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

*/

package org.lockss.laaws.model.cfg;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.lockss.laaws.model.rs.PageInfo;

/**
 * A page of AU configurations, as returned by the configuration service's
 * paginated GET /aus (laaws-configservice issue 569).  The page information
 * has the same shape as the repository service's.
 */
public class AuConfigPageInfo implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final String SERIALIZED_NAME_AU_CONFIGS = "auConfigs";
  @SerializedName(SERIALIZED_NAME_AU_CONFIGS)
  private List<AuConfiguration> auConfigs = new ArrayList<>();

  public static final String SERIALIZED_NAME_PAGE_INFO = "pageInfo";
  @SerializedName(SERIALIZED_NAME_PAGE_INFO)
  private PageInfo pageInfo;

  public AuConfigPageInfo() {
  }

  /** The AU configurations in the page */
  public List<AuConfiguration> getAuConfigs() {
    return auConfigs;
  }

  public void setAuConfigs(List<AuConfiguration> auConfigs) {
    this.auConfigs = auConfigs;
  }

  public AuConfigPageInfo addAuConfigsItem(AuConfiguration auConfig) {
    this.auConfigs.add(auConfig);
    return this;
  }

  /** The page information; its continuation token is null on the last
   * page */
  public PageInfo getPageInfo() {
    return pageInfo;
  }

  public void setPageInfo(PageInfo pageInfo) {
    this.pageInfo = pageInfo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuConfigPageInfo other = (AuConfigPageInfo) o;
    return Objects.equals(auConfigs, other.auConfigs)
      && Objects.equals(pageInfo, other.pageInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(auConfigs, pageInfo);
  }

  @Override
  public String toString() {
    return "AuConfigPageInfo[auConfigs=" + auConfigs
      + ", pageInfo=" + pageInfo + "]";
  }
}
