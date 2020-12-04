/*

Copyright (c) 2000-2020, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.respediatrica;

import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.respediatrica.ResPediatricaUrlNormalizer;
import org.lockss.test.LockssTestCase;

public class TestResPediatricaUrlNormalizer extends LockssTestCase {
    private static final String urlStr1 = "https://cdn.gn1.link/residenciapediatrica/Content/css/responsive.css?v=1";
    private static final String urlStr2 = "https://cdn.gn1.link/residenciapediatrica/Content/css/style.css?v=1";
    private static final String urlStr3 = "https://cdn.gn1.link/residenciapediatrica/Images/liv-re-home.jpg?v=2";
    private static final String urlStr4 = "https://cdn.gn1.link/residenciapediatrica/Scripts/script.js?v=2";

    private static final String resultStr1 = "https://cdn.gn1.link/residenciapediatrica/Content/css/responsive.css";
    private static final String resultStr2 = "https://cdn.gn1.link/residenciapediatrica/Content/css/style.css";
    private static final String resultStr3 = "https://cdn.gn1.link/residenciapediatrica/Images/liv-re-home.jpg";
    private static final String resultStr4 = "https://cdn.gn1.link/residenciapediatrica/Scripts/script.js";

    public void testUrlNormalizer() throws Exception {
        UrlNormalizer normalizer = new ResPediatricaUrlNormalizer();
        assertEquals(resultStr1, normalizer.normalizeUrl(urlStr1, null));
        assertEquals(resultStr2, normalizer.normalizeUrl(urlStr2, null));
        assertEquals(resultStr3, normalizer.normalizeUrl(urlStr3, null));
        assertEquals(resultStr4, normalizer.normalizeUrl(urlStr4, null));
    }
}

