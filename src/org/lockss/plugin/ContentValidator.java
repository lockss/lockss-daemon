/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import org.lockss.daemon.*;

/** Validate a received file; determine whether to accept or reject it.  To
 * accept, just return.  To reject or issue a warning, throw a
 * ContentValidationException or subclass.  The CacheException to which
 * this exception is mapped, by default or by plugin-specified mapping,
 * determines the action taken.  If the CacheException's ATTRIBUTE_FAIL or
 * ATTRIBUTE_FATAL is set, the file will not be stored and the crawl will
 * fail (when it completes, or, for ATTRIBUTE_FATAL, immediately).  If
 * neither attribute is set a warning will be issued; if ATTRIBUTE_NO_STORE
 * is set the file will not be stored, otherwise it will be stored.
 */
public interface ContentValidator {

  /** Validate a received file.  Examine the headers and/or content of the
   * CachedUrl and return if acceptable or throw a
   * ContentValidationException or subclass if not.  The CachedUrl need not
   * be released - the caller does so.<br>The action taken depends on the
   * attributes of the {@link org.lockss.util.urlconn.CacheException} to
   * which the thrown ContentValidationException is mapped.  The default
   * mappings are:<ul>
   *
   * <li>ContentValidationException -> UnretryableException.  (The URL is
   * recorded as a failed fatch and not stored)</li>
   *
   * <li>ContentValidationException.EmptyFile -> WarningOnly.  (A warning
   * is reported for the URL but the URL is stored and the crawl may still
   * succeed)</li>
   *
   * <li>ContentValidationException.WrongLength ->
   * RetryableNetworkException_3_10S.  (The fetch will be retried up to
   * three times at 10 second intervals )</li>
   *
   * </ul> Plugins may change these mappings, or define additional
   * subclasses of ContentValidationException and map them to
   * CacheExceptions, using <tt>plugin_cache_result_list</tt>.
   *
   * @param cu the CachedUrl to validate.  If a redirect occurred, the
   * CachedUrl argument reflects the original URL, the response headers are
   * those from the final, non-redirect response, and the list of
   * redirected-to URLs (not including the original URL) is in the
   * CachedUrl.PROPERTY_VALIDATOR_REDIRECT_URLS ({@value
   * org.lockss.plugin.CachedUrl#PROPERTY_VALIDATOR_REDIRECT_URLS})
   * property.  (This is a List, which must be retrieved using Map.get(),
   * not Properties.getProperty()).
   * @throws ContentValidationException 
   * @throws IOException propagated from reading cu, etc.
   * @throws PluginException 
   */
  public void validate(CachedUrl cu)
      throws ContentValidationException, PluginException, IOException;
}
