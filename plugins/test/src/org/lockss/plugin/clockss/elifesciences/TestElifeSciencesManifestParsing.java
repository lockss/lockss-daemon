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

package org.lockss.plugin.clockss.elifesciences;

import org.lockss.test.LockssTestCase;
import org.lockss.util.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;


public class TestElifeSciencesManifestParsing extends LockssTestCase {

    private static final Logger log = Logger.getLogger(TestElifeSciencesManifestParsing.class);

    private Document doc;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        String fname = "sample_manifest.xml";
        String manifestXml = getResourceContent(fname);
        assertNotNull("Could not load test resource: " + fname, manifestXml);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();

        builder.setEntityResolver(new EntityResolver() {
            @Override
            public InputSource resolveEntity(String publicId, String systemId)
                    throws SAXException, IOException {
                return new InputSource(new StringReader(""));
            }
        });

        doc = builder.parse(new InputSource(new StringReader(manifestXml)));
        log.debug3("Parsed " + fname + " for testing.");
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private String findHrefByMediaType(String targetMediaType) {
        NodeList instances = doc.getElementsByTagName("instance");
        for (int i = 0; i < instances.getLength(); i++) {
            Element el = (Element) instances.item(i);
            String mediaType = el.getAttribute("media-type");
            String href = el.getAttribute("href");
            if (targetMediaType.equalsIgnoreCase(mediaType)) {
                return href;
            }
        }
        return null;
    }

    public void testXmlInstanceFound() throws Exception {
        String xmlHref = findHrefByMediaType("application/xml");
        assertNotNull("Expected an application/xml instance in manifest", xmlHref);
        assertEquals("content/599315.xml", xmlHref);
    }

    public void testPdfInstanceFound() throws Exception {
        String pdfHref = findHrefByMediaType("application/pdf");
        assertNotNull("Expected an application/pdf instance in manifest", pdfHref);
        assertEquals("content/elife-preprint-100056-v3.pdf", pdfHref);
    }
}