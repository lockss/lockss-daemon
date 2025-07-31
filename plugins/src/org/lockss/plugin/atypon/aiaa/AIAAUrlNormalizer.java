/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.atypon.aiaa;

import org.lockss.plugin.QueryUrlNormalizer;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.util.Logger;
import org.lockss.daemon.PluginException;
import java.util.regex.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.atypon.BaseAtyponUrlNormalizer;
import org.apache.commons.collections4.ListValuedMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class AIAAUrlNormalizer extends QueryUrlNormalizer {

    private static Logger log = Logger.getLogger(AIAAUrlNormalizer.class);

    //Example: https://arc.aiaa.org/browse/book/10.2514/MAVIAT21/proceedings-topics/aer
    protected Pattern TOC_PAGE = Pattern.compile("https://[^/]+/browse/book/[0-9.]+/[^./]+/proceedings-topics/[^./#=\\?]+$", Pattern.CASE_INSENSITIVE);

    protected UrlNormalizer baseUrlNormalizer = new BaseAtyponUrlNormalizer();

    @Override 
    public String normalizeUrl(String url, ArchivalUnit au)
      throws PluginException {
        url = baseUrlNormalizer.normalizeUrl(url, au);
        if(TOC_PAGE.matcher(url).matches()){
            url = url + "?pageSize=100&sortBy=Earliest&startPage=0";
        }
        
        return super.normalizeUrl(url, au);
      }

    @Override
    public void processQueryMap(ListValuedMap<String, String> queryMap){
        List<String> keySet = new ArrayList<String>(queryMap.keySet());
        for(String key:keySet){
            log.debug3("key is " + key + " value is " + queryMap.get(key));
            if("sortBy".equals(key) && queryMap.get(key).contains("relevancy")){
                queryMap.remove("sortBy");
                queryMap.put("sortBy","Earliest");
            }
            if("pageSize".equals(key) && (queryMap.get(key).contains("20") || queryMap.get(key).contains("50"))){
                queryMap.remove("pageSize");
                queryMap.put("pageSize","100");
            }
        }
    }

    @Override
    public boolean shouldDropKey(String key) {
      if(key.equals("pageSize") || key.equals("sortBy") || 
         key.equals("startPage")){
        return false;
      }else return true;
    }
}
