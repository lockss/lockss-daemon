/*
 * $Id$
 */

/*

Copyright (c) 2000-2019 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.codeocean;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lockss.config.TdbAu;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.BaseFileArticleMetadataExtractor;

/*
 * Customize the ArticleMetadata class for generic file objects for Code Ocean
 * code modules
 */

public class CodeOceanArticleMetadataExtractor extends BaseFileArticleMetadataExtractor{
	
	private static final String CODE_OCEAN_FILE_TYPE = "code module";
    private static final String CODE_OCEAN = "Code Ocean";
    private static final Pattern  ZIP_PAT = Pattern.compile("/code-ocean-released/([^/]+)/([^/]+)/([^/]+)\\.zip", Pattern.CASE_INSENSITIVE);  
	private static final int YEAR_GROUP = 1;
	private static final int PUB_GROUP = 2;
	
	public CodeOceanArticleMetadataExtractor(String role) {
		super(role);
	}

	@Override
	protected String getContentYear(CachedUrl cu, TdbAu tdbau) {
		// Get limited information from the TDB file
		String defYr = super.getContentYear(cu,tdbau);
		if (defYr == null) {
			Matcher umat = ZIP_PAT.matcher(cu.getUrl());
			if (umat.find()) {
				return umat.group(YEAR_GROUP);
			}
		}
		return defYr;
	}

	/*
	 * Code Ocean is actually the provider, serving code modules
	 * for multiple publishers. 
	 * If for some reason not set, use the publisher name in the url
	 */
	@Override
	protected String getContentPublisher(CachedUrl cu, TdbAu tdbau) {
		String defPub = super.getContentPublisher(cu, tdbau);
		if (defPub == null) {
			Matcher umat = ZIP_PAT.matcher(cu.getUrl());
			if (umat.find()) {
				return umat.group(PUB_GROUP);
			}
		}
		return defPub;
	}

	@Override
	protected String getContentProvider(CachedUrl cu, TdbAu tdbau, String publisher) {
		return (tdbau != null) ? tdbau.getProviderName() : CODE_OCEAN;
	}

	/*
	 * Code Ocean is actually the provider and provides
	 * code modules for multliple publishers
	 */
	

	@Override
	protected String getFileObjectType(CachedUrl cu) {
		return CODE_OCEAN_FILE_TYPE;
	}	

}