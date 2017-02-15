/*
 * $Id$
 */

/*

 Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.entities;

import java.util.Map;

/**
 * Container for the information related to a title database archival unit that
 * is the result of a query.
 */
public class TdbAuWsResult {
  private String auId;
  private String name;
  private String pluginName;
  private TdbTitleWsResult tdbTitle;
  private TdbPublisherWsResult tdbPublisher;
  private Boolean down;
  private Boolean active;
  private Map<String, String> params;
  private Map<String, String> attrs;
  private Map<String, String> props;

  public String getAuId() {
    return auId;
  }
  public void setAuId(String auId) {
    this.auId = auId;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getPluginName() {
    return pluginName;
  }
  public void setPluginName(String pluginName) {
    this.pluginName = pluginName;
  }
  public TdbTitleWsResult getTdbTitle() {
    return tdbTitle;
  }
  public void setTdbTitle(TdbTitleWsResult tdbTitle) {
    this.tdbTitle = tdbTitle;
  }
  public TdbPublisherWsResult getTdbPublisher() {
    return tdbPublisher;
  }
  public void setTdbPublisher(TdbPublisherWsResult tdbPublisher) {
    this.tdbPublisher = tdbPublisher;
  }
  public Boolean getDown() {
    return down;
  }
  public void setDown(Boolean down) {
    this.down = down;
  }
  public Boolean getActive() {
    return active;
  }
  public void setActive(Boolean active) {
    this.active = active;
  }
  public Map<String, String> getParams() {
    return params;
  }
  public void setParams(Map<String, String> params) {
    this.params = params;
  }
  public Map<String, String> getAttrs() {
    return attrs;
  }
  public void setAttrs(Map<String, String> attrs) {
    this.attrs = attrs;
  }
  public Map<String, String> getProps() {
    return props;
  }
  public void setProps(Map<String, String> props) {
    this.props = props;
  }

  @Override
  public String toString() {
    return "TdbAuWsResult [auId=" + auId + ", name=" + name + ", pluginName="
	+ pluginName + ", tdbTitle=" + tdbTitle + ", tdbPublisher="
	+ tdbPublisher + ", down=" + down + ", active=" + active + ", params="
	+ params + ", attrs=" + attrs + ", props=" + props + "]";
  }
}
