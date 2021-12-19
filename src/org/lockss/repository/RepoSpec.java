/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.repository;

import java.util.regex.*;

import org.lockss.util.*;

/** Information about a V2 LOCKSS repository: the spec string, parsed into
 * components, and the LockssRepository instance itself.
 *
 * The spec is of the form
 * <tt><i>type</i>:<i>collection</i>[:<i>path-or-url</i>]</tt>, where
 * <i>type</i> is one of <tt>volatile</tt>, <tt>local</tt>, or
 * <tt>rest</tt>.  <tt>local</tt> requires a <i>path</i>; <tt>rest</tt>
 * requires a <i>url</i>.
 */

public class RepoSpec {
  static Logger log = Logger.getLogger(RepoSpec.class);

  private String spec;			// Canonical version of orig string
  private String type;			// local, volatile, rest
  private String path;			// URL or local path
  private String collection;		// collection name

  private RepoSpec(String spec,
		   String type,
		   String collection) {
    this.spec = spec;
    this.type = type;
    this.collection = collection;
  }

  private RepoSpec(String spec,
		   String type,
		   String collection,
		   String path) {
    this.spec = spec;
    this.type = type;
    this.collection = collection;
    this.path = path;
  }

  /** Return the original spec string */
  public String getSpec() {
    return spec;
  }

  /** Return the repository type (volatile, local, rest) */
  public String getType() {
    return type;
  }

  /** Return the local repository path */
  public String getPath() {
    return path;
  }

  /** Return the rest repository url */
  public String getUrl() {
    return path;
  }

  /** Return the collection name */
  public String getCollection() {
    return collection;
  }

  public String toString() {
    return spec;
  }

  static Pattern REPO_SPEC_PATTERN =
    Pattern.compile("([^:]+):([^:]+)(?::(.*$))?");

  /** Parse the spec string and return a RepoSpec respresenting it and its
   * parts */
  public static RepoSpec fromSpec(String spec) {
    Matcher m1 = REPO_SPEC_PATTERN.matcher(spec);
    if (m1.matches()) {
      String coll = m1.group(2);
      if (StringUtil.isNullString(coll)) {
	throw new IllegalArgumentException("Illegal V2 repository spec; no collection: " + spec);
      } else {
	String type = m1.group(1);
	switch (type) {
	case "volatile":
	  return new RepoSpec(spec, type, coll);
	case "local":
	  String path = m1.group(3);
	  if (StringUtil.isNullString(path)) {
	    throw new IllegalArgumentException("Illegal V2 repository spec; no path: " + spec);
	  }
	  return new RepoSpec(spec, type, coll, path);
	case "rest":
	  String url = m1.group(3);
	  if (StringUtil.isNullString(url)) {
	    throw new IllegalArgumentException("Illegal V2 repository spec; no URL: " + spec);
	  }
	  return new RepoSpec(spec, type, coll, url);
	default:
	  throw new IllegalArgumentException("Illegal V2 repository spec; unknown type: " + spec);
	}
      }
    }
    throw new IllegalArgumentException("Illegal V2 repository spec; couldn't parse: " + spec);
  }
}

