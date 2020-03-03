/*

Copyright (c) 2000-2020 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.dspace;

import java.io.IOException;
import java.util.*;
import org.dspace.xoai.model.oaipmh.Header;
import org.dspace.xoai.serviceprovider.exceptions.BadArgumentException;
import org.dspace.xoai.serviceprovider.parameters.ListIdentifiersParameters;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.*;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.Logger;

public class IdentifierListOaiPmhCrawlSeed extends BaseOaiPmhCrawlSeed {
  private static final Logger log = 
      Logger.getLogger(IdentifierListOaiPmhCrawlSeed.class);
  
  protected Collection<String> permUrls = new ArrayList<String>();
  
  public IdentifierListOaiPmhCrawlSeed (CrawlerFacade cf) {
    super(cf);
  }
  
  /**
   * Builds a param set for the OAI query
   * @param from
   * @param until
   * @param set
   * @param metadataPrefix
   * @return ListIdentifiersParameters 
   */
  protected ListIdentifiersParameters buildParams() {
    ListIdentifiersParameters mip = ListIdentifiersParameters.request();
    mip.withMetadataPrefix(metadataPrefix);
    if(usesDateRange){
      mip.withFrom(from);
      mip.withUntil(until);
    }
    if(usesSet && set!=NULL_SET) {
      mip.withSetSpec(set);
    }
    return mip;
  }
  /**
   * Iterates through OAI response returning a Collection of the IDs
   * @param params
   * @return
   * @throws ConfigurationException
   */
  protected Collection<String> getIdentifiersList(
      ListIdentifiersParameters params) throws ConfigurationException {
    try {
      Collection<String> idList = new ArrayList<String>();
      for(Iterator<Header> idIter = getServiceProvider().listIdentifiers(params);
          idIter.hasNext(); ) {
        Header h = idIter.next();
        idList.add(h.getIdentifier());
      }
      if (log.isDebug3()) {
        log.debug3("Identifiers: " + idList.toString());
      }
      return idList;
    } catch (BadArgumentException e) {
      throw new ConfigurationException(
          "Incorrectly formatted OAI parameter", e);
    }
  }
  
  /**
   * Fetches OAI response and iterates through converting article IDs to 
   * atricle URLs and returning the list.
   * @throws ConfigurationException 
   */
  @Override
  public Collection<String> doGetStartUrls() 
      throws ConfigurationException, PluginException, IOException {
    return idsToUrls(getIdentifiersList(
        buildParams()));
  }
  
  /**
  * Override this to provide different logic to convert OAI PMH ids 
  * to corresponding article urls
  * @param id
  * @param url
  * @return
  */
  public Collection<String> idsToUrls(Collection<String> ids) {
    Collection<String> urlList = new ArrayList<String>();
    for(String id : ids){
      if(id.contains(":") && !id.endsWith(":")) {
        String id_num = id.substring(id.lastIndexOf(':') + 1);
        if(permUrls.isEmpty()) {
          permUrls.add(baseUrl + oaiUrlPostfix + "?verb=GetRecord&identifier=" + 
              id + "&metadataPrefix=" + metadataPrefix);
        }
        urlList.add(baseUrl + "xmlui/handle/" + id_num);
      }
    }
    if (log.isDebug3()) {
      log.debug3("Start URLs: " + urlList.toString());
    }
    return urlList;
  }
}
