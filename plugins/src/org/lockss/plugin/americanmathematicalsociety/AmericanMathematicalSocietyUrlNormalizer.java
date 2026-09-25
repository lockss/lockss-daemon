/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.americanmathematicalsociety;

import org.apache.commons.lang.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.*;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import java.util.regex.Pattern;

public class AmericanMathematicalSocietyUrlNormalizer implements UrlNormalizer{
  
    /*
        Some URLs have the journal id as upper case (such as https://pubs.ams.org/BULL/2018-55-01/S0273-0979-2017-01575-3)
        while others have it as lowercase (such as https://www.ams.org/journals/bull/2018-55-01/S0273-0979-2017-01575-3/S0273-0979-2017-01575-3.pdf). 
        In order for the article iterator to match corresponding URLs in the same ArticleFiles, we will be changing all the 
        journal ids to be lowercase. 
    */
    private static final Logger log = Logger.getLogger(AmericanMathematicalSocietyUrlNormalizer.class);
    protected static final Pattern ABSTRACT_LANDING = Pattern.compile("\"https://pubs.ams.org/%s/\"");

    @Override
    public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
        String journal_id = au.getConfiguration().get("journal_id");
        if(url.contains("/" + journal_id.toUpperCase() + "/")){
            url = url.replace(journal_id.toUpperCase(), journal_id.toLowerCase());
        }
        return url;
    }
}