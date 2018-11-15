/*
 * $Id$
 */

/*

Copyright (c) 2000-2018 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.HeaderUtil;
import org.lockss.util.Logger;
import org.lockss.util.PatternStringMap;

/**
 * <p>
 * This ContentValidator is to allow validation of mime type based on the URL pattern.
 * The plugin can define those patters for which they want mime type validation and map them to the 
 * expected mime type for each pattern.
 * Default is that this validator simply logs a warning. The plugin can override that and set
 * any of the other validation exceptions to be used for a mismatch.
 * </p>
 * 
 * @author Alexandra Ohlson
 * @since 1.74
 * @see MimeTypeContentValidotarFactory
 */
public class MimeTypeContentValidator implements ContentValidator {
	private static final Logger log = Logger.getLogger(MimeTypeContentValidator.class);

	protected ContentValidationException exception;
	protected ArchivalUnit au;
	protected String contentType; //not used - provided by factory
    // Cached here as might be used several times in quick succession
	protected PatternStringMap urlMimeValidationMap = null;

	public MimeTypeContentValidator(ArchivalUnit au, String contentType) {
		this.exception = new ContentValidationException.LogOnly("URL Mime Type Mismatch");
		this.au = au;
		this.contentType = contentType; //don't think we need this for this implementation
	}
	
	PatternStringMap getUrlMimeValidationMap() {
	   if (urlMimeValidationMap == null) {
	     urlMimeValidationMap = au.makeUrlMimeValidationMap();
	   }
	   // will be EMPTY not null after creation attempt
	   return urlMimeValidationMap;
	 }

	@Override
	public void validate(CachedUrl cu)
			throws ContentValidationException, PluginException, IOException
	{
		if (cu != null) {
			String url = cu.getUrl();
			String expectedMime = getUrlMimeValidationMap().getMatch(url);
			if (expectedMime != null) {
				String actualMime = null;
				try {
					// this requires a close
					actualMime =
							HeaderUtil.getMimeTypeFromContentType(cu.getContentType());
					log.debug2("Expected mime type for: " +
						   url + ": " + expectedMime +
						   ", was: " + actualMime);
				} catch (Exception e) {
				  log.error("Checking mime type of: " + url, e);
				} finally {
					AuUtil.safeRelease(cu);
				}
				if (!expectedMatchesActualMime(expectedMime,actualMime,cu)) {
					throw getMimeTypeException();
				}

			}
		}
	}

	// default is a straight case-insensitive string comparison - we know expected is not null
	// plugin could override this for a more complicated equivalence, which is why the cu is passed along
	protected boolean expectedMatchesActualMime(String expectedMime, String actualMime, CachedUrl cu) {
		return expectedMime.equalsIgnoreCase(actualMime);
	}

	protected ContentValidationException getMimeTypeException()
	{
		return this.exception;
	}

	// plugin could override this to use a different type of exception
	protected void setMimeTypeException(ContentValidationException exc)
	{
		this.exception = exc;
	}

}
