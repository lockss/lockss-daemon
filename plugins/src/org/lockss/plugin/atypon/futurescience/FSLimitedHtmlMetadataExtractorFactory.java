/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.atypon.futurescience;

import java.io.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponNullMetadataExtractorFactory.BaseAtyponNullMetadataExtractor;


/*
 * FutureScience overcrawled badly at one point and while we can limit the iterator Pattern to 
 * require the journal_id in the doi (which FS and FM use consistently) it doesn't address
 * the overcrawling to other volumes of the same journal.
 * HOWEVER - it would appear that the overcrawled articles do not hae the RIS citation information
 * and are extracting from the dc.* information which doesn't have publication level info to 
 * use to compare.
 * For FutureScience - simply reject any html sources of metadata. Only the RIS is assocaited with
 * valid articles.
 * 
 * This could probably just be BaseAtyponNullMetadataExtractorFactory but I want to leave the option
 * to special case depending on the cu...
 * 
 */
public class FSLimitedHtmlMetadataExtractorFactory 
implements FileMetadataExtractorFactory {

	public FileMetadataExtractor 
	createFileMetadataExtractor(MetadataTarget target, String contentType)
			throws PluginException {
		return new FSNullMetadataExtractor();
	}

	public static class FSNullMetadataExtractor
	implements FileMetadataExtractor {
		static Logger log = Logger.getLogger(BaseAtyponNullMetadataExtractor.class);


		@Override
		public void extract(MetadataTarget target, CachedUrl cu, Emitter emitter)
				throws IOException {
			log.debug3("Suppressing emit of metadata in Null extractor: " + cu.getUrl());
			// do nothing, do not allow TDB info to get used as default
			// by not emitting
		}

	}
}