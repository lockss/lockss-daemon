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

public class CuChecker extends Worker {
  private static final Logger log = Logger.getLogger(CuChecker.class);
  private final CachedUrl cu;
  private final String namespace;

  public CuChecker(V2AuMover auMover, MigrationTask task) {
    super(auMover, task);
    this.cu = task.cu;
    namespace = auMover.getNamespace();
  }

  public void run() {
    if (isAbort()) {
      return;
    }
    log.debug2("Starting CuChecker: " + au + ", " + cu);
    String v2Url = null;
    try {
      CachedUrl[] v1Versions = cu.getCuVersions();
      v2Url = auMover.getV2Url(au, cu);
      List<Artifact> v2Versions;
      v2Versions = getAllCuArtifacts(au, v2Url);
      if (v2Versions.size() != v1Versions.length) {
        String msg = "Mismatched version count for: " + cu.getUrl() + 
          ": V1: " + v1Versions.length +
          ", V2: " + v2Versions.size();
        if (!v2Url.equals(cu.getUrl())) {
          msg += " v2Url: " + v2Url;
        }
        addError(msg);
        log.error(msg);
        // stop processing cu
        terminated = true;
      }
      if (!terminated) {
        log.debug2(v2Url + ":Checking Artifact...");
        for (int ver = 0; ver < v1Versions.length; ver++) {
          if (isAbort()) {
            break;
          }
          CachedUrl v1Version = v1Versions[ver];
          Artifact v2Artifact = v2Versions.get(ver);
          compareCuToArtifact(v1Version, v2Artifact);
        }
        ctrs.incr(CounterType.URLS_VERIFIED);
      }
    }
    catch (Exception ex) {
      String err = "Error verifying: " + v2Url + "in: " + au.getName() +
        ": " + ex;
      log.error(err, ex);
      task.addError(err);
      terminated = true;
    }
    finally {
      AuUtil.safeRelease(cu);
    }
  }

  private String mdMismatchMsg(ArchivalUnit au, CachedUrl cu, String field,
                               String v1, String v2) {
    return "Metadata mismatch in " + au.getName() + " for " + field +
      ": V1: " + v1 + ", V2: " + v2;
  }

  List<String> compareMetadata(ArchivalUnit au, CachedUrl cu,
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
      String msg = mdMismatchMsg(au, cu, "AUID",
                                 au.getAuId(), artifact.getAuid());
      log.warning(msg);
      res.add(msg);
    }
    if (!artifact.getNamespace().equals(namespace)) {
      String msg = mdMismatchMsg(au, cu, "Namespace",
                                 namespace, artifact.getNamespace());
      log.warning(msg);
      res.add(msg);
    }
    if (!artifact.getCollectionDate().equals(collDate)) {
      String msg = mdMismatchMsg(au, cu, "Collection date",
                                 Long.toString(collDate),
                                 Long.toString(artifact.getCollectionDate()));
      log.warning(msg);
      res.add(msg);
    }
    if (!artifact.getCommitted()) {
      String msg = mdMismatchMsg(au, cu, "Committed", "true", "false");
      log.warning(msg);
      res.add(msg);
    }
    return res;
  }

  void compareCuToArtifact(CachedUrl cu, Artifact artifact) {
    Long collectionDate = null;
    ArtifactData artifactData=null;
    try {
      String fetchTime =
          cu.getProperties().getProperty(CachedUrl.PROPERTY_FETCH_TIME);
      if (!StringUtil.isNullString(fetchTime)) {
        collectionDate = Long.parseLong(fetchTime);
      }
      List<String> mderrs = compareMetadata(au, cu, artifact, collectionDate);
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
        log.debug3("Fetching  content for byte compare");
        artifactData = artifactsApi.getMultipartArtifact(artifact.getUuid(),namespace,"ALWAYS");
        log.debug3("Successfully fetched Artifact Data");
        isMatch = IOUtils.contentEquals(artifactData.getInputStream(),
            cu.getUncompressedInputStream());
        if (!isMatch) {
          String err = "Artifact content mistmatch between V1 and V2: " +
            cu.getUrl() + " in: " + au;
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
            task.addError(cu.getUrl() + "V1 header '" + v2Key + "' missing from V2.");
          } else if (!StringUtil.equalStrings(v2Hdr.getValue(), v1Val)) {
            task.addError(cu.getUrl() + "V1 header '" + v1Key
                          + "' value mismatch, V1: " + v1Val
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
