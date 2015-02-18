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
package org.lockss.ws.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.lockss.config.TdbTitle;
import org.lockss.ws.entities.TdbPublisherWsResult;
import org.lockss.ws.entities.TdbTitleWsResult;

/**
 * Container for the information that is used as the source for a query related
 * to title database titles.
 */
public class TdbTitleWsSource extends TdbTitleWsResult {
  private TdbTitle tdbTitle;

  private boolean namePopulated = false;
  private boolean tdbPublisherPopulated = false;
  private boolean idPopulated = false;
  private boolean proprietaryIdsPopulated = false;
  private boolean publicationTypePopulated = false;
  private boolean issnPopulated = false;
  private boolean issnLPopulated = false;
  private boolean eIssnPopulated = false;
  private boolean printIssnPopulated = false;
  private boolean issnsPopulated = false;

  public TdbTitleWsSource(TdbTitle tdbTitle) {
    this.tdbTitle = tdbTitle;
  }

  @Override
  public String getName() {
    if (!namePopulated) {
      setName(tdbTitle.getName());
      namePopulated = true;
    }

    return super.getName();
  }

  @Override
  public TdbPublisherWsResult getTdbPublisher() {
    if (!tdbPublisherPopulated) {
      setTdbPublisher(new TdbPublisherWsSource(tdbTitle.getTdbPublisher()));
      tdbPublisherPopulated = true;
    }

    return super.getTdbPublisher();
  }

  @Override
  public String getId() {
    if (!idPopulated) {
      setId(tdbTitle.getId());
      idPopulated = true;
    }

    return super.getId();
  }

  /**
   * @deprecated Replaced by {@link #getProprietaryIds()}
   */
  @Override
  @Deprecated public String getProprietaryId() {
    setUpProprietaryIds();

    return super.getProprietaryId();
  }

  @Override
  public List<String> getProprietaryIds() {
    setUpProprietaryIds();

    return super.getProprietaryIds();
  }

  private void setUpProprietaryIds() {
    if (!proprietaryIdsPopulated) {
      List<String> ids = Arrays.asList(tdbTitle.getProprietaryIds());
      setProprietaryIds(ids);

      if (ids.size() > 0) {
	setProprietaryId(ids.get(0));
      }

      proprietaryIdsPopulated = true;
    }
  }

  @Override
  public String getPublicationType() {
    if (!publicationTypePopulated) {
      setPublicationType(tdbTitle.getPublicationType());
      publicationTypePopulated = true;
    }

    return super.getPublicationType();
  }

  @Override
  public String getIssn() {
    if (!issnPopulated) {
      setIssn(tdbTitle.getIssn());
      issnPopulated = true;
    }

    return super.getIssn();
  }

  @Override
  public String getIssnL() {
    if (!issnLPopulated) {
      setIssnL(tdbTitle.getIssnL());
      issnLPopulated = true;
    }

    return super.getIssnL();
  }

  @Override
  public String getEIssn() {
    if (!eIssnPopulated) {
      setEIssn(tdbTitle.getEissn());
      eIssnPopulated = true;
    }

    return super.getEIssn();
  }

  @Override
  public String getPrintIssn() {
    if (!printIssnPopulated) {
      setPrintIssn(tdbTitle.getPrintIssn());
      printIssnPopulated = true;
    }

    return super.getPrintIssn();
  }

  @Override
  public List<String> getIssns() {
    if (!issnsPopulated) {
      setIssns(new ArrayList<String>(Arrays.asList(tdbTitle.getIssns())));
      issnsPopulated = true;
    }

    return super.getIssns();
  }
}
