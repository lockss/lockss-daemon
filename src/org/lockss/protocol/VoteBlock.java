/*
 * $Id$
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
import java.io.*;

import org.mortbay.util.*;

import org.lockss.util.*;
import org.lockss.util.StringUtil;

/**
 * A simple bean representing a V3 vote block -- a file, or part of a file.
 */
public class VoteBlock implements LockssSerializable {
  /* Keys */
  public static final String VB_URL = "url"; // File name
  public static final String VB_VT = "vt"; // Vote type (content, metadata, etc.)
  public static final String VB_VERSION_COUNT = "vc"; // Number of versions
  public static final String VB_VERSIONS = "vn"; // Encoded list of versions
  public static final String VB_FLEN = "fl"; // Filtered length
  public static final String VB_FOFFSET = "fo"; // Filtered offset
  public static final String VB_ULEN = "ul"; // Unfiltered length
  public static final String VB_UOFFSET = "uo"; // Unfiltered offset
  public static final String VB_NH = "nh"; // Nonced hash
  public static final String VB_PH = "ph"; // Plain hash
  public static final String VB_ERR = "err"; // Hashing error flag

  /* Vote type enum.  Is this a vote on content, headers, or metadata? */
  public static final int CONTENT_VOTE = 0;
  public static final int HEADER_VOTE = 1;
  public static final int METADATA_VOTE = 2;
  private static final String[] voteTypeStrings = 
      { "Content", "Header", "Metadata" };

  // private VoteBlock.Version[] versionArray;
  private List versions;
  
  /** The URL for this vote block. */
  String url;
  
  /** The type of the poll (content, header, metadata).  Currently
   * only "Content" is implemented. */
  int voteType;

  /**
   * Construct a new VoteBlock with <em>versionCount</em> versions.
   * @param url The URL for this block.
   * @param voteType The type of the vote.
   */
  public VoteBlock(String url, int voteType) {
    this.versions = new ArrayList();
    this.url = url;
    this.voteType = voteType;
  }
  
  /**
   * Shortcut, since we are most commonly making content votes.
   * 
   * @param url The URL for this block.
   */
  public VoteBlock(String url) {
    this(url, VoteBlock.CONTENT_VOTE);
  }

  public VoteBlock(byte[] encodedForm) throws IOException {
    EncodedProperty props = new EncodedProperty();
    props.decode(encodedForm);
    this.url = props.getProperty(VB_URL);
    this.voteType = props.getInt(VB_VT, CONTENT_VOTE);
    List vers = props.getEncodedPropertyList(VB_VERSIONS);
    versions = new ArrayList(vers.size());
    for (Iterator iter = vers.iterator(); iter.hasNext(); ) {
      EncodedProperty vp = (EncodedProperty)iter.next();
      long unfilteredLength = vp.getLong(VB_ULEN, 0);
      long unfilteredOffset = vp.getLong(VB_UOFFSET, 0);
      long filteredLength = vp.getLong(VB_FLEN, 0);
      long filteredOffset = vp.getLong(VB_FOFFSET, 0);
      byte[] plainHash = vp.getByteArray(VB_PH, ByteArray.EMPTY_BYTE_ARRAY);
      byte[] noncedHash = vp.getByteArray(VB_NH, ByteArray.EMPTY_BYTE_ARRAY);
      boolean hashError = vp.getBoolean(VB_ERR, false);
      versions.add(new VoteBlock.Version(unfilteredOffset, unfilteredLength,
                                         filteredOffset, filteredLength,
                                         plainHash, noncedHash, hashError));
    }
  }
  
  public byte[] getEncoded() throws IOException {
    EncodedProperty props = new EncodedProperty();
    props.put(VB_URL, url);
    props.putInt(VB_VT, voteType);
    List<EncodedProperty> vers = new ArrayList<EncodedProperty>(versions.size());
    for (Iterator iter = versions.iterator(); iter.hasNext(); ) {
      EncodedProperty vp = new EncodedProperty();
      VoteBlock.Version ver = (VoteBlock.Version)iter.next();
      vp.putLong(VB_ULEN, ver.getUnfilteredLength());
      vp.putLong(VB_UOFFSET, ver.getUnfilteredOffset());
      vp.putLong(VB_FLEN, ver.getFilteredLength());
      vp.putLong(VB_FOFFSET, ver.getFilteredOffset());
      vp.putByteArray(VB_PH, ver.getPlainHash());
      vp.putByteArray(VB_NH, ver.getHash());
      vp.putBoolean(VB_ERR, ver.getHashError());
      vers.add(vp);
    }
    props.putEncodedPropertyList(VB_VERSIONS, vers);
    return props.encode();
  }
  
  /**
   * Return an iterator over the versions of this vote block.
   *
   * @return An iterator over the vote block's versions.
   */
  public Iterator versionIterator() {
    return versions.iterator();
  }

  /**
   * Returns the current version.
   * 
   * @return The most recent version, or null if there is none.
   */
  public VoteBlock.Version currentVersion() {
    if (versions.size() > 0) {
      return (VoteBlock.Version)versions.get(0);
    } else {
      return null;
    }
  }
  
  /**
   * Return the version at a specified index.
   * 
   * @return The nth version of the vote block, or null if there is
   *         no version.
   */
  public VoteBlock.Version getVersion(int ver) {
    if (ver >= 0 && ver < versions.size()) {
      return (VoteBlock.Version)versions.get(ver);
    } else {
      return null;
    }
  }
  
  /**
   * Return an array of all versions in this vote block.
   */
  public VoteBlock.Version[] getVersions() {
    return (VoteBlock.Version[])versions.toArray(new VoteBlock.Version[versions.size()]);
  }
  
  /**
   * Add the version to this vote block.
   * 
   * @param filteredLength The filtered length of the version.
   * @param filteredOffset The filtered offset of the version.
   * @param unfilteredLength The unfiltered length of the version.
   * @param unfilteredOffset The unfiltered offset of the version.
   * @param plainHash The plain hash of the content.
   * @param noncedHash The nonced hash of the content.
   * @param hashError True if there was an error while hashing for this version.
   */
  public void addVersion(long filteredOffset, long filteredLength,
                         long unfilteredOffset, long unfilteredLength,
                         byte[] plainHash, byte[] noncedHash,
                         boolean hashError) {
    versions.add(new VoteBlock.Version(filteredOffset, filteredLength,
                                       unfilteredOffset, unfilteredLength,
                                       plainHash, noncedHash, hashError));
  }
  
  public String getUrl() {
    return url;
  }
  
  public void setUrl(String url) {
    this.url = url;
  }
  
  public void setVoteType(int type) {
    this.voteType = type;
  }

  public int getVoteType() {
    return voteType;
  }

  public String getVoteTypeString() {
    if (voteType >= 0 && voteType < voteTypeStrings.length) {
      return voteTypeStrings[voteType];
    } else {
      return "Unknown";
    }
  }

  public int compareTo(VoteBlock otherVoteBlock) {
    String url1 = getUrl();
    String url2 = otherVoteBlock.getUrl();
    return compareUrls(url1, url2);
  }

  public static int compare(VoteBlock vb1, VoteBlock vb2) {
    return vb1.compareTo(vb2);
  }

  /**
   * A {@link Comparator} for the canonical ordering of URLs in {@link
   * VoteBlocks}. {@code null} is acceptable, and sorts after every
   * non-{@code null} URL.  non-{@code null} URL.
   * @param url1 First URL
   * @param url2 Second URL
   * @return less than 0 if url1 sorts before url2, greater than 0 if after
   */
  public static Comparator<String> URL_COMPARATOR = new Comparator<String>() {
    public int compare(String url1, String url2) {
      return StringUtil.preOrderCompareToNullHigh(url1, url2);
    }
  };
  
  /**
   * The canonical ordering of URLs in {@link VoteBlocks}. {@code null}
   * is acceptable, and sorts after every non-{@code null} URL.
   * @param url1 First URL
   * @param url2 Second URL
   * @return less than 0 if url1 sorts before url2, greater than 0 if after
   */
  public static int compareUrls(String url1, String url2) {
    return URL_COMPARATOR.compare(url1, url2);
  }

  public int size() {
    return versions.size();
  }
  
  public String toString() {
    StringBuffer sb = new StringBuffer("[VoteBlock:");
    sb.append(" " + getVoteTypeString());
    sb.append(", " + getUrl());
    sb.append(", " + size() + " version(s)]");
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

    if (vb.size() != size()) return false;
    
    boolean result = true;
    result &= StringUtil.equalStringsIgnoreCase(vb.url, url);
    result &= (vb.voteType == voteType);
    for (int i = 0; i < size(); i++) {
      result &= (vb.versions.get(i).equals(versions.get(i))); 
    }
    return result;
  }
  
  public int hashCode() {
    int result = 17;
    result = (int)(37 * result + url.hashCode());
    result = (int)(37 * result + voteType);
    for (int i = 0; i < size(); i++) {
      result = (int)(37 * result + versions.get(i).hashCode());
    }
    return result;
  }
  
  /**
   * Represents one version of a VoteBlock.
   */
  public static class Version implements LockssSerializable {
    private long filteredOffset = 0;
    private long filteredLength = 0;
    private long unfilteredOffset = 0;
    private long unfilteredLength = 0;
    private byte[] plainHash;
    private byte[] noncedHash;
    private boolean hashError = false;

    /**
     * Special constructor to create a VoteBlock from an EncodedProperty.
     *
     * @param encodedForm Encoded form for an EncodedProperty object.
     */
    public Version(byte[] encodedForm) throws IOException {
      EncodedProperty props = new EncodedProperty();
      props.decode(encodedForm);
      unfilteredOffset = props.getLong(VB_UOFFSET, unfilteredOffset);
      unfilteredLength = props.getLong(VB_ULEN, unfilteredLength);
      filteredOffset = props.getLong(VB_FOFFSET, filteredOffset);
      filteredLength = props.getLong(VB_FLEN, filteredLength);
      plainHash = props.getByteArray(VB_PH, ByteArray.EMPTY_BYTE_ARRAY);
      noncedHash = props.getByteArray(VB_NH, ByteArray.EMPTY_BYTE_ARRAY);
      hashError = props.getBoolean(VB_ERR, hashError);
    }

    public Version(long fOffset, long fLength,
                   long uOffset, long uLength,
                   byte[] plainHash, byte[] challengeHash, boolean hashError) {
      this.filteredOffset = fOffset;
      this.filteredLength = fLength;
      this.unfilteredOffset = uOffset;
      this.unfilteredLength = uLength;
      this.plainHash = plainHash;
      this.noncedHash = challengeHash;
      this.hashError = hashError;
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
      return noncedHash;
    }

    public void setChallengeHash(byte[] b) {
      this.noncedHash = b;
    }

    public byte[] getPlainHash() {
      return plainHash;
    }

    public void setPlainHash(byte[] b) {
      this.plainHash = b;
    }
    
    public boolean getHashError() {
      return hashError;
    }
    
    public void setHashError(boolean b) {
      this.hashError = b;
    }

    public String toString() {
      StringBuffer sb = new StringBuffer("[VoteBlock.Version: ");
      sb.append("fl = " + filteredLength + ", ");
      sb.append("fo = " + filteredOffset + ", ");
      sb.append("ul = " + unfilteredLength + ", ");
      sb.append("uo = " + unfilteredOffset + ", ");
      sb.append("nh = " +
                (noncedHash == null ? "null" : new String(B64Code.encode(noncedHash)))
                + ", ");
      sb.append("ph = " +
                (plainHash == null ? "null" : new String(B64Code.encode(plainHash)))
                + ", ");
      sb.append("err = " + (hashError ? "true" : "false") + "]");
      return sb.toString();
    }

    /*
     * Mostly as a convenience for testing, override equals and hashCode;
     */

    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }

      if (!(o instanceof VoteBlock.Version)) {
        return false;
      }

      VoteBlock.Version vb = (VoteBlock.Version)o;
      return vb.filteredLength == filteredLength &&
        vb.filteredOffset == filteredOffset &&
        vb.unfilteredLength == unfilteredLength &&
        vb.unfilteredOffset == unfilteredOffset &&
        Arrays.equals(vb.plainHash, plainHash) &&
        Arrays.equals(vb.noncedHash, noncedHash);
    }

    public int hashCode() {
      int result = 17;
      result = (int)(37 * result + filteredLength);
      result = (int)(37 * result + filteredOffset);
      result = (int)(37 * result + unfilteredLength);
      result = (int)(37 * result + unfilteredOffset);
      for (int i = 0; i < plainHash.length; i++) {
        result = 37 * result + plainHash[i];
      }
      for (int i = 0; i < noncedHash.length; i++) {
        result = 37 * result + noncedHash[i];
      }
      return result;
    }
  }
}
