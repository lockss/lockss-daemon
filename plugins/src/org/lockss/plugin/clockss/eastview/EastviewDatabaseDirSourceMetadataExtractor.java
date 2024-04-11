/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.clockss.eastview;

import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.MetadataField;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.BaseFileArticleMetadataExtractor;
import org.lockss.util.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * /eastviewudbcom-released/2024_01/eastview/UDB-COM/LGA/1998.zip
 */

public class EastviewDatabaseDirSourceMetadataExtractor extends BaseFileArticleMetadataExtractor{

	/*
	 * code ocean delivers capsules under publisher subdirectories - map the publisher path to a specifc publisher
	 */

	private static Logger log =
			Logger.getLogger(EastviewDatabaseDirSourceMetadataExtractor.class);

	private static final String UDBCOM_FILE_TYPE = "file";
	private static final String UDBCOM = "Eastview";
	private static final Pattern  ZIP_PAT = Pattern.compile("/eastviewudbcom-released/([^_]+)_([^_]+)/.*\\.zip", Pattern.CASE_INSENSITIVE);

	private static final int YEAR_GROUP = 1;


	public EastviewDatabaseDirSourceMetadataExtractor(String role) {
		super(role);
	}

	
	/*
	 *  CodeOcean has a file item "capsule.zip" that is the item we
	 *  store characteristics about but additional metadata is extracted
	 *  out of a preservation.yml file that is a sibling of capsule.zip
	 */
	protected CachedUrl getFileUrl(ArticleFiles af) {
		CachedUrl fcu = af.getRoleCu("FileItem");
		if (fcu == null) {
			return super.getFileUrl(af);
		}
		return fcu;
	}


    /*
     * File item is capsule.zip
     * Metadata from preservation.yml
     * But check in case that file was missing, we don't want to extract
     * any additional information so just return null.
     */
	protected CachedUrl getMetadataUrl(ArticleFiles af) {
		CachedUrl mdcu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA); 
		if((mdcu != null) && mdcu.getUrl().endsWith(".yml")) {
			return mdcu;
		}
		return null;
	}	
	/*
	 * 
	 * CodeOcean is a little different from the other FILE object items.
	 * CodeOcean provides *some* metadata in a preservation.yml file that lives as a sibling of the actual 
	 * item it describes "capsule.zip". 
	 * Override the main File metadata method setFileMetadata
	 */

	/*
	 * The AM should have the date from preservation.yml file
 	 * If not, fail over to deposit date. 
	 */
	@Override
	protected String getContentYear(CachedUrl cu, ArticleMetadata am) {
		
		if ((am != null) && am.get(MetadataField.FIELD_DATE) != null) {
			return am.get(MetadataField.FIELD_DATE);
		}
		String defYr = super.getContentYear(cu,am);
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
	protected String getContentPublisher(CachedUrl cu, ArticleMetadata am) {
		return super.getContentPublisher(cu,am);
	}


	/*
	 * Code Ocean is actually the provider and provides
	 * code modules for multliple publishers
	 */ 
	@Override
	protected String getContentProvider(CachedUrl cu, ArticleMetadata am) {
		return UDBCOM;
	}


	@Override
	protected String getFileObjectType(CachedUrl cu) {
		return UDBCOM_FILE_TYPE;
	}

}