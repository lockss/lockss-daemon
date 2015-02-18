/*
 * $Id$
 */

/*

Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;

import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.hasher.HashBlock;
import org.lockss.hasher.HashResult;
import org.lockss.protocol.VoteBlock;
import org.lockss.util.ByteArray;
import org.lockss.util.Logger;

/**
 * Keep track of which voters have which plain-hash version, and
 * generate repair candidates by finding those versions where a large
 * number of voters have a the version and we do not.
 *
 * Unfortunately, since neither the poller nor the voters report the
 * plain-hash of every version they have, we can miss some versions
 * which have good support, but the support is not visible, since the
 * voters didn't report them.
 */
final class VersionCounts implements VoteBlockTallier.VoteCallback {

  /** Our logger. */
  private static final Logger log = Logger.getLogger("VersionCounts");

  /** 
   * A mapping from byte arrays to collections of ParticipantUserData.
   * The keys are compared lexicographically rather than by identity.
   */
  private static class VersionMap extends MultiValueMap {
    void addVersion(VoteBlock.Version vbVersion, ParticipantUserData id) {
      if (! vbVersion.getHashError()) {
	HashResult plainHash = HashResult.make(vbVersion.getPlainHash());
	put(plainHash, id);
      }
    }
  }

  // A map from the plain hash to the voters who have that as their
  // head version.
  protected final VersionMap headVersionMap = new VersionMap();
  // A map from the plain hash to the voters who have that as any
  // version.
  protected final VersionMap votedVersionMap = new VersionMap();

  private VersionCounts () {
  }

  /**
   * Create a <code>VersionCounts</code>. Calls to the {@link #vote}
   * method will track the plain hash versions hashed by the voters,
   * and calls to {@link #getRepairCandidates} will return virsions
   * with at least enough versions, and exclude versions which the
   * poller has.
   */
  public static VersionCounts make() {
    return new VersionCounts();
  }

  /**
   * Incorporate all of the versions this voter is showing.
   *
   * @param voteBlock A VoteBlock for the voter.
   * @param id The ParticipantUserData representing a voter.
   */
  @Override public void vote(VoteBlock voteBlock, ParticipantUserData id) {
    VoteBlock.Version[] vbVersions = voteBlock.getVersions();
    if (vbVersions.length == 0) {
      return;
    }
    addHeadVersion(vbVersions, id);
    addVotedVersions(vbVersions, id);
  }

  private void addHeadVersion(VoteBlock.Version[] vbVersions,
			      ParticipantUserData id) {
    VoteBlock.Version vbVersion = vbVersions[0];
    headVersionMap.addVersion(vbVersion, id);
  }
 
  private void addVotedVersions(VoteBlock.Version[] vbVersions,
				ParticipantUserData id) {
    // The participant can have multiple VoteBlock.Version instances
    // with the same plain hash. But the participant still only gets
    // to enter itself in the votedVersionMap once.
    // CR: Should this enforce the "one-entry-per-voter" by using a
    // Set in the collectionFactory of VersionMap?
    VersionMap localVersionMap = new VersionMap();
    for (int versionIndex = 0; versionIndex < vbVersions.length;
	 versionIndex++) {
      if (versionIndex > 20) {
	log.warning("Participant "+id+" has "+vbVersions.length+" versions."+
		    "  Only the first "+versionIndex+" are being considered.");
	break;
      }
      VoteBlock.Version vbVersion = vbVersions[versionIndex];
      localVersionMap.addVersion(vbVersion, id);
    }
    for (HashResult votedVersion: 
	   (Collection<HashResult>)localVersionMap.keySet()) {
      // todo: Nicer place for logUniqueVersions. Assuming the
      // collectionFactory isn't a Set.
      votedVersionMap.put(votedVersion, id);
    }
  }

  /**
   * @param landslideMinimum The minimum number of votes which would
   * make a version "popular".
   * @return A Map of ParticipantUserData to plain hash values,
   * including only those with head versions which have at least the
   * minimum support.
   */
  public Map<ParticipantUserData, HashResult>
    getRepairCandidates(int landslideMinimum) {
    return getRepairCandidates(landslideMinimum, Collections.EMPTY_SET);
  }

  /**
   * @param landslideMinimum The minimum number of votes which would
   * make a version "popular".
   * @param excludedVersions A collection of plain hash values to be
   * excluded from the result.
   * @return A Map of ParticipantUserData to plain hash values,
   * including only those with head versions which have at least the
   * minimum support.
   */
  public Map<ParticipantUserData, HashResult>
    getRepairCandidates(int landslideMinimum,
		     Collection<HashResult> excludedHashes) {
    Map<ParticipantUserData, HashResult> repairCandidates =
      new HashMap<ParticipantUserData, HashResult>();
    for (HashResult headVersion: 
	   (Collection<HashResult>)headVersionMap.keySet()) {
      if (! excludedHashes.contains(headVersion)) {
	Collection<ParticipantUserData> voters =
	  votedVersionMap.getCollection(headVersion);
	// voters can't be null, since the plainHash is the head
	// version for some voters.
	if (voters.size() >= landslideMinimum) {
	  Collection<ParticipantUserData> headVoters = 
	    headVersionMap.getCollection(headVersion);
	  for (ParticipantUserData voter: headVoters) {
	    repairCandidates.put(voter, headVersion);
	  }
	}
      }
    }
    return repairCandidates;
  }

  /**
   * @param landslideMinimum The minimum number of votes which would
   * make a version "popular".
   * @param excludedVersions A collection of plain hash values to be
   * excluded from the result.
   * @return A Map of ParticipantUserData to plain hash values,
   * including only those with head versions which have at least the
   * minimum support.
   */
  public List<ParticipantUserData>
    getSortedRepairCandidates(int landslideMinimum) {
    return getSortedRepairCandidates(landslideMinimum,
				     Collections.EMPTY_SET);
  }

  public List<ParticipantUserData>
    getSortedRepairCandidates(int landslideMinimum,
			      Collection<HashResult> excludedHashes) {
    Map<Integer, Collection<ParticipantUserData>> map =
      getSortedRepairCandidatesMap(landslideMinimum, excludedHashes);
    List<ParticipantUserData> res = new ArrayList<ParticipantUserData>();
    for (Collection<ParticipantUserData> coll : map.values()) {
      res.addAll(coll);
    }
    return res;
  }

  public Map<Integer, Collection<ParticipantUserData>>
    getSortedRepairCandidatesMap(int landslideMinimum) {
    return getSortedRepairCandidatesMap(landslideMinimum,
					Collections.EMPTY_SET);
  }

  public Map<Integer, Collection<ParticipantUserData>>
    getSortedRepairCandidatesMap(int landslideMinimum,
			       Collection<HashResult> excludedHashes) {
    Map<Integer, Collection<ParticipantUserData>> repairCandidates =
      new TreeMap<Integer, Collection<ParticipantUserData>>(Collections.reverseOrder());
    for (HashResult headVersion: 
	   (Collection<HashResult>)headVersionMap.keySet()) {
      if (! excludedHashes.contains(headVersion)) {
	Collection<ParticipantUserData> voters =
	  votedVersionMap.getCollection(headVersion);
	// voters can't be null, since the plainHash is the head
	// version for some voters.
	int size = voters.size();
	if (size >= landslideMinimum) {
	  Collection<ParticipantUserData> countVoters =
	    repairCandidates.get(size);
	  if (countVoters == null) {
	    countVoters = new HashSet<ParticipantUserData>();
	    repairCandidates.put(size, countVoters);
	  }
	  Collection<ParticipantUserData> headVoters = 
	    headVersionMap.getCollection(headVersion);
	  for (ParticipantUserData voter : headVoters) {
	    countVoters.add(voter);
	  }
	}
      }
    }
    return repairCandidates;
  }
}
