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

package org.lockss.plugin.silvafennica;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.lockss.plugin.FilterFactory;
import org.lockss.test.LockssTestCase;
import org.lockss.util.Constants;

public class TestFS2HtmlHashFilterFactory extends LockssTestCase{
    public void testFilter() throws Exception{
        FilterFactory fact = new FS2HtmlHashFilterFactory();
        String page = "<html>"+
                        "<head>"+
                        "</head>"+
                        "<body>"+
                            "<p class='abstract'>"+
                              "<span class='whatever'>"+
                                "Views"+
                              "</span>"+
                              " 71 "+
                            "</p>"+
                            "<p class='abstract'>"+
                              "<span class='whatever'>"+
                                "Katselukerrat"+
                              "</span>"+
                              " 71 "+
                            "</p>"+
                            "<p class='abstract'>"+
                              "<span class='whatever'>"+
                                "Views other stuff"+
                              "</span>"+
                              "72"+
                            "</p>"+
                            "<p class='abstract'>"+
                              "<span class='whatever'>"+
                                "PASS"+
                              "</span>"+
                              "73"+
                            "</p>"+
                        "</body>"+
                      "</html>";
        InputStream in = IOUtils.toInputStream(page,Constants.DEFAULT_ENCODING);
        InputStream out = fact.createFilteredInputStream(null, in, Constants.DEFAULT_ENCODING);
        String result = IOUtils.toString(out, Constants.DEFAULT_ENCODING);
        System.out.println(result);
        assertFalse("Your test case failed! \n"+result, !result.contains("PASS")); 
        assertTrue(result.contains("Views other stuff"));
        assertFalse("Your test case failed! \n", result.contains("71"));
    }
}