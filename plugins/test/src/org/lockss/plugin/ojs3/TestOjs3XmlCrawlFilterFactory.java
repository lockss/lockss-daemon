/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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
package org.lockss.plugin.ojs3;

import java.io.InputStream;

import org.lockss.util.*;
import org.lockss.test.*;

import org.apache.commons.io.IOUtils;

public class TestOjs3XmlCrawlFilterFactory extends LockssTestCase {

    static Logger log = Logger.getLogger(TestOjs3XmlCrawlFilterFactory.class);

    private Ojs3XmlCrawlFilterFactory fact;
    private MockArchivalUnit mau;

    public void setUp() throws Exception {
        super.setUp();
        fact = new Ojs3XmlCrawlFilterFactory();
        mau = new MockArchivalUnit();
    }

    private static final String emptyFile = "";

    private static final String goodXml = 
    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"+
        "<!DOCTYPE article  PUBLIC \"-//NLM//DTD JATS (Z39.96) Journal Archiving and Interchange DTD v1.1 20151215//EN\" \"https://jats.nlm.nih.gov/archiving/1.1/JATS-archivearticle1.dtd\">\n"+
        "<article dtd-version=\"1.1\" article-type=\"book-review\">\n";

    private static final String fixedXml =
  		"<?xml version='1.0' encoding='utf-8'?>\n" + 
  		" <!DOCTYPE article PUBLIC \"-//NLM//DTD JATS (Z39.96) Journal Archiving and Interchange DTD v1.1 20151215//EN\" \"https://jats.nlm.nih.gov/archiving/1.1/JATS-archivearticle1.dtd\">\n" + 
  		"<article dtd-version=\"1.1\" article-type=\"book-review\">";
    
    private static final String goodXmlLong =
  		"<?xml version=\"1.0\" encoding='utf-8' standalone=\"yes\"?>\n" + 
  		" <!DOCTYPE article PUBLIC \"-//NLM//DTD JATS (Z39.96) Journal Archiving and Interchange DTD v1.1 20151215//EN\" \"https://jats.nlm.nih.gov/archiving/1.1/JATS-archivearticle1.dtd\">\n" + 
  		"<article dtd-version=\"1.1\" article-type=\"book-review\">\n"+
            "<front>\n"+
                "<journal-meta>\n"+
                    "<journal-id>TMR</journal-id>\n"+
                        "<journal-title-group>\n"+
                            "<journal-title>The Medieval Review</journal-title>\n"+
                        "</journal-title-group>\n"+
                        "<issn pub-type=\"epub\">1096-746X</issn>\n"+
                        "<publisher>\n"+
                            "<publisher-name>Indiana University</publisher-name>\n"+
                        "</publisher>\n"+
                "</journal-meta>\n"+
                "<article-meta>\n"+
                    "<article-id pub-id-type=\"publisher-id\">22.01.01</article-id>\n"+
                    "<title-group>\n"+
                        "<article-title>22.01.01, Pétrarque; Blanc, trans. and ed., Le Chansonnier</article-title>\n"+
                    "</title-group>\n"+
                    "<contrib-group>\n"+
                        "<contrib contrib-type=\"author\">\n"+
                            "<name>\n"+
                                "<surname>Florence Bistagne </surname>\n"+
                                "<given-names/>\n"+
                            "</name>\n"+
                            "<aff>Université d'Avignon</aff>\n"+
                            "<address>\n"+
                                "<email>florence.bistagne@orange.fr</email>\n"+
                            "</address>\n"+
                        "</contrib>\n"+
                    "</contrib-group>\n"+
                    "<pub-date publication-format=\"epub\" date-type=\"pub\" iso-8601-date=\"2022\">\n"+
                        "<year>2022</year>\n"+
                    "</pub-date>\n";

    private static final String goodXmlEncodingLater =
  		" <!DOCTYPE article PUBLIC \"-//NLM//DTD JATS (Z39.96) Journal Archiving and Interchange DTD v1.1 20151215//EN\" \"https://jats.nlm.nih.gov/archiving/1.1/JATS-archivearticle1.dtd\">\n" + 
  		"<article dtd-version=\"1.1\" article-type=\"book-review\">\n"+
            "<front>\n"+
                "<journal-meta>\n"+
                    "<journal-id>TMR</journal-id>\n"+
                        "<journal-title-group>\n"+
                            "<journal-title>The Medieval Review</journal-title>\n"+
                        "</journal-title-group>\n"+
                        "<issn pub-type=\"epub\">1096-746X</issn>\n"+
                        "<publisher>\n"+
                            "<publisher-name>Indiana University</publisher-name>\n"+
                        "</publisher>\n"+
                "</journal-meta>\n"+
                "<article-meta>\n"+
                    "<article-id pub-id-type=\"publisher-id\">22.01.01</article-id>\n"+
                    "<title-group>\n"+
                        "<article-title>22.01.01, Pétrarque; Blanc, trans. and ed., Le Chansonnier</article-title>\n"+
                    "</title-group>\n"+
                    "<contrib-group>\n"+
                        "<contrib contrib-type=\"author\">\n"+
                            "<name>\n"+
                                "<surname>Florence Bistagne </surname>\n"+
                                "<given-names/>\n"+
                            "</name>\n"+
                            "<aff>Université d'Avignon</aff>\n"+
                            "<address>\n"+
                                "<email>florence.bistagne@orange.fr</email>\n"+
                            "</address>\n"+
                        "</contrib>\n"+
                    "</contrib-group>\n"+
                    "<pub-date publication-format=\"epub\" date-type=\"pub\" iso-8601-date=\"2022\">\n"+
                        "<year>2022</year>\n"+
                    "</pub-date>\n"+
                    "<?xml version=\"1.0\" encoding=\"utf8\" standalone=\"yes\"?>\n";

    private static final String badXml =
  		"<?xml version='1.0' encoding='utf8'?>\n" + 
  		" <!DOCTYPE article PUBLIC \"-//NLM//DTD JATS (Z39.96) Journal Archiving and Interchange DTD v1.1 20151215//EN\" \"https://jats.nlm.nih.gov/archiving/1.1/JATS-archivearticle1.dtd\">\n" + 
  		"<article dtd-version=\"1.1\" article-type=\"book-review\">";

    private static final String badXmlDoubleQuotes =
  		"<?xml version='1.0' encoding=\"utf8\"?>\n" + 
  		" <!DOCTYPE article PUBLIC \"-//NLM//DTD JATS (Z39.96) Journal Archiving and Interchange DTD v1.1 20151215//EN\" \"https://jats.nlm.nih.gov/archiving/1.1/JATS-archivearticle1.dtd\">\n" + 
  		"<article dtd-version=\"1.1\" article-type=\"book-review\">";

    private static final String badXmlLong =
  		"<?xml version=\"1.0\" encoding=\"utf8\" standalone=\"yes\"?>\n" + 
  		" <!DOCTYPE article PUBLIC \"-//NLM//DTD JATS (Z39.96) Journal Archiving and Interchange DTD v1.1 20151215//EN\" \"https://jats.nlm.nih.gov/archiving/1.1/JATS-archivearticle1.dtd\">\n" + 
  		"<article dtd-version=\"1.1\" article-type=\"book-review\">\n"+
            "<front>\n"+
                "<journal-meta>\n"+
                    "<journal-id>TMR</journal-id>\n"+
                        "<journal-title-group>\n"+
                            "<journal-title>The Medieval Review</journal-title>\n"+
                        "</journal-title-group>\n"+
                        "<issn pub-type=\"epub\">1096-746X</issn>\n"+
                        "<publisher>\n"+
                            "<publisher-name>Indiana University</publisher-name>\n"+
                        "</publisher>\n"+
                "</journal-meta>\n"+
                "<article-meta>\n"+
                    "<article-id pub-id-type=\"publisher-id\">22.01.01</article-id>\n"+
                    "<title-group>\n"+
                        "<article-title>22.01.01, Pétrarque; Blanc, trans. and ed., Le Chansonnier</article-title>\n"+
                    "</title-group>\n"+
                    "<contrib-group>\n"+
                        "<contrib contrib-type=\"author\">\n"+
                            "<name>\n"+
                                "<surname>Florence Bistagne </surname>\n"+
                                "<given-names/>\n"+
                            "</name>\n"+
                            "<aff>Université d'Avignon</aff>\n"+
                            "<address>\n"+
                                "<email>florence.bistagne@orange.fr</email>\n"+
                            "</address>\n"+
                        "</contrib>\n"+
                    "</contrib-group>\n"+
                    "<pub-date publication-format=\"epub\" date-type=\"pub\" iso-8601-date=\"2022\">\n"+
                        "<year>2022</year>\n"+
                    "</pub-date>\n";

    private static final String badXmlEncodingLater =
  		" <!DOCTYPE article PUBLIC \"-//NLM//DTD JATS (Z39.96) Journal Archiving and Interchange DTD v1.1 20151215//EN\" \"https://jats.nlm.nih.gov/archiving/1.1/JATS-archivearticle1.dtd\">\n" + 
  		"<article dtd-version=\"1.1\" article-type=\"book-review\">\n"+
            "<front>\n"+
                "<journal-meta>\n"+
                    "<journal-id>TMR</journal-id>\n"+
                        "<journal-title-group>\n"+
                            "<journal-title>The Medieval Review</journal-title>\n"+
                        "</journal-title-group>\n"+
                        "<issn pub-type=\"epub\">1096-746X</issn>\n"+
                        "<publisher>\n"+
                            "<publisher-name>Indiana University</publisher-name>\n"+
                        "</publisher>\n"+
                "</journal-meta>\n"+
                "<article-meta>\n"+
                    "<article-id pub-id-type=\"publisher-id\">22.01.01</article-id>\n"+
                    "<title-group>\n"+
                        "<article-title>22.01.01, Pétrarque; Blanc, trans. and ed., Le Chansonnier</article-title>\n"+
                    "</title-group>\n"+
                    "<contrib-group>\n"+
                        "<contrib contrib-type=\"author\">\n"+
                            "<name>\n"+
                                "<surname>Florence Bistagne </surname>\n"+
                                "<given-names/>\n"+
                            "</name>\n"+
                            "<aff>Université d'Avignon</aff>\n"+
                            "<address>\n"+
                                "<email>florence.bistagne@orange.fr</email>\n"+
                            "</address>\n"+
                        "</contrib>\n"+
                    "</contrib-group>\n"+
                    "<pub-date publication-format=\"epub\" date-type=\"pub\" iso-8601-date=\"2022\">\n"+
                        "<year>2022</year>\n"+
                    "</pub-date>\n"+
                    "<?xml version=\"1.0\" encoding=\"utf8\" standalone=\"yes\"?>\n";

    private static String badHeaderString = "<?xml version=\'1.0\' encoding=\'utf8\' standalone=\'yes\'?>";
    private static String goodHeaderString = "<?xml version=\'1.0\' encoding=\'utf-8\' standalone=\'yes\'?>";
//use U+0800 which is "\u0800" in java -> 0xE0 0xA0 0x80
    public void testFiltering() throws Exception {
        InputStream inA;
        //make sure properly encoded xml files are not changed
        inA = fact.createFilteredInputStream(mau, IOUtils.toInputStream(goodXml, Constants.DEFAULT_ENCODING),
            Constants.DEFAULT_ENCODING);
        assertTrue(IOUtils.contentEquals(IOUtils.toInputStream(goodXml, Constants.DEFAULT_ENCODING), inA));
        //check standard bad encoding
        inA = fact.createFilteredInputStream(mau, IOUtils.toInputStream(badXml, Constants.DEFAULT_ENCODING),
            Constants.DEFAULT_ENCODING);
        assertTrue(IOUtils.contentEquals(IOUtils.toInputStream(fixedXml, Constants.DEFAULT_ENCODING), inA));
        //check when encoding is in double quotes
        inA = fact.createFilteredInputStream(mau, IOUtils.toInputStream(badXmlDoubleQuotes, Constants.DEFAULT_ENCODING),
            Constants.DEFAULT_ENCODING);
        assertTrue(IOUtils.contentEquals(IOUtils.toInputStream(fixedXml, Constants.DEFAULT_ENCODING), inA));
        //check when xml file is more than 1025 bytes
        inA = fact.createFilteredInputStream(mau, IOUtils.toInputStream(badXmlLong, Constants.DEFAULT_ENCODING),
            Constants.DEFAULT_ENCODING);
        assertTrue(IOUtils.contentEquals(IOUtils.toInputStream(goodXmlLong, Constants.DEFAULT_ENCODING), inA));
        //check empty string
        inA = fact.createFilteredInputStream(mau, IOUtils.toInputStream(emptyFile, Constants.DEFAULT_ENCODING),
            Constants.DEFAULT_ENCODING);
        assertTrue(IOUtils.contentEquals(IOUtils.toInputStream(emptyFile, Constants.DEFAULT_ENCODING), inA));
        //check if utf8 is later in the xml file
        inA = fact.createFilteredInputStream(mau, IOUtils.toInputStream(badXmlEncodingLater, Constants.DEFAULT_ENCODING),
            Constants.DEFAULT_ENCODING);
        assertTrue(IOUtils.contentEquals(IOUtils.toInputStream(goodXmlEncodingLater, Constants.DEFAULT_ENCODING), inA));
        //check when there's a multibyte character at 1021, 1022, 1023, 1024, and 1025 bytes  
        //need additional dummy character in 'good' string to make up for added hyphen in utf-8
        for(int i = 1021; i < 1026; i++){
            String paddedBadString = padString(i, badHeaderString) + "\u8000" ;
            String paddedGoodString = padString(i, goodHeaderString) + ".\u8000" ;
            inA = fact.createFilteredInputStream(mau, IOUtils.toInputStream(paddedBadString, Constants.ENCODING_UTF_8),
            Constants.ENCODING_UTF_8);
            assertTrue(IOUtils.contentEquals(IOUtils.toInputStream(paddedGoodString, Constants.ENCODING_UTF_8), inA));
        }
    }

    public String padString(int targetLength, String input) throws Exception{
        int targetBytes = targetLength;
        byte[] originalBytes = input.getBytes(Constants.ENCODING_UTF_8);
        int currentBytesLength = originalBytes.length;
        int bytesNeeded = targetBytes - currentBytesLength;
        byte[] paddedBytes = new byte[targetBytes];
        System.arraycopy(originalBytes, 0, paddedBytes, 0, currentBytesLength);
        byte paddingChar = (byte)'.';
        java.util.Arrays.fill(paddedBytes,currentBytesLength,targetBytes,paddingChar);
        String paddedString = new String(paddedBytes, Constants.ENCODING_UTF_8);
        log.debug3("Padded string length in bytes: " + paddedString.getBytes(Constants.ENCODING_UTF_8).length);
        return paddedString;
    }
}
