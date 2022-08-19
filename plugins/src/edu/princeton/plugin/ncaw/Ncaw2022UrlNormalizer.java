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

package edu.princeton.plugin.ncaw;

import java.net.MalformedURLException;
import java.util.regex.*;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.UrlUtil;


public class Ncaw2022UrlNormalizer implements UrlNormalizer {

    /* Needs to
     * 1. add www to the links that are missing it
     * 2. replace http with https
     */

    @Override

    public String normalizeUrl(String url, ArchivalUnit au) throws PluginException {
        //get base_url
        String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
        String baseUrlHost = "";
        try {
             baseUrlHost = UrlUtil.getHost(baseUrl);
        } catch (MalformedURLException e) {
            //if this happens, give up on normalizing url
            return url;
        }
        //pdf reference URL https://19thc-artworldwide.org/pdf/python/article_PDFs/NCAW_1074.pdf
        Pattern urlPat = Pattern.compile("^([^:]+)://([^/]+)/(.*)$", Pattern.CASE_INSENSITIVE);
        Matcher urlMat = urlPat.matcher(url);
        
        if(urlMat.matches()){
            String protocol = urlMat.group(1);
            String hostname = urlMat.group(2);
            String path = urlMat.group(3);
            //add www to the links that are missing it
            if (("www." + hostname).equals(baseUrlHost)) {
                    hostname = "www." + hostname;
                }
            //replace http with https
            if(hostname.equals(baseUrlHost) && (UrlUtil.isHttpUrl(url) && UrlUtil.isHttpsUrl(baseUrl))) {
                protocol = "https";
            }

            return String.format("%s://%s/%s", protocol, hostname, path);

        } else {
            return url;
        }
    }
}