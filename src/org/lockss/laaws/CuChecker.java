package org.lockss.laaws;

import java.util.ArrayList;
import java.util.List;
import org.lockss.laaws.api.rs.StreamingCollectionsApi;
import org.lockss.laaws.client.ApiException;
import org.lockss.laaws.model.rs.Artifact;
import org.lockss.laaws.model.rs.ArtifactPageInfo;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class CuChecker {
  private static final Logger log = Logger.getLogger(CuMover.class);
  private final V2AuMover auMover;
  private final ArchivalUnit au;
  private final CachedUrl cu;
  private boolean terminated = false;
  private final String collection;
  private final boolean doByteCompare;

  /**
   * The v2 REST collections api implemntation
   */
  private final StreamingCollectionsApi collectionsApi;

  public CuChecker(V2AuMover auMover, ArchivalUnit au, CachedUrl cu, boolean compareBytes) {
    this.auMover = auMover;
    this.au = au;
    this.cu = cu;
    this.doByteCompare=compareBytes;
    collection = auMover.getCollection();
    collectionsApi = auMover.getRepoCollectionsApiClient();
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
          auMover.addError(err);
          terminated = true;
        }
        else {
          log.info("Checking Artifact metadata...");
          for (int ver = 0; ver < v1Versions.length; ver++) {
            CachedUrl v1Version = v1Versions[ver];
            Artifact v2Artifact = v2Versions.get(ver);
            compareCuToArtifact(v1Version, v2Artifact);
          }
        }
      }
    }
    catch (Exception ex) {
      String err = v2Url + " comparison failed: " + ex.getMessage();
      log.error(err, ex);
      auMover.addError(err);
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
    if ( isMatch && doByteCompare) {
      log.debug3("Fetching  content for byte compare");
//      ArtifactData artifact = collectionsApi.getArtifact(collection, artifact.getId(),
//                    "ALWAYS");

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
        auMover.addError(err);
        log.error(err, apie);
        terminated = true;
      }
    } while (!terminated && !StringUtil.isNullString(token));
    return auArtifacts;
  }

}
