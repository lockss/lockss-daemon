/*
 * $Id$
 */

/*

Copyright (c) 2021 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.rs;

import java.io.File;
import java.io.InputStream;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CuIterator;
import org.lockss.repository.RepoSpec;
import org.lockss.rs.api.StreamingCollectionsApi;
import org.lockss.rs.client.ApiClient;
import org.lockss.rs.client.ApiException;
import org.lockss.rs.model.Artifact;
import org.lockss.util.Logger;
import org.lockss.util.UrlUtil;

public class V2RepoAuCopier {

  private static final Logger log = Logger.getLogger(V2RepoAuCopier.class);
  public static final String PREFIX = Configuration.PREFIX + "v2repo.";
  public static final String PARAM_V2_REPO_SPEC =
      PREFIX + "repo_spec";
  public static final String PARAM_V2_REPO_USER =
      PREFIX + "user";
  public static final String PARAM_V2_REPO_PASSWD =
      PREFIX + "passwd";

  public static final String PARAM_V2_USER_AGENT =
      PREFIX + "user_agent";

  public static final String  DEFAULT_V2_USER_AGENT = "lockss";

  /**
   * The v2 REST collections api implemntation
   */
  private StreamingCollectionsApi collectionsApi;

  /**
   * The v2 Repository Client
   */
  private ApiClient repoClient;

  /**
   * The v2 Repository Spec
   */
  private RepoSpec repoSpec;

  /**
   * The v2 Collection
   */
  private String collection;

  /**
   * The target v2 Service url
   */
  private String serviceUrl;

  private String userAgent=DEFAULT_V2_USER_AGENT;

  public V2RepoAuCopier() {
    Configuration config = ConfigManager.getCurrentConfig();
    //TODO: There are currently no defaults for any of these.
    //FIXME: username password should not be read from a config file.
    String rspec= config.get(PARAM_V2_REPO_SPEC);
    String ruser=config.get(PARAM_V2_REPO_USER);
    String rpass=config.get(PARAM_V2_REPO_PASSWD);
    userAgent=config.get(PARAM_V2_USER_AGENT, DEFAULT_V2_USER_AGENT);
    initClient(rspec,ruser, rpass);
  }


  /**
   * The primary constructor for a V2RepoAuCopier
   * @param rspec The v2 RepoSpec string
   * @param ruser The v2 login user
   * @param rpass The v2 login password
   */
  public V2RepoAuCopier(String rspec, String ruser, String rpass) {
    initClient(rspec, ruser, rpass);
 }

  /**
   * Move one au as identified by the name of the au
   * @param auId The ArchivalUnit Id string
   */
  public void moveAu(String auId) {
    moveAu(LockssDaemon.getLockssDaemon().getPluginManager().getAuFromId(auId));
  }

  /**
   * Move one AU
   * @param au The ArchivalUnit to move
   */
  public void moveAu(ArchivalUnit au) {
    /* get Au items from Lockss*/
    for (CuIterator iter = au.getAuCachedUrlSet().getCuIterator(); iter.hasNext(); ) {
      CachedUrl cachedUrl = iter.next();
      moveCuVersions(cachedUrl);
    }
  }

  /**
   * Initialization for Rest client
   * @param rspec The v2 RepoSpec string
   * @param ruser The v2 login user
   * @param rpass The v2 login password
   */
  protected void initClient(String rspec, String ruser, String rpass) {
    try {
      this.repoSpec=RepoSpec.fromSpec(rspec);
      this.collection=repoSpec.getCollection();
      this.serviceUrl=repoSpec.getUrl();
      if(UrlUtil.isMalformedUrl(serviceUrl)) {
        throw new IllegalArgumentException("RepoSpec contained malformed url"+serviceUrl);
      }
    }
    catch(IllegalArgumentException iae) {
      log.error(iae.getMessage());
    }

    repoClient = new ApiClient();
    repoClient.setUsername(ruser);
    repoClient.setPassword(rpass);
    repoClient.setUserAgent(userAgent);
    repoClient.setBasePath(serviceUrl);
    collectionsApi = new StreamingCollectionsApi(repoClient);
  }

  private void moveCuVersions(CachedUrl cachedUrl) {
    String auid = cachedUrl.getArchivalUnit().getAuId();
    CachedUrl[] cu_vers = cachedUrl.getCuVersions();
    for (CachedUrl cu : cu_vers) {
      String uri = cu.getUrl();
      Long collectionDate = Long
          .parseLong(cu.getProperties().getProperty(CachedUrl.PROPERTY_FETCH_TIME));
      InputStream instr = cu.getUnfilteredInputStream();
      try {
        Artifact response = moveArtifactStream(auid, uri, collectionDate, instr, collection);
        //TODO: what needs to be done with the response?
        // Is there data that needs to be moved on an artifact basis?
      } catch (ApiException ae) {
        log.error("Attempt to move uri " + uri + " in au " + auid + "failed:" + ae.getCode());
        //TODO: need much more complete error handling here.
      }
    }
    log.debug3("Completed move of all versions of " + cachedUrl.getUrl());
  }


  private Artifact moveArtifactStream(String auid, String uri, Long collectionDate,
      InputStream inStream, String collectionId) throws ApiException{
    return collectionsApi.createArtifact(auid, uri, collectionDate, inStream, collectionId);
  }

  private Artifact moveArtifactFile(String auid, String fileName, String collection, String uri,
      String date) throws ApiException {
    File artifact = new File(fileName);
    String collectionId = collection == null ? "lockss" : collection;
    Long collectionDate = Long.parseLong(date);
    Artifact response = collectionsApi
        .createArtifact(auid, uri, collectionDate, artifact, collectionId);
    return response;
  }
}
