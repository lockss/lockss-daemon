/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.associationforcomputingmachinery;

import java.io.Reader;

import org.lockss.filter.StringFilter;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.repository.LockssRepository;


public class AssociationForComputingMachineryCachedUrl extends org.lockss.plugin.base.BaseCachedUrl
{
  CachedUrl cu;
  /**
   * @param owner
   * @param url
   */
  public AssociationForComputingMachineryCachedUrl(ArchivalUnit owner,
      String url) {
    super(owner, url);
    this.cu = owner.makeCachedUrl(url);     
  }

  @Override
  public boolean hasContent() {
   return this.cu.hasContent();
  }
  /*
   * Filters out ampersands from the InputStream to avoid bad XML
   */
  @Override
  public Reader openForReading() {
      try {
              String[][] filterRules = {{"&#", "$%$amp;#"}, {"&", ""}, {"$%$amp;#", "&amp;#"}};
              return StringFilter.makeNestedFilter(cu.openForReading(), filterRules, true);
      } catch (Exception e) {
        //logger.error("Creating InputStreamReader for '" + getUrl() + "'", e);
        throw new LockssRepository.RepositoryStateException("Couldn't create InputStreamReader:" + e.toString());
      }
  }
}
