/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.silverchair;

import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.HeaderUtil;
import org.lockss.util.IOUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class SilverchairCommonThemeContentImageContentValidatorFactory implements ContentValidatorFactory {
    private static final Logger log = Logger.getLogger(SilverchairCommonThemeContentImageContentValidatorFactory.class);

    protected static final String VIEWIMAGE = "/view-large/figure/";
    protected static final String[] IMAGE_EXTS = { ".tif", ".jpg", ".jpeg", ".png", ".gif" };

    public ContentValidator getTextTypeValidator() {
        return new TextTypeValidator();
    }

    public ContentValidator createContentValidator(ArchivalUnit au, String contentType) {
        switch (HeaderUtil.getMimeTypeFromContentType(contentType)) {
            case "text/html":
            case "text/*":
                return getTextTypeValidator();
            default:
                return null;
        }
    }

    public static class TextTypeValidator implements ContentValidator {
        public TextTypeValidator() {
            super();
        }

        public boolean invalidFileExt(String url) {
            if (url.contains(VIEWIMAGE)) {
                return false;
            }
            for (String ext : IMAGE_EXTS) {
                if (StringUtil.endsWithIgnoreCase(url, ext)) {
                    return true;
                }
            }
            return false;
        }

        public void validate(CachedUrl cu)
                throws ContentValidationException, PluginException, IOException {
            String url = cu.getUrl();
            if (invalidFileExt(url)) {
                log.warning("URL MIME type mismatch: " + url);
                throw new ContentValidationException("URL MIME type mismatch");
            }
        }
    }
}