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

package edu.indiana.lib.plugin.mto;

import java.net.MalformedURLException;

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class MTO2022UrlNormalizer extends HttpHttpsParamUrlNormalizer {

  private static final Logger log = Logger.getLogger(MTO2022UrlNormalizer.class);
  
  public MTO2022UrlNormalizer() {
    super(ConfigParamDescr.BASE_URL.getKey());
  }
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) {
    String baseUrlHost = au.getConfiguration().get("base_url_host");
    try {
      String urlHost = UrlUtil.getHost(url);
      if (urlHost.equals("www." + baseUrlHost)) {
        url = UrlUtil.delSubDomain(url, "www");
      }
    }
    catch (MalformedURLException mue) {
      log.debug2("Malformed URL", mue);
    }
    return super.normalizeUrl(url, au);
  }
  
  public static void main(String[] args) throws Exception {
//    String url = "https://mtosmt.org/issues/mto.04.10.1/toc.10.1.html";  
    String url = "http://mtosmt.org/issues/mto.04.10.1/toc.10.1.html";  
//    String url = "http://www.mtosmt.org/issues/mto.04.10.1/mto.04.10.1.schmalfeldt.html";  
    
    String baseUrl = "https://mtosmt.org/";
    String urlWithoutWww = UrlUtil.delSubDomain(url, "www");
    try {
      if (UrlUtil.getUrlPrefix(urlWithoutWww).equals(baseUrl)) {
        System.out.println(urlWithoutWww);
        System.exit(0);
      } 
    }
    catch (MalformedURLException mue) {
      System.out.println("Malformed URL: " + mue);
    }
    System.out.println(url);
    System.exit(1);
  }
  
}
