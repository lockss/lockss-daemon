/*
 * $Id$
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.mbf;

import java.io.*;
import java.security.*;

import org.lockss.plugin.*;
import org.lockss.daemon.*;
import org.lockss.protocol.*;

/**
 * @author David S. H. Rosenthal
 * @version 1.0
 */
public class MBFV2 extends MemoryBoundFunctionVote {
  private static final String algHash = "SHA1";
  private static final int Finished = 0;
  private static final int StartingGeneration = 1;
  private static final int StartingVerification = 2;
  private static final int FirstBlockProofGeneration = 3;
  private static final int BlockHashGeneration = 4;
  private static final int BlockProofGeneration = 5;
  private static final int FirstBlockProofVerification = 6;
  private static final int BlockHashVerification = 7;
  private static final int BlockProofVerification = 8;
  private static final int Bad = 9;
  private static final String[] stateName = {
    "Finished",
    "StartingGeneration",
    "StartingVerification",
    "FirstBlockProofGeneration",
    "BlockHashGeneration",
    "BlockProofGeneration",
    "FirstBlockProofVerification",
    "BlockHashVerification",
    "BlockProofVerification",
    "Bad",
  };

  private int currentState;
  private MessageDigest proofDigest;
  private MemoryBoundFunction mbf;
  private MessageDigest contentDigest;
  private CachedUrlSetHasher hasher;
  private int[] thisProof;
  private byte[] thisHash;
  private int[] thisSignature;
  private int index;
  private int bytesToGo;
  private int[][] ourProofs;
  private byte[][] ourHashes;
  private byte[] firstProofNonce;

  /**
   * No-argument constructor for use with Class.newInstance().
   */
  public MBFV2() {
    proofDigest = null;
    mbf = null;
    contentDigest = null;
    hasher = null;
    thisProof = null;
    thisHash = null;
    thisSignature = null;
    index = -1;
    bytesToGo = -1;
    firstProofNonce = null;
    currentState = Bad;
    if (false)
      logger.setLevel(6);
  }

  /**
   * Public constructor for an object that will compute a vote
   * using hashing and memory bound functions.
   * It accepts as input a nonce and a CachedUrlSet.  It divides the
   * content into i blocks of length <= 2**(i+1) and for each computes
   * the MBF proof and the hash.  The first proof depends on the nonce
   * the AU name and the first bytes of the AU.  Subsequent proofs
   * depend on the preceeding hash and proof.  The effort sizer e
   * is constant for all rounds.  The path length l is set equal to
   * the block size for that round.
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param cusVal the CachedUrlSet containing the content to be voted on
   * @param pollID the ID of the poll
   * @param voterID the ID of the voter
   *
   */
  protected void setupGeneration(MemoryBoundFunctionFactory fact,
				 byte[] nVal,
				 int eVal,
				 CachedUrlSet cusVal,
				 byte[] pollID,
				 PeerIdentity voterID)
    throws MemoryBoundFunctionException {
    super.setupGeneration(fact, nVal, eVal, cusVal, pollID, voterID);
    setup(nVal, eVal, cusVal, pollID, voterID);
    setState(StartingGeneration);
    logParams();
  }

  /**
   * Public constructor for an object that will verify a vote
   * using hashing and memory bound functions.  It accepts as
   * input a nonce,  a cachedUrlSet,  and arrays of proofs
   * and hashes.
   * @param nVal a byte array containing the nonce
   * @param eVal the effort sizer (# of low-order zeros in destination)
   * @param cusVal the CachedUrlSet containing the content to be voted on
   * @param sVals the starting points chosen by the prover for each block
   * @param hashes the hashes of each block
   *
   */
  public void setupVerification(MemoryBoundFunctionFactory fact,
				byte[] nVal,
				int eVal,
				CachedUrlSet cusVal,
				int sVals[][],
				byte[][] hashes,
				byte[] pollID,
				PeerIdentity voterID)
    throws MemoryBoundFunctionException {
    super.setupVerification(fact, nVal, eVal, cusVal, sVals, hashes,
			    pollID, voterID);
    setup(nVal, eVal, cusVal, pollID, voterID);
    setState(StartingVerification);
    ourProofs = getProofArray();
    ourHashes = getHashArray();
    if (ourProofs.length != ourHashes.length)
      throw new MemoryBoundFunctionException("proofs " + ourProofs.length +
					     " != hashes " + ourHashes.length);

    logParams();
  }

  private void setup(byte[] nVal,
		     int eVal,
		     CachedUrlSet cusVal,
		     byte[] pollID,
		     PeerIdentity voterID) throws
    MemoryBoundFunctionException {
    finished = false;
    if (proofDigest == null) try {
      proofDigest = MessageDigest.getInstance(algHash);
    } catch (NoSuchAlgorithmException ex) {
      throw new MemoryBoundFunctionException(algHash + " throws " +
					     ex.toString());
    }
    if (contentDigest == null) try {
      contentDigest = MessageDigest.getInstance(algHash);
    } catch (NoSuchAlgorithmException ex) {
      throw new MemoryBoundFunctionException(algHash + " throws " +
					     ex.toString());
    }
    hasher = cusVal.getContentHasher(contentDigest);
    mbf = null;
    thisProof = null;
    thisHash = null;
    index = -1;
  }

  private void setState(int newState) {
    logger.debug("Change state from " + stateName[currentState] + " to " +
		stateName[newState]);
    currentState = newState;
    if (currentState == Finished)
      logger.info("MBFV2: valid " + valid + " agreeing " + agreeing);
  }

  /**
   * Do "n" steps of the underlying hash or effort proof generation
   * @param n number of steps to move.
   * @return true if there is more work to do
   *
   */
  public boolean computeSteps(int n) throws MemoryBoundFunctionException {
    logger.debug("computeSteps: block " + index + " steps " + n + " state " +
		 stateName[currentState]);
    switch (currentState) {
    case Finished:
      return false;
    case StartingGeneration:
      startingGeneration(n);
      break;
    case StartingVerification:
      startingVerification(n);
      break;
    case FirstBlockProofGeneration:
      firstBlockProofGeneration(n);
      break;
    case BlockHashGeneration:
      blockHashGeneration(n);
      break;
    case BlockProofGeneration:
      blockProofGeneration(n);
      break;
    case FirstBlockProofVerification:
      firstBlockProofVerification(n);
      break;
    case BlockHashVerification:
      blockHashVerification(n);
      break;
    case BlockProofVerification:
      blockProofVerification(n);
      break;
    default:
      throw new MemoryBoundFunctionException("bad state " + currentState);
    }
    if (currentState == Finished)
      finished = true;
    return (!finished);
  }

/*
First block. Nonce for the proof is the hash of:

- Nonce in message
- Poll ID
- AU ID (url)
- IP address of voter

Second and succeeding blocks. Nonce for the proof is the hash of:

- Hash of preceeding block
- Signature of proof of preceeding block

The signature of a proof is a variable-size array of ints.

The signature of a non-empty proof contains,  for each index in
the proof (i,e, each index resulting in a path with the required
number of low-order zeros),  the final value fetched from the
basis array T during traversal of the path.

The signature of an empty proof contains,  for every index in the
range of the proof,  the final value fetched from the
basis array T during traversal of the path.

Both arrays of 32-bit ints can be created only by following the
paths in the range.
*/

  private byte[] makeFirstBlockProofNonce()
    throws MemoryBoundFunctionException {
    proofDigest.update(nonce);  // The nonce from the message
    proofDigest.update(poll);   // The poll ID
    if (cus != null) {
      ArchivalUnit au = cus.getArchivalUnit();
      String auId = au.getAuId();
      proofDigest.update(auId.getBytes());  // The AU ID
    } else {
      throw new MemoryBoundFunctionException("no CUS");
    }
    proofDigest.update(voter);  // Voter identity
    return proofDigest.digest();
  }

  private byte[] makeSubsequentBlockProofNonce(byte[] prevHash,
					       int[] prevProof) {
    // Second or subsequent block - nonce is previous hash and
    // previous proof signature
    proofDigest.update(prevHash);  //  Previous hash
    logger.debug("block " + index + " hash " + byteArrayToString(prevHash));
    if (thisSignature == null) {
      // Use the previous proof - dangerous because too few bits
      logger.debug("no signature");
      if (prevProof.length > 0)
	for (int i = 0; i < prevProof.length; i++) {
	  byte[] tmp = intToByteArray(prevProof[i]);
	  logger.debug("proof " + byteArrayToString(tmp));
	  proofDigest.update(tmp);
      } else {
	logger.debug("block " + index + " 0-length proof");
	proofDigest.update(nonce);  // The previous proof itself
      }
    } else {
      // Use the signature array as the nonce
      logger.debug("signature length " + thisSignature.length);
      for (int i = 0; i < thisSignature.length; i++) {
	byte[] signature = intToByteArray(thisSignature[i]);
	logger.debug("signature " + i + " is " + byteArrayToString(signature));
	proofDigest.update(signature);  // The signature of the previous proof
      }
      thisSignature = null;
    }
    return proofDigest.digest();
  }

  private void startingGeneration(int n) throws MemoryBoundFunctionException {
    if (verify || currentState != StartingGeneration)
      throw new MemoryBoundFunctionException("bad state");
    if (index >= 0)
      throw new MemoryBoundFunctionException("bad index " + index + " in " +
					     stateName[currentState]);
    firstProofNonce = makeFirstBlockProofNonce();
    thisHash = null;
    thisProof = null;
    setState(FirstBlockProofGeneration);
  }

  private void startingVerification(int n) throws MemoryBoundFunctionException {
    if (!verify || currentState != StartingVerification)
      throw new MemoryBoundFunctionException("bad state");
    if (index >= 0)
      throw new MemoryBoundFunctionException("bad index " + index + " in " +
					     stateName[currentState]);
    logger.debug("starting verification " + ourProofs.length + "/" +
		ourHashes.length);
    logger.debug("proof "  + proofToString(ourProofs));
    firstProofNonce = makeFirstBlockProofNonce();
    logger.debug("verifySteps: index < 0");
    thisHash = null;
    thisProof = null;
    setState(FirstBlockProofVerification);
  }

  private void firstBlockProofGeneration(int n) throws MemoryBoundFunctionException {
    if (verify || currentState != FirstBlockProofGeneration)
      throw new MemoryBoundFunctionException("bad state");
    if (thisHash != null)
      throw new MemoryBoundFunctionException("thisHash should be null in " +
					     stateName[currentState]);
    if (thisProof != null)
      throw new MemoryBoundFunctionException("thisProof should be null in " +
					     stateName[currentState]);
    if (index >= 0)
      throw new MemoryBoundFunctionException("index should be -1 in " +
					     stateName[currentState]);
    if (mbf != null)
      throw new MemoryBoundFunctionException("mbf should be null in " +
					     stateName[currentState]);
    if (firstProofNonce == null)
      throw new MemoryBoundFunctionException("firstProofNonce should not be null in " +
					     stateName[currentState]);
    try{
      logger.debug("MBFV2: nonce " + byteArrayToString(firstProofNonce));
      mbf = factory.makeGenerator(firstProofNonce, e, 1, 1);
    } catch (NoSuchAlgorithmException ex) {
      throw new MemoryBoundFunctionException("factory throws " +
					     ex.toString());
    } catch (InstantiationException ex) {
      throw new MemoryBoundFunctionException("factory throws " +
					     ex.toString());
    } catch (IllegalAccessException ex) {
      throw new MemoryBoundFunctionException("factory throws " +
					     ex.toString());
    }
    index = 0;
    setState(BlockProofGeneration);
  }

  private void blockHashGeneration(int n) throws MemoryBoundFunctionException {
    if (verify || currentState != BlockHashGeneration)
      throw new MemoryBoundFunctionException("bad state");
    if (thisHash != null)
      throw new MemoryBoundFunctionException("thisHash should be null" + " in " +
					     stateName[currentState]);
    if (thisProof == null)
      throw new MemoryBoundFunctionException("thisProof should not be null" + " in " +
					     stateName[currentState]);
    if (bytesToGo <= 0)
      throw new MemoryBoundFunctionException("bytesToGo should be > 0" + " in " +
					     stateName[currentState]);
    if (index < 0)
      throw new MemoryBoundFunctionException("index should be >= 0" + " in " +
					     stateName[currentState]);
    try {
      int bytesHashed = hasher.hashStep(bytesToGo > n ? n : bytesToGo);
      logger.debug("generateSteps: hashed " + bytesHashed + "/" + bytesToGo
		  + " bytes");
      if (bytesHashed <= 0)
	throw new MemoryBoundFunctionException("too few bytes hashed " +
					       bytesHashed + " in " +
					     stateName[currentState]);
      bytesToGo -= bytesHashed;
      boolean done = hasher.finished();
      if (bytesToGo <= 0 || done) {
	// Extract the hash of this block and reset hasher for next
	thisHash = contentDigest.digest();
	if (thisHash == null)
	  throw new MemoryBoundFunctionException("contentDigest return null" + " in " +
					     stateName[currentState]);
  	saveHash(index, thisHash);
	logger.debug("generateSteps: finished hashing block");
      }
      if (done) {
	finished = true;
	logger.debug("generateSteps: finished hashing AU");
	setState(Finished);
      } else if (bytesToGo <= 0) {
	index++;
	setState(BlockProofGeneration);
      }
    } catch (IOException ex) {
      throw new MemoryBoundFunctionException("content hash threw " +
					     ex.toString() + " in " +
					     stateName[currentState]);
    }
  }

  private void blockProofGeneration(int n) throws MemoryBoundFunctionException {
    if (verify || currentState != BlockProofGeneration)
      throw new MemoryBoundFunctionException("bad state");
    if (index < 0)
      throw new MemoryBoundFunctionException("index should be >= 0" + " in " +
					     stateName[currentState]);
    if (mbf == null) try {
      if (thisHash == null)
	throw new MemoryBoundFunctionException("thisHash should not be null in " +
					       stateName[currentState]);
      if (thisProof == null)
	throw new MemoryBoundFunctionException("thisProof should not be null in " +
					     stateName[currentState]);
      byte[] nonce1 = makeSubsequentBlockProofNonce(thisHash, thisProof);
      logger.debug("MBFV2: nonce " + byteArrayToString(nonce1));
      logger.debug("generateSteps: index " + index);
      thisHash = null;
      thisProof = null;
      mbf = factory.makeGenerator(nonce1, e, (1 << index), index/2);
    } catch (NoSuchAlgorithmException ex) {
      throw new MemoryBoundFunctionException("factory throws " +
					     ex.toString() + " in " +
					     stateName[currentState]);
    } catch (InstantiationException ex) {
      throw new MemoryBoundFunctionException("factory throws " +
					     ex.toString() + " in " +
					     stateName[currentState]);
    } catch (IllegalAccessException ex) {
      throw new MemoryBoundFunctionException("factory throws " +
					     ex.toString());
    } else {
      if (thisProof != null)
	throw new MemoryBoundFunctionException("thisProof should be null in " +
					     stateName[currentState]);
      if (thisHash != null)
	throw new MemoryBoundFunctionException("thisHash should be null in " +
					       stateName[currentState]);
    }
    // Work on generating the proof
    mbf.computeSteps(n);
    if (mbf.finished()) {
      logger.debug("generateSteps: proof finished");
      // Finished
      thisProof = mbf.result();
      if (thisProof == null)
	throw new MemoryBoundFunctionException("Null proof in " +
					       stateName[currentState]);
      saveProof(index, thisProof);
      thisSignature = mbf.signatureArray();
      bytesToGo = (1 << index);
      setState(BlockHashGeneration);
      mbf = null;
    }
  }

  private void firstBlockProofVerification(int n) throws MemoryBoundFunctionException {
    if (!verify || currentState != FirstBlockProofVerification)
      throw new MemoryBoundFunctionException("bad state");
    if (thisHash != null)
      throw new MemoryBoundFunctionException("thisHash should be null" + " in " +
					     stateName[currentState]);
    if (thisProof != null)
      throw new MemoryBoundFunctionException("thisProof should be null" + " in " +
					     stateName[currentState]);
    if (index > 0)
      throw new MemoryBoundFunctionException("index should be <= 0" + " in " +
					     stateName[currentState]);
    if (mbf != null)
      throw new MemoryBoundFunctionException("mbf should be null" + " in " +
					     stateName[currentState]);
    if (firstProofNonce == null)
      throw new MemoryBoundFunctionException("firstProofNonce should not be null in " +
					     stateName[currentState]);
    index = 0;
    thisProof = ourProofs[index];
    thisHash = null;
    try{
      logger.debug("MBFV2: nonce " + byteArrayToString(firstProofNonce));
      mbf = factory.makeVerifier(firstProofNonce, e, 1, 1,
				 thisProof, 100); // XXX fix this
    } catch (NoSuchAlgorithmException ex) {
      throw new MemoryBoundFunctionException("factory throws " +
					     ex.toString() + " in " +
					     stateName[currentState]);
    } catch (InstantiationException ex) {
      throw new MemoryBoundFunctionException("factory throws " +
					     ex.toString() + " in " +
					     stateName[currentState]);
    } catch (IllegalAccessException ex) {
      throw new MemoryBoundFunctionException("factory throws " +
					     ex.toString());
    }
    setState(BlockProofVerification);
  }

  private void blockHashVerification(int n) throws MemoryBoundFunctionException {
    if (!verify || currentState != BlockHashVerification)
      throw new MemoryBoundFunctionException("bad state");
    if (thisHash != null)
      throw new MemoryBoundFunctionException("thisHash should be null in " +
					     stateName[currentState]);
    if (thisProof != null)
      throw new MemoryBoundFunctionException("thisProof should be null" + " in " +
					     stateName[currentState]);
    if (bytesToGo <= 0)
      throw new MemoryBoundFunctionException("bytesToGo should be > 0" + " in " +
					     stateName[currentState]);
    if (index < 0)
      throw new MemoryBoundFunctionException("index should be >= 0" + " in " +
					     stateName[currentState]);
    /* if (agreeing) */ try {
      int bytesHashed = hasher.hashStep(bytesToGo > n ? n : bytesToGo);
      logger.debug("verifySteps: hashed " + bytesHashed + "/" + bytesToGo
		  + " bytes");
      bytesToGo -= bytesHashed;
      boolean done = hasher.finished();
      if (bytesToGo <= 0 || done) {
	// Extract the hash of this block and reset hasher for next
	thisHash = ourHashes[index];
	byte[] newHash = contentDigest.digest();
	if (!MessageDigest.isEqual(thisHash, newHash)) {
	  // Hashes don't match
	  StringBuffer gen = new StringBuffer();
	  for (int i = 0; i < thisHash.length; i++) {
	    gen.append(thisHash[i]);
	    if (i < (thisHash.length - 1))
	      gen.append(",");
	  }
	  StringBuffer ver = new StringBuffer();
	  for (int i = 0; i < newHash.length; i++) {
	    ver.append(newHash[i]);
	    if (i < (newHash.length - 1))
	      ver.append(",");
	  }
	  logger.debug("verifySteps: [" + gen.toString() + "] != [" +
		      ver.toString() + "]");
	  agreeing = false;
	}
	logger.debug("verifySteps: finished hashing block");
	mbf = null;
	thisHash = null;
	thisProof = null;
      }
      if (done) {
	finished = true;
	logger.debug("verifySteps: finished hashing AU");
	setState(Finished);
      } else if (bytesToGo <= 0) {
	index++;
	setState(BlockProofVerification);
      }
    } catch (IOException ex) {
      throw new MemoryBoundFunctionException("content hash threw " +
					     ex.toString() + " in " +
					     stateName[currentState]);
    }
  }

  private void blockProofVerification(int n) throws MemoryBoundFunctionException {
    if (!verify || currentState != BlockProofVerification)
      throw new MemoryBoundFunctionException("bad state");
    if (index < 0)
      throw new MemoryBoundFunctionException("index should be >= 0" + " in " +
					     stateName[currentState]);
    if (thisHash != null)
      throw new MemoryBoundFunctionException("thisHash should be null" + " in " +
					     stateName[currentState]);
    if (mbf == null) try {
      if (index < 1)
	throw new MemoryBoundFunctionException("index should be > 0" + " in " +
					     stateName[currentState]);

      // Second or subsequent block - nonce is previous hash and
      // previous proof.
      byte[] nonce1 = makeSubsequentBlockProofNonce(ourHashes[index-1],
						    ourProofs[index-1]);
      logger.debug("MBFV2: nonce " + index + " " + byteArrayToString(nonce1));
      thisProof = ourProofs[index];
      mbf = factory.makeVerifier(nonce1, e, (1 << index), index/2,
				 thisProof, 100); // XXX fix this
    } catch (NoSuchAlgorithmException ex) {
      throw new MemoryBoundFunctionException("factory throws " +
					     ex.toString() + " in " +
					     stateName[currentState]);
    } catch (InstantiationException ex) {
      throw new MemoryBoundFunctionException("factory throws " +
					     ex.toString() + " in " +
					     stateName[currentState]);
    } catch (IllegalAccessException ex) {
      throw new MemoryBoundFunctionException("factory throws " +
					     ex.toString());
    }
    if (thisProof == null)
      throw new MemoryBoundFunctionException("thisProof should not be null in " +
					     stateName[currentState]);
    // No proof yet - work on proof
    mbf.computeSteps(n);
    if (mbf.finished()) {
      // Finished
      thisProof = null;
      thisSignature = mbf.signatureArray();
      logger.debug("verifier signature " + ((thisSignature == null) ? "null" :
				       "length " + thisSignature.length));
      bytesToGo = (1 << index);
      if (mbf.result() == null) {
	logger.debug("verifySteps: proof invalid");
	valid = false;
	agreeing = false;
	setState(Finished);
      } else {
	setState(BlockHashVerification);
      }
    }
  }

  private byte[] intToByteArray(int b) {
    byte[] ret = new byte[4];
    ret[0] = (byte)(b & 0xff);
    ret[1] = (byte)((b >> 8) & 0xff);
    ret[2] = (byte)((b >> 16) & 0xff);
    ret[3] = (byte)((b >> 24) & 0xff);
    return (ret);
  }

  private String proofToString(int[][] arr) {
    StringBuffer sb = new StringBuffer();
    sb.append("{");
    for (int i = 0; i < arr.length; i++) {
      sb.append("{");
      for (int j = 0; j < arr[i].length; j++) {
	sb.append(arr[i][j]);
	if (j < (arr[i].length - 1))
	  sb.append(",");
      }
      if (i < (arr.length - 1))
	sb.append("},");
      else
	sb.append("}");
    }
    sb.append("}");
    return sb.toString();
  }

  private String hashesToString(byte[][] arr) {
    StringBuffer sb = new StringBuffer();
    sb.append("{");
    for (int i = 0; i < arr.length; i++)
      sb.append(byteArrayToString(arr[i]));
    sb.append("}");
    return sb.toString();
  }

  private String byteArrayToString(byte[] arr) {
    StringBuffer sb = new StringBuffer();
    sb.append("{");
    for (int i = 0; i < arr.length; i++) {
      sb.append(arr[i]);
      if (i < (arr.length -1))
	sb.append(",");
    }
    sb.append("}");
    return sb.toString();
  }

  private void logParams() {
    logger.debug("nonce " + byteArrayToString(nonce));
    if (ourProofs != null)
      logger.debug("proof " + proofToString(ourProofs));
    if (ourHashes != null)
      logger.debug("hashes " + hashesToString(ourHashes));

  }

}
