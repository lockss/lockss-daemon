package org.lockss.plugin.catalog;

import static org.lockss.util.Constants.DAY;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.daemon.CachedUrlSetHasher;
import org.lockss.hasher.BlockHasher;
import org.lockss.hasher.HashBlock;
import org.lockss.hasher.HashService;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.base.BaseUrlCacher;
import org.lockss.poller.Poll;
import org.lockss.poller.PollManager;
import org.lockss.poller.PollSpec;
import org.lockss.util.ByteArray;
import org.lockss.util.CIProperties;
import org.lockss.util.Deadline;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

public class ScheduledChecksumVerifier {
  static final String PREFIX = Configuration.PREFIX + "localhash.";
  /** If true, empty poll state directories found at startup will be
   * deleted.
   */
  static final String PARAM_SCHEDULE_FACTOR = PREFIX + "scheduleFactor";
  static final int DEFAULT_SCHEDULE_FACTOR = 2;
  static final String PARAM_SCHEDULE_ADDENDUM = PREFIX + "scheduleAddendum";
  static final int DEFAULT_SCHEDULE_ADDENDUM = 0;
  
  protected static Logger logger = Logger.getLogger("ScheduledChecksumVerifier");

  public static boolean verifyAU(ArchivalUnit au) throws Exception {
    CachedUrlSet cus = au.getAuCachedUrlSet();
    int maxVersions = 1; //Only deal with the latest version for now
    
    LockssDaemon theDaemon = LockssDaemon.getLockssDaemon();
    if (theDaemon == null) {
      throw new IllegalStateException("no LOCKSS daemon");
    }
    MessageDigest[] hasherDigests = new MessageDigest[1];
    hasherDigests[0] = getChecksumProducer();
    if (hasherDigests[0] == null)
      throw new Exception("Can't schedule a hash of an AU without configuring the hashing algorithm");
    byte[][] initBytes = new byte[1][];
    initBytes[0] = new byte[0];
    
    VerifierEventHandler eh = new VerifierEventHandler(au);  
    BlockHasher hasher = new BlockHasher(cus, maxVersions, hasherDigests,
        initBytes, eh);
    // Now schedule the hash
    eh.setHasher(hasher);
    HashService hashService = theDaemon.getHashService();
    //
    long schedule_factor = CurrentConfig.getIntParam(PARAM_SCHEDULE_FACTOR, DEFAULT_SCHEDULE_FACTOR);
    long schedule_addendum = CurrentConfig.getIntParam(PARAM_SCHEDULE_ADDENDUM, DEFAULT_SCHEDULE_ADDENDUM);
    Deadline deadline = Deadline.in( cus.estimatedHashDuration() * schedule_factor + schedule_addendum) ;
    boolean res = false;
    try {
      res = hashService.scheduleHash(hasher, deadline, eh, null);
      if( res ) {
        logger.debug(String.format("Scheduled checksum verification of %s", au.getName()));
      } else {
        logger.error(String.format("Could not schedure checksum verification of %s", au));
      }
    } catch (Exception e) {
      logger.error(String.format("Failed to schedure checksum verification of %s", au), e);
    }
    return res;

  }
  
  private static MessageDigest getChecksumProducer() {
    MessageDigest checksumProducer = null;
    String checksumAlgorithm =
      CurrentConfig.getParam(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
          BaseUrlCacher.DEFAULT_CHECKSUM_ALGORITHM);
    if (!StringUtil.isNullString(checksumAlgorithm)) {
      try {
        checksumProducer = MessageDigest.getInstance(checksumAlgorithm);
        return checksumProducer;
      } catch (NoSuchAlgorithmException ex) {
        logger.error(String.format("Checksum algorithm %s not found, checksuming disabled", checksumAlgorithm));
      }
    }
    return null;
  }  
  
  private static class VerifierEventHandler implements BlockHasher.EventHandler, HashService.Callback {
    private ArchivalUnit au;
    private BlockHasher hasher;
    private LockssDaemon theDaemon;
    private boolean errors = false;
    private long totalBytes = 0;
    
    VerifierEventHandler(ArchivalUnit au) {
      this.au = au;
    }
    
    public void setHasher(BlockHasher hasher) {
      this.hasher = hasher;
    }
    
    @Override
    public void blockDone(HashBlock hblock) {
      /*
       * For the time being, this codes treat all errors equally; it simply invokes a poll on the AU
       * The three cases of error are (in order of appearance below):
       *   1) An exception is received instead of a digest
       *   2) A checksum cannot be found in the properties of the URL
       *   3) The stored checksum differs from the calculated one 
       * Maybe in the future, these errors should be treated individually...
       */
      boolean matched = false;
      HashBlock.Version ver = hblock.currentVersion();
      if ( ver.getHashError() != null ) {
        logger.error(String.format("Error on hash calculation of url %s", hblock.getUrl()), ver.getHashError());
        //And treat it equally to a mismatch
      } else {
        byte bdigest[] = ver.getHashes()[0];
        String sdigest = ByteArray.toHexString(bdigest);
        String url = hblock.getUrl();
        long bytes = hblock.getTotalFilteredBytes();
        logger.debug(String.format("URL %s has checksum %s [%d]", url, sdigest, bytes));
        totalBytes += bytes;
        
        CachedUrl cu = au.makeCachedUrl(url);
        CIProperties headers = cu.getProperties();
        String checksum = headers.getProperty(CachedUrl.PROPERTY_CHECKSUM);
        if (!StringUtil.isNullString(checksum))
          matched = checksum.equalsIgnoreCase(sdigest);
      }
      
      if (! matched ) {
        if ( ! errors )
          handleError();
        errors = true;
        //? How do I invoke the repair process
        //? How do I stop the hashing process
        //hasher.abortHash();
      }
    }
    @Override
    public void hashingFinished(CachedUrlSet urlset, long timeUsed,
        Object cookie, CachedUrlSetHasher hasher, Exception e) {
      logger.debug(String.format("Finished checksum verification of %s, bytes processed=%d", au.getName(), totalBytes));
    }
    
    
    private void handleError() {
     
      logger.debug("Enqueuing a V3 Content Poll on " + au.getName());
      PollSpec spec = new PollSpec(au.getAuCachedUrlSet(), Poll.V3_POLL);
      PollManager.PollReq req = new PollManager.PollReq(au)
        .setPollSpec(spec)
        .setPriority(2);
      try {
        PollManager pollManager = theDaemon.getPollManager();
        pollManager.enqueuePoll(req);
        //statusMsg = "Enqueued V3 poll for " + au.getName();
      } catch (IllegalStateException e) {
        //errMsg = "Failed to enqueue poll on "
        //  + au.getName() + ": " + e.getMessage();
      }
      
    }
  }
}
