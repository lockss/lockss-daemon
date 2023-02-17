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

package org.lockss.plugin.clockss.codeocean;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.MetadataField;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.BaseFileArticleMetadataExtractor;
import org.lockss.util.Logger;

/*
 * Customize the ArticleMetadata class for generic file objects for Code Ocean
 * code modules
 * 
 * The files inside a specific 
 * code-ocean-released/2019/nature/LONGUUID/v1.0/
 *     capsule.zip  - code capsule, data
 *     results.zip - 
 *     image.tar.xz - compressed docker image
 *     extract.sh - extraction script should anything be needed
 *     prervation.yml - metadata for CLOCKSS purposes - do not use capsule.zip!/metadata.yml
 * code-ocean-released/2019/nature/4aaa25ae-2fb9-49fe-8379-7deb6bfb80e9/v1.0/capsule.zip
 * code-ocean-released/2019/nature/4aaa25ae-2fb9-49fe-8379-7deb6bfb80e9/v1.0/extract.sh
 * code-ocean-released/2019/nature/4aaa25ae-2fb9-49fe-8379-7deb6bfb80e9/v1.0/image.tar.xz
 * code-ocean-released/2019/nature/4aaa25ae-2fb9-49fe-8379-7deb6bfb80e9/v1.0/preservation.yml
 * code-ocean-released/2019/nature/4aaa25ae-2fb9-49fe-8379-7deb6bfb80e9/v1.0/results.zip
 * 
 * The metadata object is preservation.yml but the access.url and size and type are for capsule.zip
 * Use capsule.zip as the metadata object and then do filename substitution for extracting the details
 */

public class CodeOceanArticleMetadataExtractor extends BaseFileArticleMetadataExtractor{
	
	/*
	 * code ocean delivers capsules under publisher subdirectories - map the publisher path to a specifc publisher
	 */

	private static Logger log = 
			Logger.getLogger(CodeOceanArticleMetadataExtractor.class);
	
	static private final Map<String, String> PublisherIDMap =
			new HashMap<String,String>();
	static {
		PublisherIDMap.put("nature", "Springer");
		PublisherIDMap.put("bmc", "Springer");
		PublisherIDMap.put("taylorandfrancis", "Taylor & Francis");
		PublisherIDMap.put("cambridge", "Cambridge University Press");
	}
	private static final String CODE_OCEAN_FILE_TYPE = "code capsule";
	private static final String CODE_OCEAN = "Code Ocean";
	private static final Pattern  ZIP_PAT = Pattern.compile("/code-ocean-released/([^/]+)/([^/]+)/([^/]+)/[vV]([^/]+)/capsule\\.zip", Pattern.CASE_INSENSITIVE);
	/* regex group1 = ingest year, group2 = publisher, group3 = identifier, group4= version number */
	private static final int YEAR_GROUP = 1;
	private static final int PUB_GROUP = 2;
	private static final int UUID_GROUP = 3;
	private static final int VERSION_GROUP = 4;

	
	public CodeOceanArticleMetadataExtractor(String role) {
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
		// if not there, use the 
    	Matcher umat = ZIP_PAT.matcher(cu.getUrl());
		if (umat.find()) {
			String pubkey = umat.group(PUB_GROUP);
			if (PublisherIDMap.containsKey(pubkey)) {
					return PublisherIDMap.get(pubkey);
			}
			return pubkey;
		}
		// last chance
		return super.getContentPublisher(cu,am);
	}
	
	@Override
	protected String getFileIdentifier(CachedUrl cu) {
		Matcher umat = ZIP_PAT.matcher(cu.getUrl());
		if (umat.find()) {
			return umat.group(UUID_GROUP);
		}
		return super.getFileIdentifier(cu); // not useful, just capsule.zip
	}

	/*
	 * Code Ocean is actually the provider and provides
	 * code modules for multliple publishers
	 */ 
	@Override
	protected String getContentProvider(CachedUrl cu, ArticleMetadata am) {
		return CODE_OCEAN;
	}


	@Override
	protected String getFileObjectType(CachedUrl cu) {
		return CODE_OCEAN_FILE_TYPE;
	}
	
	
	/*
	 * The following are unique to Code Ocean and should only be queries on a FILE object
	 * that has type of CODE_OCEAN_FILE_TYPE and provider = CODE_OCEAN
	 * 
	 * CODE OCEAN provides information about the DOI of the article the code capsule
	 * is associated with if it's associated with a published article. This might be null
	 * if it is associated with unpublished graduate research
	 */

	@Override
	protected void setAdditionalArticleMetadata(CachedUrl cu, ArticleMetadata am) {
		Map<String,String> file_map = am.getRawMap(MetadataField.FIELD_MD_MAP.getKey());
		String defV = "1.0";
		Matcher umat = ZIP_PAT.matcher(cu.getUrl());
		if (umat.find()) {
			defV = umat.group(VERSION_GROUP);
		}
		file_map.put("CapsuleVersion", defV);
		if (am.getRaw("article_doi") != null) {
			file_map.put("article_doi",am.getRaw("article_doi"));
		}
		if (am.getRaw("article_publish_date") != null) {
			file_map.put("article_publish_date",am.getRaw("article_publish_date"));
		}	
		if (am.getRaw("article_journal") != null) {
			file_map.put("article_journal",am.getRaw("article_journal"));
		}	
		// The doi for this item will already be on the AM if it was found
	}
	

}