/*

  Copyright (c) 2021-2025 Board of Trustees of Leland Stanford Jr. University,
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

public class CuMover extends CuBase {
  private static final Logger log = Logger.getLogger(CuMover.class);

  private String v2Url;

  protected static StatusLine STATUS_LINE_OK =
    new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "OK");


  public CuMover(V2AuMover auMover, MigrationTask task) {
    super(auMover, task);
  }

  public void run() {
    if (isAbort()) {
      return;
    }
    try {
      log.debug2("Starting CuMover: " + au + ", " + cu);
      buildCompatMap(cu);
      if (mappedCus.isEmpty()) {
        log.warning("No active versions of " + cu + " found.");
      }
      for (String v2Url : mappedCus.keySet()) {
        // Skip fetching the existing V2 artifects if we know no
        // artifacts of this AU were present on target when operation
        // started
        Map<Integer,Artifact> v2Artifacts = Collections.emptyMap();
        if (auMover.existsInV2(au.getAuId())) {
          try {
            v2Artifacts = getV2ArtifactsForUrl(auid, v2Url);
          } catch (ApiException e) {
            // XXX FATAL?
            log.warning("Can't get list of existing V2 artifacts for " +
                        v2Url + ", continuing.");
          }
        }
        moveCuVersions(v2Url, mappedCus.get(v2Url), v2Artifacts);
      }

      ctrs.add(CounterType.URLS_MOVED, mappedCus.keySet().size());
    } finally {
      for (CachedUrl cu : mappedCus.values()) {
        AuUtil.safeRelease(cu);
      }
    }
  }
  
  /**
   * Move all versions of one v2 URL for a cachedUrl.
   *
   * @param v2Url       The uri for the current cached url.
   * @param v1Versions  V1 CUs
   * @param v2Artifacts The list of artifacts which already match this cachedUrl uri.
   */
  void moveCuVersions(String v2Url, List<CachedUrl> v1Versions,
                      Map<Integer,Artifact> v2Artifacts) {
    log.debug3("moveCuVersions("+v2Url+")");
    // Will be sorted in ascending version order (which isn't really
    // necessary now that we're explicitly setting version numbers)
    Deque<CachedUrl> cuQueue = new ArrayDeque<>();
    // Queue all CU versions that don't already exist on target
    for (CachedUrl v1Ver : v1Versions) {
      Artifact v2Ver = v2Artifacts.get(v1Ver.getVersion());
      if (v2Ver == null) {
        cuQueue.addFirst(v1Ver);
      } else {
        AuUtil.safeRelease(v1Ver);
      }
    }
    int onTarget = v1Versions.size() - cuQueue.size();
    log.debug2("Queueing " + cuQueue.size() + " versions, " +
                 onTarget + " already present on target");
    if (log.isDebug3()) {
      log.debug3("Copy queue: " + cuQueue);
    }
    while(!isAbort() && cuQueue.peek() != null) {
      moveNextCuVersion(auid, v2Url, cuQueue);
    }
  }

  /**
   * Queue a request to create an artifact for the next version of a CachedUrl
   * @param auid The au we are moving
   * @param v2Url The v2 url
   * @param cuQueue The queue of cached url versions.
   */
  void moveNextCuVersion(String auid, String v2Url, Queue<CachedUrl> cuQueue) {
    Long collectionDate = null;
    CachedUrl cu = cuQueue.poll();
    if (cu == null) {
      if (log.isDebug3()) {
        log.debug3("All versions of " + v2Url + " have been queued.");
      }
      return;
    }
    try {
      String fetchTime = cu.getProperties().getProperty(CachedUrl.PROPERTY_FETCH_TIME);
      if (!StringUtil.isNullString(fetchTime)) {
        collectionDate = Long.parseLong(fetchTime);
      }
      else {
        log.debug2(v2Url + ": version " + cu.getVersion() + " is missing fetch time.");
      }
      if (log.isDebug3()) {
        log.debug3("Moving cu version " + cu.getVersion() + " - fetched at " + fetchTime);
      }
      copyArtifact(auid, v2Url, collectionDate, cu, namespace);
      log.debug3("copyArtifact returned");
    }
    catch (ApiException apie) {
      String err = "Failed to write " + v2Url + cuVersionString(cu) + ": " +
          apie.getCode() + " - " + apie.getMessage();
      log.warning(err);
      task.addError(err);
    }
    catch (LockssRepository.RepositoryStateException e) {
      String err = "V1 repository error reading " + v1Url + cuVersionString(cu);
      log.warning(err, e);
      task.addError(err + ": " + e);
    }
    catch (Exception | Error e) {
      String err = "Unexpected Error copying " + v1Url + cuVersionString(cu);
      log.warning(err, e);
      task.addError(err + ": " + e);
    }
    finally {
      AuUtil.safeRelease(cu);
    }
  }

  /**
   * Make a synchronous rest call to the V2 repository to create a new artifact.
   *
   * @param auid           au identifier for the CachedUrl we are moving.
   * @param v2Url          the uri  for the CachedUrl we are moving.
   * @param collectionDate the date at which this item was collected or null
   * @param cu             the CachedUrl we are moving.
   * @param namespace   the v2 namespace we are moving to
   * @throws ApiException the rest exception thrown should anything fail in the request.
   */
  void copyArtifact(String auid, String v2Url, Long collectionDate,
      CachedUrl cu, String namespace) throws ApiException {
    log.debug3("createArtifact("+v2Url+")");
    DigestCachedUrl dcu = new DigestCachedUrl(cu);
    Gson gson = new Gson();
    try {
      ctrs.addInProgressDcu(CounterType.CONTENT_BYTES_MOVED, dcu);
      ctrs.addInProgressDcu(CounterType.BYTES_MOVED, dcu);

      // The artifact properties
      Map<String, String> props = new HashMap<>();
      props.put(ArtifactProperties.SERIALIZED_NAME_NAMESPACE,namespace);
      props.put(ArtifactProperties.SERIALIZED_NAME_AUID, auid);
      props.put(ArtifactProperties.SERIALIZED_NAME_URI, v2Url);
      props.put(ArtifactProperties.SERIALIZED_NAME_VERSION,
                Integer.toString(cu.getVersion()));
      props.put(ArtifactProperties.SERIALIZED_NAME_COLLECTION_DATE, String.valueOf(collectionDate));
      String prop_str = gson.toJson(props);

      // Build the httpResponseHeader part, consisting of HTTP status
      // line and response headers
      BasicHttpResponse response = new BasicHttpResponse(STATUS_LINE_OK);
      CIProperties hdr_props = cu.getProperties();
      if (hdr_props != null) {
        ((Set<String>) ((Map) hdr_props).keySet()).forEach(
          key -> response.addHeader(CuMover.v2CuPropKey(key),
                                    hdr_props.getProperty(key)));
      }

      Artifact uncommitted =
        artifactsApiLongCall.createArtifact(prop_str, dcu,
                                            respHeadersToString(response));
      if (uncommitted != null) {
        if (log.isDebug3()) {
          log.debug3("createArtifact returned,  content bytes: " +
                     cu.getContentSize() + ", total: " + dcu.getContentBytesRead());
        }
        commitArtifact(uncommitted, dcu);
      }
    } finally {
      // Ensure it's removed in case didn't happen in commitArtifact()
       ctrs.removeInProgressDcu(CounterType.CONTENT_BYTES_MOVED, dcu);
       ctrs.removeInProgressDcu(CounterType.BYTES_MOVED, dcu);
    }
  }

  static final String FETCH_TIME_V2_LOWER =
    CachedUrl.PROPERTY_FETCH_TIME_V2.toLowerCase();

  static String v2CuPropKey(String key) {
    if (key.equalsIgnoreCase(CachedUrl.PROPERTY_FETCH_TIME)) {
      return FETCH_TIME_V2_LOWER;
    }
    return key;
  }

  private String respHeadersToString(HttpResponse response) {
    StringBuilder sb = new StringBuilder();
    sb.append(response.getStatusLine());
    sb.append("\r\n");
    for (Header hdr : response.getAllHeaders()) {
      sb.append(hdr.getName());
      sb.append(": ");
      sb.append(hdr.getValue());
      sb.append("\r\n");
    }
    return sb.toString();
  }

  /**
   * Make a synchronous rest call to commit the artifact that just completed successful creation.
   *
   * @param uncommitted the v2 artifact to be committed
   * @param dcu the DigestCachedUrl with the commutted digest added to CachedUrl
   * @throws ApiException the rest exception thrown should anything fail in the request.
   */
  void commitArtifact(Artifact uncommitted, DigestCachedUrl dcu) throws ApiException {
    Artifact committed;
    log.debug3("committing artifact " + uncommitted.getUuid());
    committed = artifactsApi.updateArtifact(uncommitted.getUuid(),true, uncommitted.getNamespace());
    String contentDigest = dcu.getContentDigest();
    if (!committed.getContentDigest().equals(contentDigest)) {
      String err="Error in commit of " + dcu.getCu().getUrl() + " content digest do not match";
      log.error(err);
      log.debug("v1 digest: " +dcu.getContentDigest()+  " v2 digest: " + committed.getContentDigest());
      task.addError(err);
    }
    else {
      if (log.isDebug2()) {
        log.debug2("Hash match: " + dcu.getCu().getUrl() + ": v1 digest: " +dcu.getContentDigest()+  " v2 digest: " + committed.getContentDigest());
      }
      log.debug3("Successfully committed artifact " + committed.getUuid());
      ctrs.incr(CounterType.ARTIFACTS_MOVED);
      ctrs.removeInProgressDcu(CounterType.CONTENT_BYTES_MOVED, dcu);
      ctrs.removeInProgressDcu(CounterType.BYTES_MOVED, dcu);
      ctrs.add(CounterType.CONTENT_BYTES_MOVED, dcu.getContentBytesMoved());
      ctrs.add(CounterType.BYTES_MOVED, dcu.getTotalBytesMoved());
    }
  }
}
