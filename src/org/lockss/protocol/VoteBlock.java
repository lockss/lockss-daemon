/*
 * $Id: VoteBlock.java,v 1.6 2005-11-16 07:44:09 smorabito Exp $
 */

/*
  Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.protocol;

import java.util.*;

import org.mortbay.util.*;

import org.lockss.util.*;
import org.lockss.util.StringUtil;

/**
 * A simple bean representing a V3 vote block -- a file, or part of a file.
 */
public class VoteBlock implements LockssSerializable {

  /** Vote type enum.  Is this a vote on content, headers, or metadata? */
  public static final int CONTENT_VOTE = 0;
  public static final int HEADER_VOTE = 1;
  public static final int METADATA_VOTE = 3;
  private static final String[] voteTypeStrings = {
    "Content", "Header", "Metadata"
  };

  private int pollType;
  private String fileName;
  private long filteredLength = 0;
  private long filteredOffset = 0;
  private long unfilteredLength = 0;
  private long unfilteredOffset = 0;
  private byte[] contentHash;

  public VoteBlock() {}

  public VoteBlock(String fileName, long fLength, long fOffset,
		   long uLength, long uOffset, byte[] hash, int pollType) {
    this.fileName = fileName;
    this.filteredLength = fLength;
    this.filteredOffset = fOffset;
    this.unfilteredLength = uLength;
    this.unfilteredOffset = uOffset;
    this.contentHash = hash;
    this.pollType = pollType;
  }

  public String getUrl() {
    return fileName;
  }

  public void setFileName(String s) {
    this.fileName = s;
  }

  public long getFilteredLength() {
    return filteredLength;
  }

  public void setFilteredLength(long l) {
    this.filteredLength = l;
  }

  public long getFilteredOffset() {
    return filteredOffset;
  }

  public void setFilteredOffset(long l) {
    this.filteredOffset = l;
  }

  public long getUnfilteredLength() {
    return unfilteredLength;
  }

  public void setUnfilteredLength(long l) {
    this.unfilteredLength = l;
  }

  public long getUnfilteredOffset() {
    return unfilteredOffset;
  }

  public void setUnfilteredOffset(long l) {
    this.unfilteredOffset = l;
  }

  public byte[] getHash() {
    return contentHash;
  }

  public void setHash(byte[] b) {
    this.contentHash = b;
  }

  public void setVoteType(int type) {
    this.pollType = type;
  }

  public int getVoteType() {
    return pollType;
  }

  public String getVoteTypeString() {
    if (pollType >= 0 && pollType < voteTypeStrings.length) {
      return voteTypeStrings[pollType];
    } else {
      return "Unknown";
    }
  }

  public String toString() {
    StringBuffer sb = new StringBuffer("[VoteBlock: ");
    sb.append("vt = " + getVoteTypeString() + ", ");
    sb.append("fn = " + fileName + ", ");
    sb.append("fl = " + filteredLength + ", ");
    sb.append("fo = " + filteredOffset + ", ");
    sb.append("ul = " + unfilteredLength + ", ");
    sb.append("uo = " + unfilteredOffset + ", ");
    sb.append("ch = " +
	      contentHash == null ? "null" : new String(B64Code.encode(contentHash))
	      + ", ");
    return sb.toString();
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }

    if (!(o instanceof VoteBlock)) {
      return false;
    }

    VoteBlock vb = (VoteBlock)o;
    return StringUtil.equalStrings(vb.fileName, fileName) &&
      vb.pollType == pollType &&
      vb.filteredLength == filteredLength &&
      vb.filteredOffset == filteredOffset &&
      vb.unfilteredLength == unfilteredLength &&
      vb.unfilteredOffset == unfilteredOffset &&
      Arrays.equals(vb.contentHash, contentHash);
  }

  public int hashCode() {
    int result = 17;
    result = 37 * result + fileName.hashCode();
    result = 37 * result + pollType;
    result = (int)(37 * result + filteredLength);
    result = (int)(37 * result + filteredOffset);
    result = (int)(37 * result + unfilteredLength);
    result = (int)(37 * result + unfilteredOffset);
    for (int i = 0; i < contentHash.length; i++) {
      result = 37 * result + contentHash[i];
    }
    return result;
  }
}
