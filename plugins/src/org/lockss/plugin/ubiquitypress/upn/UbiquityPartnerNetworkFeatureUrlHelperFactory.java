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

package org.lockss.plugin.ubiquitypress.upn;
import org.lockss.plugin.*;

import java.io.IOException;
import java.util.Collection;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import org.lockss.util.Logger;
import org.lockss.daemon.PluginException;

public class UbiquityPartnerNetworkFeatureUrlHelperFactory implements FeatureUrlHelperFactory{

    private static final Logger log = Logger.getLogger(UbiquityPartnerNetworkFeatureUrlHelperFactory.class);

    @Override
    public FeatureUrlHelper createFeatureUrlHelper(Plugin plug) {
      return new UbiquityPartnerNetworkFeatureUrlHelper();
    }

    private static class UbiquityPartnerNetworkFeatureUrlHelper extends BaseFeatureUrlHelper{

        private String year;
        private String baseUrl;

        @Override
        public Collection<String> getAccessUrls(ArchivalUnit au)
            throws IOException, PluginException {
    
            if (au == null) {
              return null;
            }
            String auid = au.getAuId();
            String fname = "deceasedAUs.dat";
            InputStream is = null;
    
            is = getClass().getResourceAsStream(fname);
            if (is == null) {
              throw new ExceptionInInitializerError("UPN Network Deceased AUs data file not found.");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String nextline = null;
            Collection<String> uUrls = new ArrayList<String>(1);
            while ((nextline = reader.readLine()) != null && uUrls.isEmpty()) {
              nextline = nextline.trim();
              //check every line in data file to see if au is a deceased au
              if(nextline.contains(auid)){
                log.debug3("The AUID is in the deceased AUs file and it's " + nextline.toString());
                baseUrl = au.getConfiguration().get("base_url");
                year = au.getConfiguration().get("year");
                String s = baseUrl + "lockss/year/" + year;
                uUrls.add(s);
                log.debug3("The start url is " + s);
                break;
              }
            }
            reader.close();
            if(uUrls.isEmpty()){
              log.debug3("uUrls is empty and the start urls are " + au.getStartUrls());
              return au.getStartUrls();
            }
            log.debug3("uUrls is NOT empty and the start urls are " + uUrls);
            return uUrls;
        }
    }
}
