package org.lockss.laaws;

import static org.lockss.laaws.Counters.CounterType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
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
  private final String collection;

  public CuChecker(V2AuMover auMover, MigrationTask task) {
    super(auMover, task);
    this.cu = task.cu;
    collection = auMover.getCollection();
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
        String msg = "Mismatched version count: V1: " + v1Versions.length +
          ", V2: " + v2Versions.size() + ", " + cu.getUrl();
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
      String err = v2Url + " comparison failed: " + ex.getMessage();
      log.error(err, ex);
      task.addError(err);
      terminated = true;
    }
    finally {
      AuUtil.safeRelease(cu);
    }
  }

  void compareCuToArtifact(CachedUrl cu, Artifact artifact) {
    Long collectionDate = null;
    ArtifactData artifactData=null;
    MultipartFileResponse mfr=null;
    try {
      String fetchTime =
          cu.getProperties().getProperty(CachedUrl.PROPERTY_FETCH_TIME);
      if (!StringUtil.isNullString(fetchTime)) {
        collectionDate = Long.parseLong(fetchTime);
      }
      boolean isMatch =
          artifact.getAuid().equals(au.getAuId()) &&
              artifact.getCollection().equals(collection)  &&
              artifact.getCollectionDate().equals(collectionDate) &&
              artifact.getCommitted().equals(Boolean.TRUE);
      if (!isMatch) {
        log.warning(cu.getUrl() + "V1 and V2 metadata did not match");
      }
      else {
        log.debug3(cu.getUrl() + ": metadata matches.");
      }
      if ( isMatch && auMover.isCompareBytes()) {
        log.debug3("Fetching  content for byte compare");
        mfr = collectionsApi.getMultipartArtifact(collection,
            artifact.getId(),
            "ALWAYS");
        artifactData = new ArtifactData(mfr.getMimeMultipart());
        log.debug3("Successfully fetched Artifact Data");
        isMatch = IOUtils.contentEquals(artifactData.getInputStream(),
            cu.getUncompressedInputStream());
        if (!isMatch) {
          String err = cu.getUrl() + "V1 and V2 artifact content did not match.";
          log.warning(err);
          task.addError(err);
        }
        else {
          log.debug3("V1 and V2 artifact content were a match.");
        }
        ctrs.incr(CounterType.ARTIFACTS_VERIFIED);
        // stats to update when available
        ctrs.add(CounterType.CONTENT_BYTES_VERIFIED, artifactData.getContentLength());
        ctrs.add(CounterType.BYTES_VERIFIED, mfr.getSize()); // http response total bytes
      }
    }
    catch (ApiException | IOException ex) {
      String err = cu.getUrl() + "Error checking cu: " + ex.getMessage();
      log.error(err, ex);
      task.addError(err);
      // change to failed check counter
      terminated = true;
    }
    finally {
      AuUtil.safeRelease(cu);
      if(artifactData !=null) {
        artifactData.release();
      }
      if(mfr != null) {
        mfr.delete();
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
        pageInfo = collectionsApi.getArtifacts(collection, au.getAuId(),
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
