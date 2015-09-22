/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.base;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.lockss.app.*;
import org.lockss.state.*;
import org.lockss.alert.*;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;
import org.lockss.repository.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.daemon.*;
import org.lockss.crawler.*;

/**
 * Basic, fully functional UrlCacher.  Utilizes the LockssRepository for
 * caching, and {@link LockssUrlConnection}s for fetching.  Plugins may
 * extend this to achieve, <i>eg</i>, specialized host connection or
 * authentication.  The redirection semantics offered here must be
 * preserved.
 */
public class DefaultUrlCacher implements UrlCacher {
  protected static Logger logger = Logger.getLogger(DefaultUrlCacher.class);

  /** The algorithm to use for content checksum calculation. 
   * An empty value disables checksums 
   */
  public static final String PARAM_CHECKSUM_ALGORITHM =
		    Configuration.PREFIX + "baseuc.checksumAlgorithm";
  public static final String DEFAULT_CHECKSUM_ALGORITHM = null;
  
  protected final ArchivalUnit au;
  protected final String origUrl;   // URL with which I was created
  protected String fetchUrl;		// possibly affected by redirects
  private List<String> redirectUrls;
  private final LockssRepository repository;
  private final CacheResultMap resultMap;
  private LockssWatchdog wdog;
  private BitSet fetchFlags = new BitSet();
  private InputStream input;
  private CIProperties headers;
  private boolean markLastContentChanged = true;
  private boolean alreadyHasContent;
  
  /**
   * Uncached url object and Archival Unit owner 
   * 
   * @param owner
   * @param uUrl
   */
  public DefaultUrlCacher(ArchivalUnit owner, UrlData ud) {
    if(ud.headers == null) {
      throw new NullPointerException(
          "Unable to store content with null headers");
    }
    origUrl = ud.url;
    headers = ud.headers;
    input = ud.input;
    au = owner;
    Plugin plugin = au.getPlugin();
    repository = plugin.getDaemon().getLockssRepository(au);
    resultMap = plugin.getCacheResultMap();
  }

  /**
   * Returns the original URL (the one the UrlCacher was created with),
   * independent of any redirects followed.
   * @return the url string
   */
  public String getUrl() {
    return origUrl;
  }

  /**
   * Return the URL that returned content
   */
  String getFetchUrl() {
    return fetchUrl != null ? fetchUrl : origUrl;
  }

  /**
   * Return the ArchivalUnit to which this UrlCacher belongs.
   * @return the owner ArchivalUnit
   */
  public ArchivalUnit getArchivalUnit() {
    return au;
  }

  /**
   * Return a CachedUrl for the content stored.  May be
   * called only after the content is completely written.
   * @return CachedUrl for the content stored.
   */
  public CachedUrl getCachedUrl() {
    return au.makeCachedUrl(getUrl());
  }

  public void setFetchFlags(BitSet fetchFlags) {
    this.fetchFlags = fetchFlags;
  }

  public BitSet getFetchFlags() {
    return fetchFlags;
  }

  public void setWatchdog(LockssWatchdog wdog) {
    this.wdog = wdog;
  }
  
  public LockssWatchdog getWatchdog() {
    return wdog;
  }
  
  public void setRedirectUrls(List<String> redirectUrls) {
    this.redirectUrls = redirectUrls;
    if(fetchUrl == null) {
      this.fetchUrl = redirectUrls.get(redirectUrls.size()-1);
    }
  }
  
  public void setFetchUrl(String fetchUrl) {
    this.fetchUrl = fetchUrl;
  }

  public void storeContent() throws IOException {
    storeContent(input, headers);
  }
  /** Store into the repository the content and headers from a successful
   * fetch.  If redirects were followed and
   * REDIRECT_OPTION_STORE_ALL was specified, store the content and
   * headers under each name in the chain of redirections.
   */
  public void storeContent(InputStream input, CIProperties headers)
      throws IOException {
    if(input != null) {
      Collection<String> startUrls = au.getStartUrls();
      if(startUrls != null && !startUrls.isEmpty() 
          && startUrls.contains(origUrl)) {
        markLastContentChanged = false;
      }
      if (logger.isDebug2()) logger.debug2("Storing url '"+ origUrl +"'");
      storeContentIn(origUrl, input, headers);
      if (logger.isDebug3()) {
        logger.debug3("redirectUrls: " + redirectUrls);
      }
      if (redirectUrls != null && fetchUrl != null) {
        CachedUrl cu = getCachedUrl();
        CIProperties headerCopy  = CIProperties.fromProperties(headers);
        int last = redirectUrls.size() - 1;
        for (int ix = 0; ix <= last; ix++) {
          String name = redirectUrls.get(ix);
          if (logger.isDebug2())
            logger.debug2("Storing in redirected-to url: " + name);
          InputStream is = cu.getUnfilteredInputStream();
          try {
            if (ix < last) {
              // this one was redirected, set its redirected-to prop to the
              // next in the list.
              headerCopy.setProperty(CachedUrl.PROPERTY_REDIRECTED_TO,
                  redirectUrls.get(ix + 1));
            } else if (!name.equals(fetchUrl)) {
              // Last in list.  If not same as fetchUrl, means the final
              // redirection was a directory(slash) redirection, which we don't
              // store as a different name or put on redirectUrls.  Indicate the
              // redirection to the slashed version.  The proxy must be aware
              // of this.  (It can't rely on this property being present,
              // becuase foo/ might later be fetched, not due to a redirect
              // from foo.)
              headerCopy.setProperty(CachedUrl.PROPERTY_REDIRECTED_TO, fetchUrl);
            } else {
              // This is the name that finally got fetched, don't store
              // redirect prop or content-url
              headerCopy.remove(CachedUrl.PROPERTY_REDIRECTED_TO);
              headerCopy.remove(CachedUrl.PROPERTY_CONTENT_URL);
            }
            storeContentIn(name, is, headerCopy);
          } finally {
            IOUtil.safeClose(is);
          }
        }
      }
    } else {
      logger.warning("Skipped storing a null input stream for " + origUrl);
    }
  }

  private boolean isCurrentVersionSuspect() {
    // Inefficient to call AuSuspectUrlVersions.isSuspect(url, version)
    // here as would have to find version number for each URL, which
    // require disk access.  This loop first filters on URL so finds
    // version number only when necessary.  Also, in some tests
    // getCachedUrl() will get NPE on other URLs due to MockArchivalUnit
    // not having been set up with a corresponding MockCachedUrl.

    Collection <AuSuspectUrlVersions.SuspectUrlVersion> suspects =
      AuUtil.getSuspectUrlVersions(au).getSuspectList();
    if (logger.isDebug2()) {
      logger.debug2("Checking for current suspect version: " + getUrl());
    }
    int curVer = -1;
    for (AuSuspectUrlVersions.SuspectUrlVersion suv : suspects ) {
      if (suv.getUrl().equals(getUrl())) {
	if (curVer == -1) {
	  curVer = getCachedUrl().getVersion();
	}
	if (suv.getVersion() == curVer) {
	  if (logger.isDebug3()) {
	    logger.debug3("Found suspect current version " +
			  curVer + ": " + getUrl());
	  }
	  return true;
	} else {
	  if (logger.isDebug3()) {
	    logger.debug3("Found suspect non-current version " +
			  suv.getVersion() + " != " + curVer + ": " + getUrl());
	  }
	}
      }
    }
    return false;
  }

  public void storeContentIn(String url, InputStream input,
			     CIProperties headers)
      throws IOException {
    RepositoryNode leaf = null;
    OutputStream os = null;
    boolean currentWasSuspect = isCurrentVersionSuspect();
    try {
      leaf = repository.createNewNode(url);
      alreadyHasContent = leaf.hasContent();
      leaf.makeNewVersion();
      
      MessageDigest checksumProducer = null;
      String checksumAlgorithm =
          CurrentConfig.getParam(PARAM_CHECKSUM_ALGORITHM,
              DEFAULT_CHECKSUM_ALGORITHM);
      if (!StringUtil.isNullString(checksumAlgorithm)) {
        try {
          checksumProducer = MessageDigest.getInstance(checksumAlgorithm);
        } catch (NoSuchAlgorithmException ex) {
          logger.warning(String.format("Checksum algorithm %s not found, "
              + "checksumming disabled", checksumAlgorithm));
        }
      }

      os = leaf.getNewOutputStream();
      long bytes =
          StreamUtil.copy(input, os, -1, wdog, true, checksumProducer);
      if (logger.isDebug3()) {
        logger.debug3("Stored " + bytes + " bytes in " + this);
      }
      if (!fetchFlags.get(DONT_CLOSE_INPUT_STREAM_FLAG)) {
        try {
          input.close();
        } catch (IOException ex) {
          CacheException closeEx =
            resultMap.mapException(au, fetchUrl, ex, null);
          if (!(closeEx instanceof CacheException.IgnoreCloseException)) {
            throw new StreamUtil.InputException(ex);
          }
        }
      }
      os.close();
      CacheException vExp = validate(bytes);
      if (vExp != null) {
	if (vExp instanceof CacheException.WarningOnly) {
	  infoException = vExp;
	} else {
	  throw vExp;
	}
      }
      headers.setProperty(CachedUrl.PROPERTY_NODE_URL, url);
      if (checksumProducer != null) {
        byte bdigest[] = checksumProducer.digest();
        String sdigest = ByteArray.toHexString(bdigest);
        headers.setProperty(CachedUrl.PROPERTY_CHECKSUM,
		    String.format("%s:%s", checksumAlgorithm, sdigest));
      }
      leaf.setNewProperties(headers);
      leaf.sealNewVersion();
      AuState aus = AuUtil.getAuState(au);
      if (aus != null && currentWasSuspect) {
	aus.incrementNumCurrentSuspectVersions(-1);
      }
      if (aus != null && markLastContentChanged) {
        aus.contentChanged();
      }
      if (alreadyHasContent) {
	Alert alert = Alert.auAlert(Alert.NEW_FILE_VERSION, au);
	alert.setAttribute(Alert.ATTR_URL, getFetchUrl());
	String msg = "Collected an edditional version: " + getFetchUrl();
	alert.setAttribute(Alert.ATTR_TEXT, msg);
	raiseAlert(alert);
      }
    } catch (StreamUtil.OutputException ex) {
      if (leaf != null) {
        try {
          leaf.abandonNewVersion();
        } catch (Exception e) {
          // just being paranoid
        }
      }
      throw resultMap.getRepositoryException(ex.getIOCause());
    } catch (IOException ex) {
      logger.debug("storeContentIn1", ex);
      if (leaf != null) {
        try {
          leaf.abandonNewVersion();
        } catch (Exception e) {
          // just being paranoid
        }
      }
      // XXX some code below here maps the exception
      throw ex instanceof CacheException
	? ex : resultMap.mapException(au, url, ex, null);
    } finally {
      IOUtil.safeClose(os);
    }
  }
  
  /**
   * Overrides normal <code>toString()</code> to return a string like
   * "BUC: <url>"
   * @return the class-url string
   */
  public String toString() {
    return "[BUC: " + getUrl() + "]";
  }

  //  Beginnings of validation framework.
  protected CacheException infoException;

  public CacheException getInfoException() {
    return infoException;
  }

  // XXX need to make it possible for validator to access CU before seal(),
  // so it can prevent file from being committed.
  protected CacheException validate(long size) throws CacheException {
    LinkedList<Exception> validationFailures = new LinkedList<Exception>();
    long contLen = getContentLength();
    if (contLen >= 0 && contLen != size) {
      Alert alert = Alert.auAlert(Alert.FILE_VERIFICATION, au);
      alert.setAttribute(Alert.ATTR_URL, getFetchUrl());
      String msg = "File size (" + size +
	") differs from Content-Length header (" + contLen + "): "
	+ getFetchUrl();
      alert.setAttribute(Alert.ATTR_TEXT, msg);
      raiseAlert(alert);
      validationFailures.add(new ContentValidationException.WrongLength(msg));
    }
//     try {
      if (size == 0) {
        Exception ex =
            new ContentValidationException.EmptyFile("Empty file stored");
	validationFailures.addFirst(ex);
      }
      return firstMappedException(validationFailures);
//     } catch (Exception e) {
//       throw resultMap.mapException(au, conn, e, null);
//     }
  }


  private CacheException firstMappedException(List<Exception> exps) {
    for (Exception ex : exps) {
      CacheException mapped = resultMap.mapException(au, fetchUrl, ex, null);
      if (mapped != null) {
	return mapped;
      }
    }
    return null;
  }


  private void raiseAlert(Alert alert) {
    try {
      au.getPlugin().getDaemon().getAlertManager().raiseAlert(alert);
    } catch (RuntimeException e) {
      logger.error("Couldn't raise alert", e);
    }
  }

  public long getContentLength() {
    try {
      return Long.parseLong(headers.getProperty("content-length"));
    } catch (Exception e) {
      return -1;
    }
  }  
}
