/*

Copyright (c) 2021-2023 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.laaws;
import java.io.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.rewriter.*;
import org.lockss.extractor.*;

/** A CU that should be treated as if it had the URL that V2 would use.
 * Used for hashing and migrator. */
public class V2CompatCachedUrl implements CachedUrl {
  CachedUrl cu;
  String v2Url;

  public V2CompatCachedUrl(CachedUrl cu, String v2Url) {
    this.cu = cu;
    this.v2Url = v2Url;
  }

  /** Return the V2-compatible URL */
  public String getUrl() {
    return v2Url;
  }
  public CachedUrl getCuVersion(int version) {
    return cu.getCuVersion(version);
  }
  public CachedUrl[] getCuVersions() {
    return cu.getCuVersions();
  }
  public CachedUrl[] getCuVersions(int maxVersions) {
    return cu.getCuVersions(maxVersions);
  }
  public int getVersion() {
    return cu.getVersion();
  }
  public void delete() throws UnsupportedOperationException, IOException {
    cu.delete();
  }
  public void setOption(String option, String val) {
    cu.setOption(option, val);
  }
  public InputStream getUnfilteredInputStream() {
    return cu.getUnfilteredInputStream();
  }
  public InputStream getUnfilteredInputStream(HashedInputStream.Hasher hasher) {
    return cu.getUnfilteredInputStream(hasher);
  }
  public InputStream getUncompressedInputStream() {
    return cu.getUncompressedInputStream();
  }
  public InputStream getUncompressedInputStream(HashedInputStream.Hasher
                                                hasher) {
    return cu.getUncompressedInputStream(hasher);
  }
  public InputStream openForHashing() {
    return cu.openForHashing();
  }
  public InputStream openForHashing(HashedInputStream.Hasher hasher) {
    return cu.openForHashing(hasher);
  }
  public Reader openForReading() {
    return cu.openForReading();
  }
  public LinkRewriterFactory getLinkRewriterFactory() {
    return cu.getLinkRewriterFactory();
  }
  public CIProperties getProperties() {
    return cu.getProperties();
  }
  public void addProperty(String key, String value) {
    cu.addProperty(key, value);
  }
  public long getContentSize() {
    return cu.getContentSize();
  }
  public String getContentType() {
    return cu.getContentType();
  }
  public String getEncoding() {
    return cu.getEncoding();
  }
  public ArchivalUnit getArchivalUnit() {
    return cu.getArchivalUnit();
  }
  public void release() {
    cu.release();
  }
  public FileMetadataExtractor getFileMetadataExtractor(MetadataTarget target) {
    return cu.getFileMetadataExtractor(target);
  }
  public CachedUrl getArchiveMemberCu(ArchiveMemberSpec ams) {
    return cu.getArchiveMemberCu(ams);
  }
  public boolean isArchiveMember() {
    return cu.isArchiveMember();
  }
  public boolean isLeaf() {
    return cu.isLeaf();
  }
  public boolean hasContent() {
    return cu.hasContent();
  }
  public int getType() {
    return cu.getType();
  }
  public String toString() {
    return "[V2CU: " + getUrl() + ": " + cu + "]";
  }
}
