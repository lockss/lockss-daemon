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
package org.lockss.laaws;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.lockss.app.LockssDaemon;
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

public class LockssRepositoryMover {

  private static final Logger log = Logger.getLogger(LockssRepositoryMover.class);
  private final StreamingCollectionsApi collectionsApi;
  private final ApiClient repoClient;
  /**
   * The v2 Repository Spec
   */
  private RepoSpec v2RepoSpec;
  /**
   * The v2 Collection
   */
  private String v2Collection;


  /**
   * The v2 Service URL
   */
  private URL v2ServiceUrl;

  public LockssRepositoryMover(RepoSpec repoSpec, String userName, String password) {
    v2RepoSpec = repoSpec;
    v2Collection = repoSpec.getCollection();
    if (UrlUtil.isUrl(repoSpec.getUrl())) {
      try {
        v2ServiceUrl = new URL(repoSpec.getUrl());
      } catch (MalformedURLException e) {
        e.printStackTrace();
      }
    }
    repoClient = new ApiClient();
    repoClient.setUsername(userName);
    repoClient.setPassword(password);
    repoClient.setUserAgent("lockss");
    repoClient.setBasePath(repoSpec.getUrl());
    collectionsApi = new StreamingCollectionsApi(repoClient);

  }

  public void moveAu(String auId) {
    moveAu(LockssDaemon.getLockssDaemon().getPluginManager().getAuFromId(auId));
  }

  public void moveAu(ArchivalUnit au) {
    /* get Au items from Lockss*/
    for (CuIterator iter = au.getAuCachedUrlSet().getCuIterator(); iter.hasNext(); ) {
      CachedUrl cachedUrl = iter.next();
      moveCuVersions(cachedUrl);
    }
  }

  private void moveCuVersions(CachedUrl cachedUrl) {
    String auid = cachedUrl.getArchivalUnit().getAuId();
    File artifact = null;
    String collectionid = null;
    CachedUrl[] cu_vers = cachedUrl.getCuVersions();
    for (CachedUrl cu : cu_vers) {
      String uri = cu.getUrl();
      Long collectionDate = Long
          .parseLong(cu.getProperties().getProperty(CachedUrl.PROPERTY_FETCH_TIME));
      InputStream instr = cu.getUnfilteredInputStream();
      try {
        Artifact response = moveArtifactStream(auid, uri, collectionDate, instr, collectionid);
      } catch (ApiException e) {
        e.printStackTrace();
      }
    }
  }


  private Artifact moveArtifactStream(String auid, String uri, Long collectionDate,
      InputStream inStream, String collectionId) throws ApiException{
    return collectionsApi.createArtifact(auid, uri, collectionDate, inStream, collectionId);
  }

  private Artifact moveArtifactFile(String auid, String fileName, String collection, String uri,
      String date) {
    File artifact = new File(fileName);
    String collectionId = collection == null ? "lockss" : collection;
    Long collectionDate = Long.parseLong(date);
    Artifact response = null;
    try {
      response = collectionsApi
          .createArtifact(auid, uri, collectionDate, artifact, collectionId);
    } catch (ApiException e) {
      e.printStackTrace();
    }
    return response;
  }
}
