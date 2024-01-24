/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.michigan.deepblue;

import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.serviceprovider.exceptions.BadArgumentException;
import org.dspace.xoai.serviceprovider.exceptions.InvalidOAIResponse;
import org.dspace.xoai.serviceprovider.model.Context;
import org.dspace.xoai.serviceprovider.model.Context.KnownTransformer;
import org.dspace.xoai.serviceprovider.parameters.ListRecordsParameters;
import org.dspace.xoai.services.api.MetadataSearch;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.UrlCacher;
import org.lockss.plugin.UrlData;
import org.lockss.util.CIProperties;
import org.lockss.util.Constants;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeepBlueOaiCrawlSeed extends RecordFilteringOaiPmhCrawlSeed {
  public static final String DEFAULT_DATE_TAG = "dc.date.issued.en_US";
  public static final String DEFAULT_IDENTIFIER_TAG = "dc.identifier.uri.none";
  protected Collection<String> startUrls;
  protected int year;
  protected Pattern yearPattern = Pattern.compile("^([0-9]{4})$");
  public static final String OAI_DC_METADATA_PREFIX = "oai_dc";
  public static final String XOAI_METADATA_PREFIX = "xoai";
  private static Logger logger =
	      Logger.getLogger(DeepBlueOaiCrawlSeed.class);

  /*
  Sample record:
  https://deepblue.lib.umich.edu/dspace-oai/request?verb=ListRecords&set=col_2027.42_41251&metadataPrefix=xoai

    <metadata>
       <element name="dc">
          <element name="contributor">
             <element name="author">
                <element name="en_US">
                   <field name="value">Ehlers, G. M.</field>
                   <field name="value">Stumm, E. C.</field>
                </element>
             </element>
             <element name="affiliationum">
                <element name="en_US">
                   <field name="value">Museum of Paleontology, The University of Michigan, 1109 Geddes Road, Ann Arbor, MI 48109-1079, U.S.A.</field>
                </element>
             </element>
             <element name="affiliationumcampus">
                <element name="en_US">
                   <field name="value">Ann Arbor</field>
                </element>
             </element>
          </element>
          <element name="date">
             <element name="accessioned">
                <element name="none">
                   <field name="value">2006-09-12T19:59:17Z</field>
                </element>
             </element>
             <element name="available">
                <element name="none">
                   <field name="value">2006-09-12T19:59:17Z</field>
                </element>
             </element>
             <element name="issued">
                <element name="en_US">
                   <field name="value">1949</field>
                </element>
             </element>
          </element>
          <element name="identifier">
             <element name="citation">
                <element name="en_US">
                   <field name="value">Vol. 7, No. 8 &lt;http://hdl.handle.net/2027.42/48241&gt;</field>
                </element>
             </element>
             <element name="other">
                <element name="en_US">
                   <field name="value">0868909 (Grad, Sci, Museums)</field>
                </element>
             </element>
             <element name="uri">
                <element name="none">
                   <field name="value">https://hdl.handle.net/2027.42/48241</field>
                </element>
             </element>
          </element>
          <element name="description">
             <element name="en_US">
                <field name="value">123-130</field>
             </element>
             <element name="bitstreamurl">
                <element name="en_US">
                   <field name="value">http://deepblue.lib.umich.edu/bitstream/2027.42/48241/2/ID080.pdf</field>
                </element>
             </element>
          </element>
          <element name="format">
             <element name="extent">
                <element name="none">
                   <field name="value">2946 bytes</field>
                   <field name="value">1386240 bytes</field>
                </element>
             </element>
             <element name="mimetype">
                <element name="none">
                   <field name="value">text/plain</field>
                   <field name="value">application/pdf</field>
                </element>
             </element>
          </element>
          <element name="language">
             <element name="iso">
                <element name="none">
                   <field name="value">en_US</field>
                </element>
             </element>
          </element>
          <element name="publisher">
             <element name="en_US">
                <field name="value">Museum of Paleontology, The University of Michigan</field>
             </element>
          </element>
          <element name="relation">
             <element name="ispartofseries">
                <element name="en_US">
                   <field name="value">Contributions</field>
                </element>
             </element>
          </element>
          <element name="title">
             <element name="en_US">
                <field name="value">Corals of the Devonian Traverse Group of Michigan. Part I. Spongophyllum</field>
             </element>
          </element>
          <element name="type">
             <element name="en_US">
                <field name="value">Article</field>
             </element>
          </element>
          <element name="subject">
             <element name="hlbsecondlevel">
                <element name="en_US">
                   <field name="value">Anthropology and Archaeology</field>
                   <field name="value">Geology and Earth Sciences</field>
                </element>
             </element>
             <element name="hlbtoplevel">
                <element name="en_US">
                   <field name="value">Science</field>
                   <field name="value">Social Sciences</field>
                </element>
             </element>
          </element>
       </element>
       <element name="bundles">
          <element name="bundle">
             <field name="name">TEXT</field>
             <element name="bitstreams">
                <element name="bitstream">
                   <field name="name">ID080.pdf.txt</field>
                   <field name="originalName">ID080.pdf.txt</field>
                   <field name="description">Extracted text</field>
                   <field name="format">text/plain</field>
                   <field name="size">18206</field>
                   <field name="url">https://deepblue.lib.umich.edu/bitstream/2027.42/48241/3/ID080.pdf.txt</field>
                   <field name="checksum">c81aa7a3299898580bbd3f29602f7ced</field>
                   <field name="checksumAlgorithm">MD5</field>
                   <field name="sid">3</field>
                </element>
             </element>
          </element>
          <element name="bundle">
             <field name="name">LICENSE</field>
             <element name="bitstreams">
                <element name="bitstream">
                   <field name="name">license.txt</field>
                   <field name="format">text/plain</field>
                   <field name="size">2946</field>
                   <field name="url">https://deepblue.lib.umich.edu/bitstream/2027.42/48241/1/license.txt</field>
                   <field name="checksum">d565140e788494ecc5a995fbd106a511</field>
                   <field name="checksumAlgorithm">MD5</field>
                   <field name="sid">1</field>
                </element>
             </element>
          </element>
          <element name="bundle">
             <field name="name">ORIGINAL</field>
             <element name="bitstreams">
                <element name="bitstream">
                   <field name="name">ID080.pdf</field>
                   <field name="format">application/pdf</field>
                   <field name="size">1386240</field>
                   <field name="url">https://deepblue.lib.umich.edu/bitstream/2027.42/48241/2/ID080.pdf</field>
                   <field name="checksum">4284f5b22721d152128619fe50b3b954</field>
                   <field name="checksumAlgorithm">MD5</field>
                   <field name="sid">2</field>
                </element>
             </element>
          </element>
       </element>
       <element name="others">
          <field name="handle">2027.42/48241</field>
          <field name="identifier">oai:deepblue.lib.umich.edu:2027.42/48241</field>
          <field name="lastModifyDate">2021-07-28 19:45:11.886</field>
       </element>
       <element name="repository">
          <field name="name">Deep Blue</field>
          <field name="mail">deepblue@umich.edu</field>
       </element>
       <element name="license">
          <field name="bin">VGhpcyBBZ3JlZW1lbnQgaXMgbWFkZSBieSBhbmQgYmV0d2VlbiB0aGUgTXVzZXVtIG9mIFBhbGVvbnRvbG9neSBvZiB0aGUgVW5pdmVyc2l0eSBvZiBNaWNoaWdhbiAKKGhlcmVpbmFmdGVyIGNhbGxlZCB0aGUgQ29tbXVuaXR5KSwgcmVwcmVzZW50ZWQgYnkgQ2FuZGFjZSBMLiBTdGF1Y2gsIHRoZSBDb21tdW5pdHkncyBDb2xsZWN0aW9uIENvb3JkaW5hdG9yIAooaGVyZWluYWZ0ZXIgY2FsbGVkIHRoZSBEZXNpZ25lZSkgYW5kIHRoZSBVbml2ZXJzaXR5IG9mIE1pY2hpZ2FuIExpYnJhcnkncyBEZWVwIEJsdWUgSW5zdGl0dXRpb25hbCBSZXBvc2l0b3J5IAooaGVyZWluYWZ0ZXIgY2FsbGVkIHRoZSBSZXBvc2l0b3J5KSBhbmQgaXMgZW50ZXJlZCBpbnRvIG9uIHRoaXMgNXRoIGRheSBvZiBTZXB0ZW1iZXIsIDIwMDYuCgpUaGlzIGFncmVlbWVudCBhbGxvd3MgdGhlIERlc2lnbmVlIHRvIGFjdCBvbiBiZWhhbGYgb2YgaW5kaXZpZHVhbCBkZXBvc2l0b3JzIGFuZC9vciB0aGUgQ29tbXVuaXR5IGFuZCBhdXRob3JpemVzIAp0aGUgQ29vcmRpbmF0b3IgdG8gYmF0Y2ggbG9hZCBjb250ZW50IHRvIHRoZSBSZXBvc2l0b3J5LgoKT24gYmVoYWxmIG9mIHRoZSBDb21tdW5pdHksIHRoZSBEZXNpZ25lZSBhY2tub3dsZWRnZXMgaGF2aW5nIHJlYWQgYWxsIHBvbGljaWVzIG9mIHRoZSBSZXBvc2l0b3J5IGFuZCBhZ3JlZXMgdG8gCmFiaWRlIGJ5IHRoZSB0ZXJtcyBhbmQgY29uZGl0aW9ucyB0aGVyZWluLiBGb3IgY29udGVudCBlbnRydXN0ZWQgb3IgZ2lmdGVkIHRvIHRoZSBEZXNpZ25lZSwgdGhlIERlc2lnbmVlIGFncmVlcyAKdGhhdCBtYWtpbmcgdGhlIGNvbnRlbnQgYXZhaWxhYmxlIGluIHRoZSBSZXBvc2l0b3J5IGlzIGFuIGFwcHJvcHJpYXRlIGFuZCBhY2NlcHRhYmxlIGV4dGVuc2lvbiBvZiBpdHMgYWdyZWVtZW50IAp3aXRoIHRoZSBDb21tdW5pdHksIGFuZCBpcyBjb25zaXN0ZW50IHdpdGggdGhlIERlc2lnbmVlJ3MgY2hhcmdlIHRvIHByb3ZpZGUgYWNjZXNzIHRvIHRoaXMgY29udGVudC4gSW4gYWRkaXRpb24sIAp0aGUgQ29vcmRpbmF0b3IgaGFzIGNvbnZleWVkIHRvIGFwcHJvcHJpYXRlIENvbW11bml0eSByZXByZXNlbnRhdGl2ZXMgdGhlIHRlcm1zIGFuZCBjb25kaXRpb25zIG91dGxpbmVkIGluIHRob3NlIApwb2xpY2llcywgaW5jbHVkaW5nIHRoZSBsYW5ndWFnZSBvZiB0aGUgc3RhbmRhcmQgZGVwb3NpdCBsaWNlbnNlIHF1b3RlZCBiZWxvdyBhbmQgdGhhdCB0aGUgQ29tbXVuaXR5IG1lbWJlcnMgaGF2ZSAKZ3JhbnRlZCB0aGUgQ29vcmRpbmF0b3IgdGhlIGF1dGhvcml0eSB0byBkZXBvc2l0IGNvbnRlbnQgb24gdGhlaXIgYmVoYWxmLgoKVGhlIHN0YW5kYXJkIGRlcG9zaXQgbGljZW5zZSBzdGF0ZXM6CgpJIGhlcmVieSBncmFudCB0byB0aGUgUmVnZW50cyBvZiB0aGUgVW5pdmVyc2l0eSBvZiBNaWNoaWdhbiB0aGUgbm9uLWV4Y2x1c2l2ZSByaWdodCB0byByZXRhaW4sIHJlcHJvZHVjZSBhbmQgCmRpc3RyaWJ1dGUgdGhlIGRlcG9zaXRlZCB3b3JrICh0aGUgV29yaykgaW4gd2hvbGUgb3IgaW4gcGFydCwgaW4gYW5kIGZyb20gaXRzIGVsZWN0cm9uaWMgZm9ybWF0LiBUaGlzIGFncmVlbWVudCAKZG9lcyBub3QgcmVwcmVzZW50IGEgdHJhbnNmZXIgb2YgY29weXJpZ2h0IHRvIHRoZSBVbml2ZXJzaXR5IG9mIE1pY2hpZ2FuLgoKVGhlIFVuaXZlcnNpdHkgb2YgTWljaGlnYW4gbWF5IG1ha2UgYW5kIGtlZXAgbW9yZSB0aGFuIG9uZSBjb3B5IG9mIHRoZSBXb3JrIGZvciBwdXJwb3NlcyBvZiBzZWN1cml0eSwgYmFja3VwLCAKcHJlc2VydmF0aW9uIGFuZCBhY2Nlc3MsIGFuZCBtYXkgbWlncmF0ZSB0aGUgV29yayB0byBhbnkgbWVkaXVtIG9yIGZvcm1hdCBmb3IgdGhlIHB1cnBvc2Ugb2YgcHJlc2VydmF0aW9uIGFuZCAKYWNjZXNzIGluIHRoZSBmdXR1cmUuIFRoZSBVbml2ZXJzaXR5IG9mIE1pY2hpZ2FuIHdpbGwgbm90IG1ha2UgYW55IGFsdGVyYXRpb24sIG90aGVyIHRoYW4gYXMgYWxsb3dlZCBieSB0aGlzIAphZ3JlZW1lbnQsIHRvIHRoZSBXb3JrLgoKSSByZXByZXNlbnQgYW5kIHdhcnJhbnQgdG8gdGhlIFVuaXZlcnNpdHkgb2YgTWljaGlnYW4gdGhhdCB0aGUgV29yayBpcyBteSBvcmlnaW5hbCB3b3JrLiBJIGFsc28gcmVwcmVzZW50IHRoYXQgCnRoZSBXb3JrIGRvZXMgbm90LCB0byB0aGUgYmVzdCBvZiBteSBrbm93bGVkZ2UsIGluZnJpbmdlIG9yIHZpb2xhdGUgYW55IHJpZ2h0cyBvZiBvdGhlcnMuCgpJIGZ1cnRoZXIgcmVwcmVzZW50IGFuZCB3YXJyYW50IHRoYXQgSSBoYXZlIG9idGFpbmVkIGFsbCBuZWNlc3NhcnkgcmlnaHRzIHRvIHBlcm1pdCB0aGUgVW5pdmVyc2l0eSBvZiBNaWNoaWdhbiAKdG8gcmVwcm9kdWNlIGFuZCBkaXN0cmlidXRlIHRoZSBXb3JrIGFuZCB0aGF0IGFueSB0aGlyZC1wYXJ0eSBvd25lZCBjb250ZW50IGlzIGNsZWFybHkgaWRlbnRpZmllZCBhbmQgYWNrbm93bGVkZ2VkIAp3aXRoaW4gdGhlIFdvcmsuCgpCeSBncmFudGluZyB0aGlzIGxpY2Vuc2UsIEkgYWNrbm93bGVkZ2UgdGhhdCBJIGhhdmUgcmVhZCBhbmQgYWdyZWVkIHRvIHRoZSB0ZXJtcyBvZiB0aGlzIGFncmVlbWVudCBhbmQgYWxsIHJlbGF0ZWQgClVuaXZlcnNpdHkgb2YgTWljaGlnYW4gYW5kIERlZXAgQmx1ZSBwb2xpY2llcy4KCkRlc2lnbmVlClNpZ25hdHVyZTogW0NhbmRhY2UgTC4gU3RhdWNoXQpOYW1lOiBDYW5kYWNlIEwuIFN0YXVjaApUaXRsZTogTXVzZXVtIEJ1c2luZXNzIEFkbWluaXN0cmF0b3IsIE11c2V1bSBQYWxlb250b2xvZ3kKRGF0ZTogNiBTZXAgMjAwNgpFbWFpbDogY3N0YXVjaEB1bWljaC5lZHUKClJlcG9zaXRvcnkKU2lnbmF0dXJlOiBbSmltIE90dGF2aWFuaV0KTmFtZTogSmltIE90dGF2aWFuaQpUaXRsZTogQ29vcmRpbmF0b3IsIERlZXAgQmx1ZQpEYXRlOiA1IFNlcCAyMDA2CkVtYWlsOiBqaW0ub3R0YXZpYW5pQHVtaWNoLmVkdQoK</field>
       </element>
    </metadata>
   */

  public DeepBlueOaiCrawlSeed(CrawlerFacade cf) {
    super(cf);
    setMetadataPrefix(XOAI_METADATA_PREFIX);
    setUrlPostfix("dspace-oai/request");
  }

  @Override
  protected Context buildContext(String url) {
    Context con = super.buildContext(url);
    //"xoai" is the default Transformer, so no need to "overwrite" it
    //con.withMetadataTransformer(OAI_DC_METADATA_PREFIX, KnownTransformer.OAI_DC);
    //con.withMetadataTransformer(XOAI_METADATA_PREFIX, KnownTransformer.OAI_DC);
    return con;
  }
  
  @Override
  protected void initialize() 
      throws PluginException, ConfigurationException, IOException {
    super.initialize();
    if(UrlUtil.isHttpUrl(baseUrl)) {
      baseUrl = UrlUtil.replaceScheme(baseUrl, "http", "https");
    }
    
  }

  /*
   * Here's an example of an OAI request of DeepBlue:
   * https://deepblue.lib.umich.edu/dspace-oai/request?verb=ListRecords&set=col_2027.42_41251&metadataPrefix=xoai
   */
  
  @Override
  protected Collection<String> getRecordList(ListRecordsParameters params)
		  throws ConfigurationException, IOException {

      String url = UrlUtil.encodeUrl(au.getAuId());

      String storeUrl = baseUrl + "auid=" + UrlUtil.encodeUrl(au.getAuId());

      logger.debug3("baseUrl = " + baseUrl + ", url = " + url + ", storeUrl = " + storeUrl);

      String link;
      Boolean error = false;
      Set<String> idSet = new HashSet<String>();
      try {

	      for (Iterator<Record> recIter = getServiceProvider().listRecords(params);
	           recIter.hasNext();) {

            logger.debug3("Inside for....");

	        Record rec = recIter.next();
            if (rec == null) {
                logger.debug3("Rec is null");
            }

	        MetadataSearch<String> metaSearch = 
	            rec.getMetadata().getValue().searcher();

	        if (checkMetaRules(metaSearch)) {
                logger.debug3(" -  checkMetaRules passed");
	        	link = findRecordArticleLink(rec);
                if (link != null) {
                    logger.debug3(" - link = " + link);
                    /*
                    replace what's in identifier with url from base_url
                    <dc:identifier>https://hdl.handle.net/2027.42/48172</dc:identifier>
                    https://deepblue.lib.umich.edu/handle/2027.42/48172
                     */

                    if (link.contains("http://hdl.handle.net/") || 	link.contains("https://hdl.handle.net/")) {
                        String replaced_link = link.replace("http://hdl.handle.net/",baseUrl + "handle/").replace("https://hdl.handle.net/",baseUrl + "handle/");
                        logger.debug3(" - link = " + link + ", replaced_link = " + replaced_link);
                        idSet.add(replaced_link);
                    }
                } else {
                    logger.debug3(" - empty link");
                }
	        } else {
                logger.debug3(" - checkMetaRules failed");
            }
	      }
      } catch (InvalidOAIResponse e) {

          logger.debug3("Inside cache invalid block....Rec is null");

    	  if(e.getCause() != null && e.getCause().getMessage().contains("LOCKSS")) {
    		  error = true;
    		  logger.debug("OAI result errored due to LOCKSS audit proxy. Trying alternate start Url", e);
    	  } else {
    		  throw e;
    	  }
      } catch (BadArgumentException e) {

          logger.debug3("Inside BadArgumentException....Rec is null");

    	  throw new ConfigurationException("Incorrectly formatted OAI parameter", e);
      }
      
      List<String> idList = new ArrayList<String>();
	  if(error) {
		  idList.add(storeUrl);
	  } else if(!idSet.isEmpty()) {
		  idList.addAll(idSet);
		  Collections.sort(idList);
		  storeStartUrls(idList, storeUrl);
	  }
	  return idList;
  }
  
  protected void storeStartUrls(Collection<String> urlList, String url) throws IOException {
	  StringBuilder sb = new StringBuilder();
	  sb.append("<html>\n");
	  for (String u : urlList) {
          logger.debug3("storeStartUrl = " + u);
		  sb.append("<a href=\"" + u + "\">" + u + "</a><br/>\n");
	  }
	  sb.append("</html>");
	  CIProperties headers = new CIProperties();
	  //Should use a constant here
	  headers.setProperty("content-type", "text/html; charset=utf-8");
      UrlData ud = new UrlData(new ByteArrayInputStream(sb.toString().getBytes(Constants.ENCODING_UTF_8)), headers, url);
      UrlCacher cacher = facade.makeUrlCacher(ud);
      cacher.storeContent();
  }
  
  protected String findRecordArticleLink(Record rec) { 
    MetadataSearch<String> recSearcher = rec.getMetadata().getValue().searcher();
    List<String> idTags = recSearcher.findAll(DEFAULT_IDENTIFIER_TAG);
    if(idTags != null && !idTags.isEmpty()) {
      for(String value : idTags) {
          logger.debug("To Follow value: " + value);
        if (value.startsWith("http") || value.startsWith("https")) {
          logger.debug("To Follow: " + value);
          return value;
        }
      }
    } else {
        logger.debug("findRecordArticleLink find Null");
    }
    return null;
  }
  
  
  @Override
  protected void parseRules(String rule) throws ConfigurationException {
    if(rule.length() == 4) {
      try {
        year = Integer.parseInt(rule);
      } catch(NumberFormatException ex) {
        throw new ConfigurationException("OAI date must be a 4 digit year");
      }
    } else {
      throw new ConfigurationException("OAI date must be a 4 digit year");
    }
    
  }

  @Override
  protected boolean checkMetaRules(MetadataSearch<String> metaSearch) {
      List<String> matchingTags;
      matchingTags = metaSearch.findAll(DEFAULT_DATE_TAG);

      logger.debug3("Inside checkMetaRules......default date tag is = " + DEFAULT_DATE_TAG);


      if(matchingTags!= null && !matchingTags.isEmpty()) {
          for(String value : matchingTags) {
              try{
                  String subYear;
                  Matcher yearMatch = yearPattern.matcher(value);
                  if(yearMatch.find()) {
                      subYear = yearMatch.group(1);
                      logger.debug3(" subYear = " + subYear + " value = " + value + ", expected year = " + year);
                      if(year == Integer.parseInt(subYear)) {
                          logger.debug3(" subYear = " + subYear + " value = " + value + " ======== expected year = " + year);
                          return true;
                      }
                      return true;
                  }
              } catch(NumberFormatException|IllegalStateException ex) {
                  logger.debug3(" yearPattern match does not expectation");
              }
          }
      } else if (matchingTags!= null) {
          logger.debug3(" matchingTags is not null, checkMetaRules metaSearch = " + metaSearch);
          for(String value : matchingTags) {
              logger.debug3(" checkMetaRules metaSearch value = " + value);
          }
      } else if (matchingTags == null) {
          logger.debug3(" matchingTags is NULL, checkMetaRules metaSearch = " + metaSearch);
      }
      return true;
  }
  
  /**
   * Override this to provide different logic to convert OAI PMH ids to
   * corresponding article urls
   * 
   * @param id
   * @param url
   * @return
   */
  public Collection<String> idsToUrls(Collection<String> ids) {
    return ids;
  }
  
  @Override
  public boolean isFailOnStartUrlError() {
    return false;
  }
  
}
