package org.lockss.laaws;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.input.CountingInputStream;
import org.lockss.laaws.api.rs.StreamingCollectionsApi;
import org.lockss.laaws.client.ApiException;
import org.lockss.laaws.model.rs.Artifact;
import org.lockss.laaws.model.rs.ArtifactPageInfo;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import static org.lockss.laaws.V2AuMover.CounterType;

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
    log.debug2("Starting CuChecker: " + au + ", " + cu);
    CachedUrl[] v1Versions = cu.getCuVersions();
    String v2Url = auMover.getV2Url(au, cu);
    List<Artifact> v2Versions;
    try {
      v2Versions = getAllCuArtifacts(au, v2Url);
      if (!terminated) {
        int versionCompare = v2Versions.size() - v1Versions.length;
        String cmpString = versionCompare > 0 ? "more" : "fewer";
        if (versionCompare != 0) {
          String err = "Mismatched version count for " + v2Url
              + ": V2 Repo has " + cmpString + " versions than V1 Repo";
          addError(err);
          terminated = true;
        }
        else {
          log.info("Checking Artifact metadata...");
          for (int ver = 0; ver < v1Versions.length; ver++) {
            CachedUrl v1Version = v1Versions[ver];
            Artifact v2Artifact = v2Versions.get(ver);
            compareCuToArtifact(v1Version, v2Artifact);
          }
          ctrs.incr(CounterType.URLS_VERIFIED);
        }
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
    String fetchTime = cu.getProperties().getProperty(CachedUrl.PROPERTY_FETCH_TIME);
    Long collectionDate = null;
    if (!StringUtil.isNullString(fetchTime)) {
      collectionDate = Long.parseLong(fetchTime);
    }
    boolean isMatch =
        artifact.getAuid().equals(au.getAuId()) &&
        artifact.getCollection().equals(collection)  &&
        artifact.getCollectionDate().equals(collectionDate) &&
        artifact.getCommitted().equals(Boolean.TRUE);
    if ( isMatch && auMover.isCompareBytes()) {
      log.debug3("Fetching  content for byte compare");
      try {
        File v2Artifact = collectionsApi.getArtifact(collection, artifact.getId(),
            "ALWAYS");
        log.debug("File: " + v2Artifact.getName()
            + "Can Read:"+v2Artifact.canRead()
            +" Can Write:" + v2Artifact.canWrite());
        FileInputStream fis = new FileInputStream(v2Artifact);
        CountingInputStream cis = new CountingInputStream(fis);


        ctrs.incr(CounterType.ARTIFACTS_VERIFIED);

        // stats to update when available
        ctrs.add(CounterType.CONTENT_BYTES_VERIFIED, cis.getByteCount());
        ctrs.add(CounterType.BYTES_VERIFIED, 0); // http response total bytes
      }
      catch (ApiException | IOException e) {
        e.printStackTrace();
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
