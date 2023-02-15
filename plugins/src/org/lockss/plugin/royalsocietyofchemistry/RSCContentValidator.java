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

package org.lockss.plugin.royalsocietyofchemistry;

import java.io.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.StringUtil;
import org.lockss.util.HeaderUtil;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;

public class RSCContentValidator {
	
	private static final Logger log = Logger.getLogger(RSCContentValidator.class);

	protected static final String PDF_1 = "/articlepdf/";
	protected static final String PDF_2 = "/chapterpdf/";

	//http://pubs.rsc.org/en/content/articlelanding/2016/fo/c6fo00030d
	//http://pubs.rsc.org/en/journals/lockss?journalcode=fo&volume=7&year=2016&issue=10  


	/*
  <title>  Access Denied</title><link rel="shortcut icon" href="https://www.rsc-cdn....">
</head><body><div>Access Denied</div>
...
	 */
	private static final String ACCESS_DENIED_STRING = "Access Denied";


	public static class TextTypeValidator implements ContentValidator {

		public void validate(CachedUrl cu)
				throws ContentValidationException, PluginException, IOException {
			// validate based on pdf type url (ie .pdf or .jpg)
			String url = cu.getUrl();
			if ((StringUtil.indexOfIgnoreCase(url, PDF_1) > 0) ||
					(StringUtil.indexOfIgnoreCase(url, PDF_2) > 0)) {
				//We want this to try and fetch again - this exception gets mapped to retry
				throw new ContentValidationException("URL MIME type mismatch");
			}

			/* if this is a TOC or article landing page */

			if (url.contains("/articlelanding/") || url.contains("&issue=")) { 
				Reader reader = new BufferedReader(cu.openForReading());
				try {
					if (StringUtil.containsString(reader,ACCESS_DENIED_STRING,true)) {
						throw new ContentValidationException("Found access denied page");
					}
				} finally {
					IOUtil.safeClose(reader);
					cu.release();
				}
			}			
		}
	}

  public static class Factory implements ContentValidatorFactory {
    public ContentValidator createContentValidator(ArchivalUnit au, String contentType) {
      switch (HeaderUtil.getMimeTypeFromContentType(contentType)) {
      case "text/html":
      case "text/*":
        return new TextTypeValidator();
      default:
        return null;
      }
    }
  }
  
}

