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
      for (String v2Url : mappedCus.keySet()) {
        try {
          List<Artifact> v2Arts = getV2ArtifactsForUrl(auid, v2Url);
          checkCuVersions(v2Url, mappedCus.get(v2Url), v2Arts);
        } catch (ApiException e) {
          log.warning("Can't get list of V2 artifacts for " +
                      v2Url + ", continuing.");
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
  void checkCuVersions(String v2Url, List<CachedUrl> localVersions,
                       List<Artifact> v2Artifacts) {
    log.debug3("checkCuVersions("+v2Url+")");
    int v2Count = v2Artifacts.size();
    try {
      int minCount = Math.min(localVersions.size(), v2Artifacts.size());
      if (v2Artifacts.size() != localVersions.size()) {
        String msg = "Mismatched version count for: " + v2Url +
          ": V1: " + localVersions.size() +
          ", V2: " + v2Artifacts.size() + ".";
        if (auMover.isCompareEvenIfVersionMismatch() && minCount > 0) {
          msg += "  Attempting to compare the most recent " + minCount +
            " versions.";
          addError(msg);
          log.error(msg);
        } else {
          // stop processing cu
          terminated = true;
        }
      }
      if (!terminated) {
        log.debug2(v2Url + ":Checking Artifact...");
        for (int ver = 0; ver < minCount; ver++) {
          if (isAbort()) {
            break;
          }
          CachedUrl v1Version = localVersions.get(ver);
          Artifact v2Artifact = v2Artifacts.get(ver);
          compareCuToArtifact(v1Version, v2Artifact, ver + 1);
        }
      }
    }
    catch (Exception ex) {
      String err = "Error verifying: " + v2Url + "in: " + au.getName() +
        ": " + ex;
      log.error(err, ex);
      task.addError(err);
      terminated = true;
    }
  }

  private String mdMismatchMsg(ArchivalUnit au, CachedUrl cu, int ver,
                               String field, String v1, String v2) {
    return "Metadata mismatch: " + au.getName() + ", url: " + cu.getUrl() + ", ver: " + ver + ", " + field  +
      ": V1: " + v1 + ", V2: " + v2;
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
        log.debug3(cu.getUrl() + ": metadata matches.");
      }
      if (mderrs.isEmpty() && auMover.isCompareBytes()) {
        log.debug3("Fetching content for byte compare");
        artifactData = artifactsApi.getMultipartArtifact(artifact.getUuid(),namespace,"ALWAYS");
        log.debug3("Successfully fetched Artifact Data");
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
        if (log.isDebug2()) log.debug2("ad.getResponseHeader(): " + v2RespHdr);
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

  /**
   * Retrieve all the artifacts for an AU
   * @param au The ArchivalUnit to retrieve
   * @return a list of Artifacts.
   */
  List<Artifact>  getAllCuArtifacts(ArchivalUnit au, String url) {
    List<Artifact> auArtifacts = new ArrayList<>();
    ArtifactPageInfo pageInfo;
    String token = null;
    do {
      try {
        pageInfo = artifactsApi.getArtifacts( au.getAuId(),namespace,
            url, null, "all", false, null, token);
        auArtifacts.addAll(pageInfo.getArtifacts());
        token = pageInfo.getPageInfo().getContinuationToken();
      }
      catch (ApiException apie) {
        String msg = apie.getCode() == 0 ? apie.getMessage()
            : apie.getCode() + " - " + apie.getMessage();
        String err = "Error occurred while retrieving artifacts for au: " + msg;
        task.addError(err);
        log.error(err, apie);
        terminated = true;
      }
    } while (!terminated && !StringUtil.isNullString(token));
    return auArtifacts;
  }

}
