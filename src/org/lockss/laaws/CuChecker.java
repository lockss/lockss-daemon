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

import static org.lockss.laaws.Counters.CounterType;

import java.io.IOException;
import java.util.*;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.lockss.laaws.client.ApiException;
import org.lockss.laaws.model.rs.Artifact;
import org.lockss.laaws.model.rs.ArtifactData;
import org.lockss.laaws.model.rs.ArtifactPageInfo;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class CuChecker extends CuBase {
  private static final Logger log = Logger.getLogger(CuChecker.class);

  public CuChecker(V2AuMover auMover, MigrationTask task) {
    super(auMover, task);
  }

  public void run() {
    if (isAbort()) {
      return;
    }
    try {
      log.debug2("Starting CuChecker: " + au + ", " + cu);
      buildCompatMap(cu);
      if (mappedCus.isEmpty()) {
        log.debug2("No active versions of " + cu + " found.");
      }
      for (String v2Url : mappedCus.keySet()) {
        try {
          checkCuVersions(v2Url, mappedCus.get(v2Url),
                          getV2ArtifactsForUrl(auid, v2Url));
        } catch (ApiException e) {
          log.warning("Can't get list of V2 artifacts for " +
                      v2Url + ", continuing.");
          String err = "Error fetching V2 artifacts for " + v2Url +
            " in: " + au.getName();
          log.warning(err, e);
          task.addError(err + ": " + e.toString());
        }
      }
      ctrs.add(CounterType.URLS_VERIFIED, mappedCus.keySet().size());
    } finally {
      for (CachedUrl cu : mappedCus.values()) {
        AuUtil.safeRelease(cu);
      }
    }
  }

  /**
   * Check all V2 artifacts for a cachedUrl.
   *
   * @param v2Url       The uri for the current cached url.
   * @param cachedUrl   The cachedUrl we which to move
   * @param v2Artifacts The list of artifacts which already match this cachedUrl uri.
   */
  void checkCuVersions(String v2Url, List<CachedUrl> v1Versions,
                       Map<Integer,Artifact> v2Artifacts) {
    log.debug3("checkCuVersions("+v2Url+")");
    List<Integer> missingVers = new ArrayList();
    Set<Integer> remainingV2 = new TreeSet(v2Artifacts.keySet());
    int v2Count = v2Artifacts.size();
    for (CachedUrl v1Ver : v1Versions) {
      if (isAbort()) {
        break;
      }
      int ver = v1Ver.getVersion();
      try {
        Artifact v2Art = v2Artifacts.get(ver);
        if (v2Art != null) {
          remainingV2.remove(v2Art.getVersion());
          compareCuToArtifact(v1Ver, v2Art, ver);
        } else {
          missingVers.add(ver);
        }
      } catch (Exception e) {
        String err = "Error verifying: " + v2Url + " version " + ver +
          " in: " + au.getName() +
          ": " + e;
        log.error(err, e);
        task.addError(err);
      } finally {
        AuUtil.safeRelease(v1Ver);
      }
      ctrs.incr(CounterType.ARTIFACTS_VERIFIED);
    }
    if (!missingVers.isEmpty()) {
      String err = "Error: Target is missing " +
        StringUtil.numberOfUnits(missingVers.size(), "version") + ": " +
        StringUtil.separatedString(missingVers, ", ") + " of " + v2Url +
        " in: " + au.getName();
      log.error(err);
      task.addError(err);
    }
    if (!remainingV2.isEmpty()) {
      String err = "Error: Target has versions that source doesn't have: " +
        StringUtil.separatedString(remainingV2, ", ") + " of " + v2Url +
        " in: " + au.getName();
      log.error(err);
      task.addError(err);
    }
  }

  void compareCuToArtifact(CachedUrl cu, Artifact artifact, int ver) {
    Long collectionDate = null;
    ArtifactData artifactData=null;
    try {
      String fetchTime =
          cu.getProperties().getProperty(CachedUrl.PROPERTY_FETCH_TIME);
      if (!StringUtil.isNullString(fetchTime)) {
        collectionDate = Long.parseLong(fetchTime);
      }
      List<String> mderrs = compareMetadata(au, cu, ver,
                                            artifact, collectionDate);
      boolean isMatch = mderrs.isEmpty();
      if (!isMatch) {
        for (String msg : mderrs) {
          task.addError(msg);
        }
      }
      else {
        log.debug3(urlVer(cu) + ": metadata matches.");
      }
      if (mderrs.isEmpty() && auMover.isCompareBytes()) {
        log.debug3("Fetching content for byte compare: " + urlVer(cu));
        artifactData = artifactsApi.getMultipartArtifact(artifact.getUuid(),namespace,"ALWAYS");
        isMatch = IOUtils.contentEquals(artifactData.getInputStream(),
                                        cu.getUncompressedInputStream());
        if (!isMatch) {
          String err = "Artifact content mistmatch between V1 and V2: " +
            cu.getUrl() + " Ver: " + ver + ": " + " in: " + au;
          log.warning(err);
          task.addError(err);
        }
        else {
          log.debug3("V1 and V2 artifact content match.");
        }
        // Ensure each V1 header has a V2 counterpart
        HttpResponse v2RespHdr = artifactData.getResponseHeader();
        if (log.isDebug3()) log.debug3("ad.getResponseHeader(): " + v2RespHdr);
        Properties cuProps = cu.getProperties();
        for (Map.Entry ent : cuProps.entrySet()) {
          String v1Key = (String)ent.getKey();
          String v2Key = CuMover.v2CuPropKey(v1Key);
          String v1Val = (String)ent.getValue();
          Header v2Hdr = v2RespHdr.getFirstHeader(v2Key);
          if (log.isDebug3()) {
            log.debug3("header: " + v1Key + ": v1: " + v1Val + ", v2: " +
                       (v2Hdr == null ? "missing" : v2Hdr.getValue()));
          }
          if (v2Hdr == null) {
            task.addError(cu.getUrl() + " V1 header " + v2Key + " = " + v1Val +
                          " missing from V2.");
          } else if (!StringUtil.equalStrings(v2Hdr.getValue(), v1Val)) {
            task.addError(cu.getUrl() + " V1 header " + v1Key
                          + " value mismatch, V1: " + v1Val
                          + ", V2: " + v2Hdr.getValue());
          }
        }
        ctrs.incr(CounterType.ARTIFACTS_VERIFIED);
        // stats to update when available
        ctrs.add(CounterType.CONTENT_BYTES_VERIFIED, artifactData.getContentLength());
        ctrs.add(CounterType.BYTES_VERIFIED, artifactData.getSize()); // http response total bytes
        if (log.isDebug3()) {
          log.debug3("vcbytes + " + artifactData.getContentLength() +
                     ", vbytes + " + artifactData.getSize());
        }
      }
    }
    catch (ApiException | IOException ex) {
      String err = "Error checking cu: " + cu.getUrl() + " in " + au + ": " + ex.getMessage();
      log.error(err, ex);
      task.addError(err);
      // change to failed check counter
      terminated = true;
    } catch (Exception ex) {
      log.error("Unexpected exception", ex);
      throw ex;
    }
    finally {
      AuUtil.safeRelease(cu);
      if(artifactData !=null) {
        artifactData.release();
      }
    }
  }

  List<String> compareMetadata(ArchivalUnit au, CachedUrl cu, int ver,
                               Artifact artifact, Long v1CollectionDate) {
    List<String> res = new ArrayList<>();

    long collDate = v1CollectionDate != null ? v1CollectionDate : -1;
    if (artifact.getAuid().equals(au.getAuId()) &&
        artifact.getNamespace().equals(namespace)  &&
        (collDate == -1 || artifact.getCollectionDate().equals(collDate)) &&
        artifact.getCommitted().equals(Boolean.TRUE)) {
      return res;
    }
    if (!artifact.getAuid().equals(au.getAuId())) {
      String msg = mdMismatchMsg(au, cu, ver, "AUID",
                                 au.getAuId(), artifact.getAuid());
      log.warning(msg);
      res.add(msg);
    }
    if (!artifact.getNamespace().equals(namespace)) {
      String msg = mdMismatchMsg(au, cu, ver, "Namespace",
                                 namespace, artifact.getNamespace());
      log.warning(msg);
      res.add(msg);
    }
    if (!artifact.getCollectionDate().equals(collDate)) {
      String msg = mdMismatchMsg(au, cu, ver, "Collection date",
                                 Long.toString(collDate),
                                 Long.toString(artifact.getCollectionDate()));
      log.warning(msg);
      res.add(msg);
    }
    if (!artifact.getCommitted()) {
      String msg = mdMismatchMsg(au, cu, ver, "Committed", "true", "false");
      log.warning(msg);
      res.add(msg);
    }
    return res;
  }

  private String mdMismatchMsg(ArchivalUnit au, CachedUrl cu, int ver,
                               String field, String v1, String v2) {
    return "Metadata mismatch: " + au.getName() + ", url: " + cu.getUrl() + ", ver: " + ver + ", " + field  +
      ": V1: " + v1 + ", V2: " + v2;
  }

  /**
   * Retrieve all artifact versions for a URL in an AU
   * @param au
   * @param url
   * @return Map of version -> artifact
   */
  Map<Integer,Artifact>  getArtifactVersions(ArchivalUnit au, String url) {
    Map<Integer,Artifact> artMap = new HashMap<>();
    String token = null;
    do {
      try {
        ArtifactPageInfo pageInfo =
          artifactsApi.getArtifacts(au.getAuId(), namespace, url,
                                    null, "all", false, null, token);
        for (Artifact art : pageInfo.getArtifacts()) {
          artMap.put(art.getVersion(), art);
        }
        token = pageInfo.getPageInfo().getContinuationToken();
      } catch (ApiException e) {
        String msg = e.getCode() == 0
          ? e.getMessage() : e.getCode() + " - " + e.getMessage();
        String err = "Error retrieving from target artifacts for: " + url +
          " in " + au + ": " + msg;
        task.addError(err);
        log.error(err, e);
        terminated = true;
      }
    } while (!terminated && !StringUtil.isNullString(token));
    return artMap;
  }

}
