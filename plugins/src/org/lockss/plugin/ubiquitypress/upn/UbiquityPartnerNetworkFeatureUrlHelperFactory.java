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
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.ArrayList;
import org.lockss.util.UrlUtil;
import org.lockss.util.Logger;
import org.lockss.daemon.PluginException;

public class UbiquityPartnerNetworkFeatureUrlHelperFactory implements FeatureUrlHelperFactory{

    private static final Logger log = Logger.getLogger(UbiquityPartnerNetworkFeatureUrlHelperFactory.class);

    @Override
    public FeatureUrlHelper createFeatureUrlHelper(Plugin plug) {
      return new UbiquityPartnerNetworkFeatureUrlHelper();
    }

    private static class UbiquityPartnerNetworkFeatureUrlHelper extends BaseFeatureUrlHelper{

        public static Set<String> deceasedAUs = new HashSet<>(Arrays.asList("org|lockss|plugin|ubiquitypress|upn|ClockssUbiquityPartnerNetworkPlugin&base_url~https%3A%2F%2Fijops%2Ecom%2F&year~2018"));
        private String year;
        private String baseUrl;

        @Override
        public Collection<String> getAccessUrls(ArchivalUnit au)
            throws IOException, PluginException {
    
          if (au == null) {
            return null;
          }
          // return the non-ojs URL for deceased AUs
          if(deceasedAUs.contains(au.getAuId())){
            Collection<String> uUrls = new ArrayList<String>(1);
            baseUrl = au.getConfiguration().get("base_url");
            year = au.getConfiguration().get("year");
            String s = baseUrl + "lockss/year/" + year;
            uUrls.add(s);
            log.debug3("The start url getting changed is " + uUrls.toString());
            return uUrls;
          }else{
            return super.getAccessUrls(au);
          }
        }
    }
}
