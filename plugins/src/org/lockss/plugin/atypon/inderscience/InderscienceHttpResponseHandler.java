/*

Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.atypon.inderscience;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.CacheResultHandler;

public class InderscienceHttpResponseHandler implements CacheResultHandler{
    
    private static final Logger logger = Logger.getLogger(InderscienceHttpResponseHandler.class);

    @Override
    public CacheException handleResult(ArchivalUnit au,
                                        String url,
                                        int responseCode) {
        switch (responseCode) {
        case 500:
            logger.debug2("500: " + url);
            if(url.matches("^https?://www.inderscienceonline.com/.*\\.(jpg|gif)")){
                logger.debug2("This link is a broken link exception (non-fatal):" + url);
                return new CacheException.NoRetryDeadLinkException("500 Internal Server Error (non-fatal)");
            }
        default: 
            logger.warning("Unexpected responseCode (" + responseCode + ") in handleResult(): AU " + au.getName() + "; URL " + url);
            throw new UnsupportedOperationException("Unexpected responseCode (" + responseCode + ")");
        }
    }

    public CacheException handleResult(ArchivalUnit au,
                                        String url,
                                        Exception ex) {
        logger.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
        throw new UnsupportedOperationException();
    }
}
