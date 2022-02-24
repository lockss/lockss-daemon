package org.lockss.laaws;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import org.lockss.laaws.V2AuMover.DigestCachedUrl;
import org.lockss.laaws.api.rs.StreamingCollectionsApi;
import org.lockss.laaws.client.ApiException;
import org.lockss.laaws.model.rs.Artifact;
import org.lockss.laaws.model.rs.ArtifactPageInfo;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;

public class CuMover {

  private static final Logger log = Logger.getLogger(CuMover.class);
  private V2AuMover auMover;
  private ArchivalUnit au;
  private CachedUrl cu;
  private String v1Url;
  private String v2Url;
  private boolean isPartialContent;
  private boolean terminated = false;
  private String collection;
  private long cuArtifactsMoved = 0;
  private long cuBytesMoved = 0;
  private long cuContentBytesMoved = 0;

  /**
   * The v2 REST collections api implemntation
   */
  private final StreamingCollectionsApi collectionsApi;


  public CuMover(V2AuMover auMover, ArchivalUnit au, CachedUrl cu) {
    this.auMover = auMover;
    this.au = au;
    this.cu = cu;
    collection = auMover.getCollection();
    collectionsApi = auMover.getRepoCollectionsApiClient();
  }

  public void run() {
    log.debug2("Starting CuMover: " + au + ", " + cu);
    v1Url=cu.getUrl();
    v2Url=auMover.getV2Url(au, cu);
    List<Artifact> cuArtifacts = null;
    try {
      cuArtifacts = getV2ArtifactsForUrl(au.getAuId(), v2Url);
    }
    catch (ApiException e) {
      log.warning("Unable to determine which V2 Urls have already been moved, continuing.");
    }
    moveCuVersions(v2Url, cu, cuArtifacts);
  }
  
  /**
   * Move all versions of a cachedUrl.
   *
   * @param v2Url       The uri for the current cached url.
   * @param cachedUrl   The cachedUrl we which to move
   * @param v2Artifacts The list of artifacts which already match this cachedUrl uri.
   */
  void moveCuVersions(String v2Url, CachedUrl cachedUrl, List<Artifact> v2Artifacts) {
    if (!terminated) {
      String auid = cachedUrl.getArchivalUnit().getAuId();
      CachedUrl[] localVersions = cachedUrl.getCuVersions();
      Queue<CachedUrl> cuQueue = Collections.asLifoQueue(new ArrayDeque<>());
      int v2Count = v2Artifacts.size();
      //If we have more v1 versions than the v2 repo - copy the missing items
      if (v2Count > 0) {
        log.debug3("v2 versions available=" + v2Count + " v1 versions available="
            + localVersions.length);
      }
      // if the v2 repository has fewer versions than the v1 repository
      // then move the missing versions or release the cu version.
      int vers_to_move = localVersions.length - v2Count;
      log.debug3("Queueing " + vers_to_move + "/" + localVersions.length + " versions...");
      if (vers_to_move > 0) {
        for (int vx = 0; vx < localVersions.length; vx++) {
          CachedUrl ver = localVersions[vx];
          if (vx < vers_to_move) {
            cuQueue.add(ver);
          }
          else {
            AuUtil.safeRelease(ver);
          }
        }
        while(!terminated && cuQueue.peek() != null)
          moveNextCuVersion(auid, v2Url, cuQueue);
      }
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
        log.debug2(v2Url + ":version: " + cu.getVersion() + " is missing fetch time.");
      }
      if (log.isDebug3()) {
        log.debug3("Moving cu version " + cu.getVersion() + " - fetched at " + fetchTime);
      }
      createArtifact(auid, v2Url, collectionDate, cu, collection);
    }
    catch (ApiException apie) {
      String err = v2Url + ": failed to create version: " + cu.getVersion() + ": " +
          apie.getCode() + " - " + apie.getMessage();
      log.warning(err);
      auMover.addError(err);
    }
    finally {
      AuUtil.safeRelease(cu);
    }
  }

  /**
   * Make an asynchronous rest call to the V2 repository to create a new artifact.
   *
   * @param auid           au identifier for the CachedUrl we are moving.
   * @param v2Url          the uri  for the CachedUrl we are moving.
   * @param collectionDate the date at which this item was collected or null
   * @param cu             the CachedUrl we are moving.
   * @param collectionId   the v2 collection we are moving to
   * @throws ApiException the rest exception thrown should anything fail in the request.
   */
  void createArtifact(String auid, String v2Url, Long collectionDate,
      CachedUrl cu, String collectionId) throws ApiException {
    log.debug3("enqueing create artifact request...");
    DigestCachedUrl dcu = new DigestCachedUrl(cu);
    Artifact uncommitted = collectionsApi.createArtifact(collectionId, auid, v2Url, dcu, collectionDate);
    cuContentBytesMoved += cu.getContentSize();
    cuBytesMoved += dcu.getBytesMoved();
    commitArtifact(uncommitted, dcu);
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
    log.debug3("committing artifact " + uncommitted.getId());
    committed = collectionsApi.updateArtifact(uncommitted.getCollection(),
        uncommitted.getId(), true);
    String contentDigest = dcu.getContentDigest();
    if (!committed.getContentDigest().equals(contentDigest)) {
      String err="Error in commit of " + dcu.getCu().getUrl() + " content digest do not match";
      log.error(err);
      log.debug("v1 digest: " +dcu.getContentDigest()+  " v2 digest: " + committed.getContentDigest());
      auMover.addError(err);
    }
    else {
      if (log.isDebug2()) {
        log.debug2("Hash match: " + dcu.getCu().getUrl() + ": v1 digest: " +dcu.getContentDigest()+  " v2 digest: " + committed.getContentDigest());
      }
      log.debug3("Successfully committed artifact " + committed.getId());
      cuArtifactsMoved++;

    }

  }

  private List<Artifact> getV2ArtifactsForUrl(String auId,  String v2Url)
      throws ApiException {
    ArtifactPageInfo pageInfo;
    String token = null;
    List<Artifact> cuArtifacts = new ArrayList<>();
    // if the v2 repo knows about this au we need to call getArtifacts.
    if (auMover.getKnownV2Aus().contains(auId)) {
      isPartialContent = true;
      log.debug2("Checking for unmoved content: " + v2Url);
      do {
        pageInfo = collectionsApi.getArtifacts(collection, auId,
            v2Url, null, "all", false, null, token);
        cuArtifacts.addAll(pageInfo.getArtifacts());
        token = pageInfo.getPageInfo().getContinuationToken();
      } while (!terminated && !StringUtil.isNullString(token));
      log.debug2("Found " + cuArtifacts.size() + " matches for " + v2Url);
    }
    return cuArtifacts;
  }

}
