/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.crawler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.util.Logger;
import org.lockss.util.TypedEntryMap;

import com.lyncode.xoai.model.oaipmh.Header;
import com.lyncode.xoai.serviceprovider.exceptions.BadArgumentException;
import com.lyncode.xoai.serviceprovider.parameters.ListIdentifiersParameters;

public class IdentifierListOaiPmhCrawlSeed 
    extends BaseOaiPmhCrawlSeed {
  private static Logger logger = 
      Logger.getLogger(IdentifierListOaiPmhCrawlSeed.class);
  public IdentifierListOaiPmhCrawlSeed (ArchivalUnit au) {
    super(au);
    if(au == null) {
      throw new IllegalArgumentException(
          "Valid ArchivalUnit required for crawl initializer");
    }
    TypedEntryMap props = au.getProperties();
    try {
      populateFromProps(props);
    } catch(ConfigurationException ex) {
      logger.error("Error creating crawl seed", ex);
    } catch(PluginException ex) {
      logger.error("Error creating crawl seed", ex);
    }
  }
  
  /**
   * Pulls needed params from the au props. Throws exceptions if
   *  expected props do not exist
   * @param props
   * @throws PluginException
   * @throws ConfigurationException
   */
  protected void populateFromProps(TypedEntryMap props) 
      throws PluginException, ConfigurationException {
    //required params
    if(props.containsKey(ConfigParamDescr.YEAR.getKey())) {
      setDates(props.getInt(ConfigParamDescr.YEAR.getKey()));
    } else if (props.containsKey(KEY_AU_OAI_FROM_DATE) &&
        props.containsKey(KEY_AU_OAI_UNTIL_DATE)) {
      setDates(props.getString(KEY_AU_OAI_FROM_DATE), 
          props.getString(KEY_AU_OAI_UNTIL_DATE));
    } else {
      throw new PluginException.InvalidDefinition("CrawlInitializer expected "
          + ConfigParamDescr.YEAR.getKey() + " or " + KEY_AU_OAI_FROM_DATE +
          " and " + KEY_AU_OAI_UNTIL_DATE);
    }
    if(props.containsKey(ConfigParamDescr.BASE_URL.getKey())) {
      this.baseUrl = props.getString(ConfigParamDescr.BASE_URL.getKey());
    } else {
      throw new PluginException.InvalidDefinition("CrawlInitializer expected "
          + ConfigParamDescr.BASE_URL.getKey());
    }
    
    //optional params
    if(props.containsKey(KEY_AU_OAI_SET)) {
      this.set = props.getString(KEY_AU_OAI_SET);
    }
    if(props.containsKey(KEY_AU_OAI_URL_POSTFIX)) {
      this.oaiUrlPostfix = props.getString(KEY_AU_OAI_URL_POSTFIX);
    }
  }
  
  /**
   * Builds a param set for the OAI query
   * @param from
   * @param until
   * @param set
   * @param metadataPrefix
   * @return ListIdentifiersParameters 
   */
  protected ListIdentifiersParameters buildParams(Date from, Date until, 
      String set, String metadataPrefix) {
    ListIdentifiersParameters mip = ListIdentifiersParameters.request();
    mip.withMetadataPrefix(metadataPrefix);
    mip.withFrom(from);
    mip.withUntil(until);
    if(set != null) {
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
      for(Iterator<Header> idIter = sp.listIdentifiers(params);
          idIter.hasNext(); ) {
        Header h = idIter.next();
        idList.add(h.getIdentifier());
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
  public Collection<String> getStartUrls() 
      throws ConfigurationException, PluginException {
    return idsToUrls(getIdentifiersList(
        buildParams(from, until, set, metadataPrefix)));
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
      urlList.add(baseUrl + "oai/request?verb=GetRecord&identifier=" + 
          id + "&metadataPrefix=" + metadataPrefix);
      if(id.contains(":") && !id.endsWith(":")) {
        String id_num = id.substring(id.lastIndexOf(':') + 1);
        urlList.add(baseUrl + "handle/" + id_num);
      }
    }
    return urlList;
  }
}
