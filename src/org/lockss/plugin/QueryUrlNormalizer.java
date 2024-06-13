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

package org.lockss.plugin;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.util.UrlUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**  
 * <p>
 * A URL normalizer that parses the query string into key-value pairs and lets you drop some, add some and reorder them. 
 * </p>
 * 
 * @author Carmen Cox
 * @since 1.78
 */

public class QueryUrlNormalizer implements UrlNormalizer{
    @Override
  public String normalizeUrl(String url,
                             ArchivalUnit au)
      throws PluginException {
        if(!url.contains("?")){
            return url;
        }
        int qmark = url.indexOf('?');
        String primary = url.substring(0,qmark);
        String query = url.substring(qmark+1);
        String[] pairs = StringUtils.split(query,'&');
        ListValuedMap<String, String> queryMap = new ArrayListValuedHashMap<String,String>();
        for(String pair:pairs){
            int equals = pair.indexOf('=');
            if(equals < 0){
                queryMap.put(pair, null);
            } else {
                String key = pair.substring(0,equals);
                String value = pair.substring(equals+1);
                if(!shouldDropKey(key) && !shouldDropKeyValue(key, value)){
                  queryMap.put(key, value);
                }
                
            }
        }
        processQueryMap(queryMap);
        List<String> sortedKeys = new ArrayList<String>(queryMap.keySet());
        Collections.sort(sortedKeys, getKeyComparator());
        StringBuilder result = new StringBuilder(primary + "?");
        for(String key:sortedKeys){
          for(String value:queryMap.get(key)){
            result.append(key);
            if(value != null){
              result.append("=").append(value);
            }
            result.append("&");
          }
        }
        result.deleteCharAt(result.length()-1);
        return result.toString();
  }

  public void processQueryMap(ListValuedMap<String, String> queryMap){

  }

  public boolean shouldDropKey(String key){
    return false;
  }

  public boolean shouldDropKeyValue(String key, String value){
    return false;
  }

  public Comparator<String> getKeyComparator(){
    return Comparator.<String>naturalOrder();
  }
}
