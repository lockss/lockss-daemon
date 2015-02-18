/*
 * $Id$
 */

/*

Copyright (c) 2011 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.protocol.PeerIdentity;
import org.lockss.protocol.VoteBlock;
import org.lockss.protocol.VoteBlocks;
import org.lockss.protocol.VoteBlocksIterator;
import org.lockss.util.*;

/**
 * <p>This class is used by {@link V3Poller} to step through the union
 * of the URLs at the poller and all the voters. For each URL, in
 * order, the poller will provide a VoteCallback, and this class will
 * call the appropriate method on the VoteCallback for each
 * participant.
 */
final class UrlTallier {
  private static Logger log = Logger.getLogger(UrlTallier.class);

  // Note: Use of the ParticipantUserData here ties this class to
  // V3Poller pretty tightly.

  /**
   * An interface to be implemented by caller, to get VoteBlock
   * instances.
   */
  public interface VoteCallback {
    /** The voter's iterator had a VoteBlock for the current URL. */
    public void vote(VoteBlock voteBlock, ParticipantUserData id,
		     int participantIndex);
    /** The voter's iterator had no VoteBlock for the current URL. */
    public void voteMissing(ParticipantUserData id);
    /** The voter's iterator has been found defective. */
    public void voteSpoiled(ParticipantUserData id);
  }

  private static final VoteCallback NULL_CALLBACK = new VoteCallback() {
      @Override public void vote(VoteBlock voteBlock, ParticipantUserData id,
				 int participantIndex) {}
      @Override public void voteMissing(ParticipantUserData id) {}
      @Override public void voteSpoiled(ParticipantUserData id) {}
    };

  /**
   * The participants in the poll. This is kept as a List to make sure
   * that the Iterator will always return the elements in the same
   * order.
   */
  private final List<ParticipantUserData> participants;
  /**
   * A coordinator of the VoteBlocksIterator instances of the
   * participants. This uses the same index numbers as the List of
   * participants.
   */
  private final VoteBlocksCoordinator coordinator;

  // package level for unit testing of this function.
  static final VoteBlocksIterator 
    getIterator(ParticipantUserData participant) {
    VoteBlocks voteBlocks = participant.getVoteBlocks();

    if (voteBlocks == null) {
      log.warning("Voter " + participant + " has no voteBlocks.");
      return null;
    }

    VoteBlocksIterator iter = null;
    try {
      iter = voteBlocks.iterator();
    } catch (FileNotFoundException e) {
      log.warning("Voter " + participant + " threw:", e);
      return null;
    }

    if (iter == null) {
      throw new ShouldNotHappenException("Voter " + participant +
					 " has a null iterator");
    }
    return iter;
  }

  /**
   * Extract the {@link VoteBlocksIterator} from each participant.
   * @param participants An ordered List of voters.
   * @return An ordered List of {@link VoteBlocksIterator}s.
   */
  private static final List<VoteBlocksIterator>
    getIterators(List<ParticipantUserData> participants) {
    List<VoteBlocksIterator> iterators = new ArrayList<VoteBlocksIterator>();

    for (ParticipantUserData participant : participants) {
      iterators.add(getIterator(participant));
    }
    return iterators;
  }

  /**
   * Create a new {@link UrlTallier}.
   * @param participants The participants. {@link UrlTallier} will
   * make and use a defensive copy.
   */
  public UrlTallier(Collection<ParticipantUserData> participants) {
    // Make a defensive copy of the Collection as a List. This keeps
    // us safe against the size or order of participants changing, and
    // the VoteBlocksCoordinator's indexes will always correspond to
    // the indexes in participants.
    this.participants = 
      Collections.unmodifiableList(new ArrayList(participants));
    this.coordinator = 
      new VoteBlocksCoordinator(getIterators(this.participants));
  }

  /**
   * Release unneeded resources used by this object at the end of a poll.
   */
  void release() {
    coordinator.release();
  }

  /**
   * Peek at the next URL known to any participant.
   * @return The next URL known to any participant, or null if
   * there are no partcipants with URLs remaining.
   */
  String peekUrl() {
    return coordinator.peekUrl();
  }
    
  /**
   * <p>Skip all the voters' URLs which are less than the given's
   * URL. Can be useful when checking a repair. The poller has the
   * given URL.</p>
   *
   * @param url Must be non-null.
   */
  void seek(String url) {
    coordinator.seek(url);
  }

  /**
   * For each participant, call exactly one of {@link
   * VoteCallback#voteSpoiled}, {@link
   * VoteCallback#voteMissing}, or {@link VoteCallback#vote}
   * on the supplied {@link VoteCallback}.
   *
   * @param url The URL in question.
   * @param voteCallback The {@link VoteCallback} to call.
   * @throws IllegalArgumentException if the URL is {@code null}, or
   * sorts before a previously supplied URL, or is greater than the
   * current value of {@link #peekUrl}.
   */
  public void voteAllParticipants(String url,
				  VoteCallback voteCallback) {
    // Check with the coordinator before voting anyone.
    coordinator.checkUrl(url);
    int i = 0;
    for (ParticipantUserData participant: participants) {
      if (coordinator.isSpoiled(i)) {
	voteCallback.voteSpoiled(participant);
      } else {
	VoteBlock voteBlock = coordinator.getVoteBlock(url, i);
	if (voteBlock == null) {
	  voteCallback.voteMissing(participant);
	} else {
	  voteCallback.vote(voteBlock, participant, i);
	}
      }
      i++;
    }
  }

  /**
   * Skip over the given URL.
   * @param url the URL in question
   * @throws IllegalArgumentException if the URL is {@code null}, or
   * sorts before a previously supplied URL, or is greater than the
   * current value of {@link #peekUrl}.
   */
  public void voteNoParticipants(String url) {
    voteAllParticipants(url, NULL_CALLBACK);
  }
}
