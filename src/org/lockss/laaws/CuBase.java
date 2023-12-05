/*

Copyright (c) 2021-2022 Board of Trustees of Leland Stanford Jr. University,
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

import com.google.gson.Gson;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.http.*;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.lockss.laaws.client.ApiException;
import org.lockss.laaws.model.rs.Artifact;
import org.lockss.laaws.model.rs.ArtifactPageInfo;
import org.lockss.laaws.model.rs.ArtifactProperties;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.repository.*;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;
import java.util.*;

import static org.lockss.laaws.Counters.CounterType;

public class CuBase extends Worker {
  private static final Logger log = Logger.getLogger(CuBase.class);

  protected CachedUrl cu;
  protected String namespace;
  protected String v1Url;
  protected boolean isPartialContent;
  protected ListValuedMap<String,CachedUrl> mappedCus =
    new ArrayListValuedHashMap<>();

  protected static StatusLine STATUS_LINE_OK =
    new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK");


  protected CuBase(V2AuMover auMover, MigrationTask task) {
    super(auMover, task);
    this.cu = task.getCu();
    v1Url = cu.getUrl();
    namespace = auMover.getNamespace();
  }

  // This causes an InputStream to be opened on each CU, which 1) may
  // take some time, and 2) consumes a number of File Descriptors
  // equal to the number of versions.  The time to open the files
  // shouldn't be much of an issue as this is running concurrently
  // with several other CU copies.  If the FD consumption is a
  // problem, that would require a not-simple refactoring, or might be
  // more easily dealt with by limiting the number of versions that
  // are copied (with a config param?)
  void buildCompatMap(CachedUrl cu) {
    CachedUrl[] v1Versions = cu.getCuVersions();
    for (CachedUrl cuVer : v1Versions) {
      String v1Url = cuVer.getUrl();
      CIProperties verProps = cu.getProperties();
      String nodeUrl = verProps.getProperty(CachedUrl.PROPERTY_NODE_URL);
      String redirTo = verProps.getProperty(CachedUrl.PROPERTY_REDIRECTED_TO);
      if (UrlUtil.isDirectoryRedirection(v1Url, nodeUrl)) {
        // This was collected as "foo/", not the result of a redirect.
        // Copy it only as "foo/"
        V2CompatCachedUrl v2cuVer = new V2CompatCachedUrl(cuVer, nodeUrl);
        mappedCus.put(nodeUrl, v2cuVer);
      } else if (UrlUtil.isDirectoryRedirection(v1Url, redirTo)) {
        // This was redirected from "foo" to "foo/".  Copy as both
        // "foo" and "foo/" to match what V2 would have collected
        V2CompatCachedUrl v2cuVer = new V2CompatCachedUrl(cuVer, redirTo);
        mappedCus.put(v1Url, cuVer);
        mappedCus.put(redirTo, v2cuVer);
      } else {
        // No slash - V2 name is the same
        mappedCus.put(v1Url, cuVer);
      }
    }
  }

  protected String cuVersionString(CachedUrl cu) {
    StringBuilder sb = new StringBuilder();
    sb.append(" (ver: ");
    if (cu.getVersion() == 0) {
      sb.append("unknown");
    } else {
      sb.append(cu.getVersion());
    }
    sb.append(")");
    return sb.toString();
  }

  protected List<Artifact> getV2ArtifactsForUrl(String auId,  String v2Url)
      throws ApiException {
    ArtifactPageInfo pageInfo;
    String token = null;
    List<Artifact> cuArtifacts = new ArrayList<>();
    // if the v2 repo knows about this au we need to call getArtifacts.
    if (auMover.existsInV2(auId)) {
      isPartialContent = true;
      log.debug2("Checking for unmoved content: " + v2Url);
      do {
        pageInfo = artifactsApi.getArtifacts(auId, namespace,
            v2Url, null, "all", false, null, token);
        cuArtifacts.addAll(pageInfo.getArtifacts());
        token = pageInfo.getPageInfo().getContinuationToken();
      } while (!isAbort() && !StringUtil.isNullString(token));
      log.debug2("Found " + cuArtifacts.size() + " matches for " + v2Url);
    }
    return cuArtifacts;
  }

}
