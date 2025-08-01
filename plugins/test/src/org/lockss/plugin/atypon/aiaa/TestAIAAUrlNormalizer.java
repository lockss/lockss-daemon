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
import org.lockss.plugin.TestQueryUrlNormalizer;
import org.lockss.plugin.UrlNormalizer;

public class TestAIAAUrlNormalizer extends TestQueryUrlNormalizer{
        public void testNormalizeUrl() throws Exception{
            UrlNormalizer norm = new AIAAUrlNormalizer();
            assertEquals("https://arc.aiaa.org/browse/book/10.2514/MASCEND22/proceedings-topics/den?pageSize=100&sortBy=Earliest&startPage=0", norm.normalizeUrl("https://arc.aiaa.org/browse/book/10.2514/MASCEND22/proceedings-topics/den",null));
            assertEquals("https://arc.aiaa.org/browse/book/10.2514/MASCEND22/proceedings-topics/den?pageSize=100&sortBy=Earliest&startPage=0", norm.normalizeUrl("https://arc.aiaa.org/browse/book/10.2514/MASCEND22/proceedings-topics/den?sortBy=Earliest&startPage=0&pageSize=50",null));
            assertEquals("https://arc.aiaa.org/browse/book/10.2514/MASCEND22/proceedings-topics/den?pageSize=100&sortBy=Earliest&startPage=0", norm.normalizeUrl("https://arc.aiaa.org/browse/book/10.2514/MASCEND22/proceedings-topics/den?sortBy=Earliest&pageSize=50&startPage=0&ContribRaw=Chrone%2C%20Jonathon%20D",null));
        }
}
