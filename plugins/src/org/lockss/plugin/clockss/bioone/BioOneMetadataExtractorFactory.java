/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.clockss.bioone;

import org.lockss.config.TdbAu;
import org.lockss.daemon.PluginException;
import org.lockss.daemon.ShouldNotHappenException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.PubMedSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorFactory;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

public class BioOneMetadataExtractorFactory extends SourceXmlMetadataExtractorFactory {
	static Logger log = Logger.getLogger(BioOneMetadataExtractorFactory.class);

	private static SourceXmlSchemaHelper JatsPublishingHelper = null;

	@Override
	public FileMetadataExtractor createFileMetadataExtractor(MetadataTarget target,
															 String contentType)
			throws PluginException {
		return new JatsPublishingSourceXmlMetadataExtractor();
	}

	public class JatsPublishingSourceXmlMetadataExtractor extends SourceXmlMetadataExtractor {

		/*
		 * This setUpSchema shouldn't be called directly
		 * but for safety, just use the CU to figure out which schema to use.
		 *
		 */
		@Override
		protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu) {
			throw new ShouldNotHappenException("This version of the schema setup cannot be used for this plugin");
		}

		@Override
		protected SourceXmlSchemaHelper setUpSchema(CachedUrl cu, Document xmlDoc) {
			String url = cu.getUrl();
			log.debug3("Setup Jats schema helper for url " + url);
			if (JatsPublishingHelper == null) {
				JatsPublishingHelper = new JatsPublishingSchemaHelper();
			}
			return JatsPublishingHelper;
		}
		
		@Override
		protected void postCookProcess(SourceXmlSchemaHelper schemaHelper,
									   CachedUrl cu, ArticleMetadata thisAM) {

			//If we didn't get a valid date value, use the copyright year if it's there
			if (thisAM.get(MetadataField.FIELD_DATE) == null) {
				if (thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date) != null) {
					thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_date));
				} else {// last chance
					thisAM.put(MetadataField.FIELD_DATE, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_edate));
				}
			}

			thisAM.put(MetadataField.FIELD_PUBLISHER, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_pubname));

			/*
			Comment out these changes, since we like to get publisher name from the xml files
			String publisherName = "BioOne";

			TdbAu tdbau = cu.getArchivalUnit().getTdbAu();
			if (tdbau != null) {
				publisherName =  tdbau.getPublisherName();
			}
			
			thisAM.put(MetadataField.FIELD_PUBLISHER, publisherName);
			 */

			thisAM.put(MetadataField.FIELD_PROVIDER, thisAM.getRaw(JatsPublishingSchemaHelper.JATS_pubname));
		}

	}
}
