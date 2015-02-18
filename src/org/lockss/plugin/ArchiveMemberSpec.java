/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import org.lockss.util.*;

  /** Specifies which member of an archive file to access.  Currently a
   * single directory path, possibly referencing nested archives. */
public class ArchiveMemberSpec {
  /** The character sequence that separates the URL of a cached file from
   * the member name to access */
  public static final String URL_SEPARATOR = "!/";

  private String url;
  private String member;

  private ArchiveMemberSpec(String url, String memberName) {
    this.url = url;
    this.member = memberName;
  }
    
  /** If the URL refers to an archive member, return a ArchiveMemberSpec
   * containing the archive URL and member name, else null.  Should be
   * called only if it's known that the AU wants archive member processing.
   * @param url
   * @return a ArchiveMemberSpec or null.
   */
  private static ArchiveMemberSpec fromUrl(String url) {
    int pos = url.indexOf(URL_SEPARATOR);
    if (pos > 0) {
      return new ArchiveMemberSpec(url.substring(0, pos),
				   url.substring(pos + URL_SEPARATOR.length()));
    }      
    return null;
  }

  /** If the URL refers to an archive member, return a ArchiveMemberSpec
   * containing the archive URL and member name, else null.
   * @param url
   * @return a ArchiveMemberSpec or null.
   */
  public static ArchiveMemberSpec fromCu(CachedUrl cu, String memberName) {
    return new ArchiveMemberSpec(cu.getUrl(), memberName);
  }

  /** If the URL refers to an archive member, return a ArchiveMemberSpec
   * containing the archive URL and member name, else null.
   * @param url
   * @return a ArchiveMemberSpec or null.
   */
  public static ArchiveMemberSpec fromUrl(ArchivalUnit au, String url) {
    ArchiveFileTypes aft = au.getArchiveFileTypes();
    if (aft == null) {
      return null;
    }
    return ArchiveMemberSpec.fromUrl(url);
  }

  /** Return the URL */
  public String getUrl() {
    return url;
  }

  /** Return the member name */
  public String getName() {
    return member;
  }

  public String toUrl() {
    return url + URL_SEPARATOR + member;
  }

  public String toString() {
    return url + URL_SEPARATOR + member;
  }
}
