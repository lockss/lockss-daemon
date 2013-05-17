/*
 * $Id: LocalHasher.java,v 1.1.2.1 2013-05-17 03:35:36 dshr Exp $
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

package org.lockss.hasher;

import java.io.*;
import java.security.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.daemon.*;
import org.lockss.config.*;

/** Hash a CachedUrlSet or an AU and, if a URL has a pre-existing hash
 * stored in the properties, compare them and flag any mis-matches. If
 * there is no hash, store one in the properties.
 */
public class LocalHasher {
  protected static Logger logger = Logger.getLogger("LocalHasher");

  private String checksumAlgorithm = null;

  private int nbytes = 1000;
  private long bytesHashed = 0;
  private int filesHashed = 0;
  private long startTime = 0;
  private long elapsedTime;

  public LocalHasher() {
    checksumAlgorithm =
      CurrentConfig.getParam(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
			     BaseUrlCacher.DEFAULT_CHECKSUM_ALGORITHM);
    startTime = System.currentTimeMillis();
  }

  public long getBytesHashed() {
    return bytesHashed;
  }

  public int getFilesHashed() {
    return filesHashed;
  }

  public long getElapsedTime() {
    return elapsedTime;
  }

  /** Do a local hash of the AU, creating or checking local hashes */
  public void doLocalHash(ArchivalUnit au)
      throws IOException {
    doLocalHash(au.getAuCachedUrlSet());
  }

  /** Do a local hash of the CUS, creating or checking local hashes */
  public void doLocalHash(CachedUrlSet cus) throws IOException {
    Iterator iterCUS = cus.contentHashIterator();
    while (iterCUS.hasNext()) {
      CachedUrlSetNode node = (CachedUrlSetNode)iterCUS.next();
      switch (node.getType()) {
      case CachedUrlSetNode.TYPE_CACHED_URL:
	if (node.hasContent()) {
	  CachedUrl cu = (CachedUrl) node;
	  doLocalHashNode(cu);
	} else {
	  doLocalEmptyNode((CachedUrl) node);
	}
	elapsedTime = System.currentTimeMillis() - startTime;
	break;
      case CachedUrlSetNode.TYPE_CACHED_URL_SET:
	doLocalHash((CachedUrlSet) node);
	break;
      default:
	throw new IllegalArgumentException("Bad CachedUrlSetNode type: "
					   + node.getType());
      }
    }
  }

  void doLocalEmptyNode(CachedUrl cu) {
    CIProperties props = cu.getProperties();
    if (props.containsKey(CachedUrl.PROPERTY_CHECKSUM)) {
      String checksum = (String) props.get(CachedUrl.PROPERTY_CHECKSUM);
      logger.error(cu.getUrl() + " no content but a checksum " + checksum);
    }
  }

  void doLocalHashNode(CachedUrl cu) throws IOException {
    CachedUrl[] versions;
    try {
      versions = cu.getCuVersions();
      for (int i = 0; i < versions.length; i++) {
	doLocalHashVersion(versions[i]);
      }
    } catch (UnsupportedOperationException ex) {
      logger.error("LocalHash: " + cu.getUrl() + " has no versions");
    }
  }

  void doLocalHashVersion(CachedUrl cu) throws IOException {
    MessageDigest checksumProducer = makeDigest(checksumAlgorithm);
    if (checksumProducer != null) {
      // Hash the content of this version
      doLocalHashVersionContent(cu, checksumProducer);
    } else {
      logger.error("LocalHash: " + checksumAlgorithm + " not available");
    }
  }

  void doLocalHashVersionContent(CachedUrl cu, MessageDigest digest)
    throws IOException {
    CIProperties props = cu.getProperties();
    // Hash the CachedUrl with the digest
    String checksumComputed = localHashVersionContent(cu, digest);
    // Check the checksum property
    if (props.containsKey(CachedUrl.PROPERTY_CHECKSUM)) {
      // Compare the hash we just did with the one in the props
      String checksumProperty = (String) props.get(CachedUrl.PROPERTY_CHECKSUM);
      if (checksumProperty.startsWith("FAIL:")) {
	logger.error("Hash FAIL: " + cu.getUrl() + " v: " + cu.getVersion() +
		  " " + checksumProperty + " != " + checksumComputed);
      } else if (checksumProperty.startsWith(checksumAlgorithm)) {
	// same algorithm
	if (checksumProperty.endsWith(checksumComputed)) {
	  // Content OK
	  logger.debug2("Hash OK: " + cu.getUrl() + " v: " + cu.getVersion());
	} else {
	  // Content not OK
	  logger.error("Hash BAD: " + cu.getUrl() + " v: " + cu.getVersion() +
		    " " + checksumProperty + " != " + checksumComputed);
	  props.put(CachedUrl.PROPERTY_CHECKSUM,
		    String.format("FAIL:%s:%s",
				  checksumAlgorithm,checksumComputed));
	}
      } else {
	int colon = checksumProperty.indexOf(':');
	if (colon < 0) {
	  throw new IllegalArgumentException("No colon in " +
					     checksumProperty);
	}
	String oldAlgorithm = checksumProperty.substring(colon);
	// Algorithm mis-match - re-compute with other algorithm
	if ((digest = makeDigest(oldAlgorithm)) != null ) {
	  String oldChecksum = localHashVersionContent(cu, digest);
	  if (checksumProperty.endsWith(oldChecksum)) {
	    // Content OK - update hash
	    props.put(CachedUrl.PROPERTY_CHECKSUM,
		      String.format("%s:%s",
				    checksumAlgorithm,checksumComputed));
	  } else {
	    // Content not OK
	    logger.error("Hash BAD: " + cu.getUrl() + " v: " + cu.getVersion() +
		      " " + checksumProperty + " != " + oldChecksum);
	    props.put(CachedUrl.PROPERTY_CHECKSUM,
		      String.format("FAIL:%s:%s",
				    checksumAlgorithm,checksumComputed));
	  }
	} else {
	  logger.error("Old algorithm not supported: " + oldAlgorithm);
	}
      }
    } else {
      // No checksum in props - put it in
      // XXX should only do this when winning PoR poll
      props.put(CachedUrl.PROPERTY_CHECKSUM,
		String.format("%s:%s", checksumAlgorithm,checksumComputed));
    }
  }

  String localHashVersionContent(CachedUrl cu, MessageDigest digest)
    throws IOException {
    InputStream is = cu.getUnfilteredInputStream();
    // XXX don't need to copy but no util to just hash a InputStream
    File tempFile = FileUtil.createTempFile("LocalHash", ".tmp");
    OutputStream os = new FileOutputStream(tempFile);
    bytesHashed += StreamUtil.copy(is, os, -1, null, true, digest);
    filesHashed++;
    is.close();
    os.close();
    tempFile.delete();
    return ByteArray.toHexString(digest.digest());
  }

  MessageDigest makeDigest(String checksumAlgorithm) {
    // Create digester
    MessageDigest checksumProducer = null;
    if (!StringUtil.isNullString(checksumAlgorithm)) {
      try {
	checksumProducer = MessageDigest.getInstance(checksumAlgorithm);
      } catch (NoSuchAlgorithmException ex) {
	logger.warning(String.format("Checksum algorithm %s not found, checksuming disabled", checksumAlgorithm));
      }
    } else {
      throw new IllegalArgumentException("Null checksum algorithm");
    }
    return checksumProducer;
  }
}
