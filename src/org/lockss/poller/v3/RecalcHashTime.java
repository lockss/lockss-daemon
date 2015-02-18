/*
 * $Id$
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller.v3;

import java.io.*;
import java.net.MalformedURLException;
import java.security.*;
import java.util.*;

import org.apache.commons.collections.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.CachedUrlSetHasher;
import org.lockss.hasher.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.poller.v3.V3Serializer.PollSerializerException;
import org.lockss.protocol.*;
import org.lockss.protocol.V3LcapMessage.PollNak;
import org.lockss.protocol.psm.*;
import org.lockss.state.*;
import org.lockss.scheduler.*;
import org.lockss.scheduler.Schedule.*;
import org.lockss.util.*;

import static org.lockss.poller.v3.V3Voter.*;

/**
 * Recalculate the hash estimate for an AU by running an actual hash.  This
 * is a separate class from V3Voter because it continues to run after the
 * poll has ended and the voter object reset.
 */
public class RecalcHashTime {
  private static final Logger log = Logger.getLogger("RecalcHash");

  private LockssDaemon daemon;
  protected ArchivalUnit au;
  protected int nHash;
  protected String hashAlg;
  protected long voteDuration;
  protected CachedUrlSetHasher hasher;
  protected long totalTime = 0;

  public RecalcHashTime(LockssDaemon daemon,
			ArchivalUnit au,
			int nHash,
			String hashAlg,
			long voteDuration) {
    this.daemon = daemon;
    this.au = au;
    this.nHash = nHash;
    this.hashAlg = hashAlg;
    this.voteDuration = voteDuration;
  }

  private PollManager getPollManager() {
    return daemon.getPollManager();
  }

  protected byte[][] initHasherByteArrays(int n) {
    byte[][] initBytes = new byte[n][];
    initBytes[0] = new byte[0];
    for (int ix = 1; ix < n; ix++) {
      initBytes[ix] =
	ByteArray.concat(PollUtil.makeHashNonce(V3Poller.HASH_NONCE_LENGTH),
			 PollUtil.makeHashNonce(V3Poller.HASH_NONCE_LENGTH));
    }
    return initBytes;
  }

  public void recalcHashTime() {
    if (!CurrentConfig.getBooleanParam(PARAM_RECALC_EXCESSIVE_HASH_ESTIMATE,
				       DEFAULT_RECALC_EXCESSIVE_HASH_ESTIMATE)) {
      return;
    }
    if (getPollManager().isRecalcAu(au)) {
      log.debug2("Already have recalc scheduled for " + au);
      return;
    }
    hasher = makeHasher();
    schedRecalcHash();
  }

  protected CachedUrlSetHasher makeHasher() {
    return new RecalcHashTimeHasher(au.getAuCachedUrlSet(),
				    PollUtil.createMessageDigestArray(nHash,
								      hashAlg),
				    initHasherByteArrays(nHash),
				    getRecalcDuration(voteDuration));
  }

  protected boolean schedRecalcHash() {
    Deadline deadline = Deadline.in(voteDuration);
    long durationIncr = voteDuration * 2;
    Deadline latestFinish = Deadline.in(voteDuration * 11);
    HashService hashService = daemon.getHashService();
    while (deadline.before(latestFinish)) {
      try {
	if (hashService.scheduleHash(hasher,
				     deadline,
				     new RecalcHashTimeCallback(),
				     null)) {
	  if (log.isDebug()) {
	      log.debug("Hash estimate recalc scheduled by " + deadline +
			" for " + au);
	  }
	  getPollManager().addRecalcAu(au);
	  return true;
	}
      } catch (IllegalArgumentException e) {
	log.error("Error scheduling duration recalc", e);
	return false;
      }
      deadline.later(durationIncr);
    }
    log.debug("Couldn't schedule estimate recalc by " + latestFinish +
	      " for " + au);
    return false;
  }
  
  long getRecalcDuration(long voteDuration) {
    double voteDurationRecip = 2.0 / PollUtil.getVoteDurationMultiplier();
    return (long)(voteDuration *
		  CurrentConfig.getDoubleParam(PARAM_RECALC_HASH_ESTIMATE_VOTE_DURATION_MULTIPLIER,
					       voteDurationRecip));
  }

  protected static class RecalcHashTimeHasher extends BlockHasher {
    private long estDuration;

    public RecalcHashTimeHasher(CachedUrlSet cus,
				MessageDigest[] digests,
				byte[][] initByteArrays,
				long estDuration) {
      super(cus, digests, initByteArrays, new RecalcHashEventHandler());
      this.estDuration = estDuration;
    }

    private String ts = null;
    public String typeString() {
      if (ts == null) {
	ts = "E(" + initialDigests.length + ")";
      }
      return ts;
    }

    public void storeActualHashDuration(long elapsed, Exception err) {
      if (err instanceof HashService.SetEstimate) {
	super.storeActualHashDuration(elapsed, err);
      }
    }

    public long getEstimatedHashDuration() {
      return estDuration;
    }

    // Suppress this, as will continue hash after timeout
    public void abortHash() {
    }

    // Use when really want to abort.
    public void abortRecalc() {
      super.abortHash();
    }
  }

  private static class RecalcHashEventHandler
    implements BlockHasher.EventHandler {

    public void blockStart(HashBlock block) { 
    }
    public void blockDone(HashBlock block) {
    }
  }

  private class RecalcHashTimeCallback implements HashService.Callback {
    public void hashingFinished(CachedUrlSet cus, long timeUsed, Object cookie,
                                CachedUrlSetHasher hasher, Exception e) {
      totalTime += timeUsed;
      if (e == null) {
	log.debug("Recalc finished, setting hash estimate to " +
		  StringUtil.timeIntervalToString(totalTime) +
		  " for " + au);
	hasher.storeActualHashDuration(totalTime,
				       new HashService.SetEstimate());
	getPollManager().removeRecalcAu(au);
      } else if (e instanceof HashService.Timeout
		 || e instanceof SchedService.Timeout) {
	log.debug("Recalc timed out after " +
		  StringUtil.timeIntervalToString(totalTime) +
		  ", rescheduling " + au);
	// run again with the same hasher, which will pick up from where it
	// left off
	schedRecalcHash();
      } else {
	log.warning("Recalc hash failed for " + au, e);
	((RecalcHashTimeHasher)hasher).abortRecalc();
	getPollManager().removeRecalcAu(au);
      }
    }
  }
}
