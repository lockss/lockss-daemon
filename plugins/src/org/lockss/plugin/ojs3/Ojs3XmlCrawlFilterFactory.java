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
import java.io.SequenceInputStream;
import java.nio.*;
import java.nio.charset.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


import org.apache.commons.io.IOUtils;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.*;

public class Ojs3XmlCrawlFilterFactory implements FilterFactory {

    private static final Logger log = Logger.getLogger(Ojs3XmlCrawlFilterFactory.class);

    protected static final Pattern xmlEncodingPat =
        Pattern.compile("(<\\?xml\\s[^?]*encoding=)('utf8'|\"utf8\")([^?]*\\?>)",
                        Pattern.CASE_INSENSITIVE);

    @Override
    public InputStream createFilteredInputStream(ArchivalUnit au,
                                                 InputStream in, 
                                                 String encoding) {

        byte[] buffer = new byte[1024];
        int actual;
        try {
          actual = IOUtils.read(in, buffer);
        }
        catch (IOException ioe) {
          log.debug3("Could not read from underlying input stream", ioe);
          return in; // Presumably no bytes read
        }
        
        // Get a CharsetDecoder that throws CharacterCodingException
        Charset charset = Charset.isSupported(encoding) ? Charset.forName(encoding) : Charset.forName(Constants.ENCODING_UTF_8);
        CharsetDecoder decoder = charset.newDecoder()
                                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                                        .onMalformedInput(CodingErrorAction.REPORT);
        
        // Decode the beginning of the stream; may have a split multibyte character at the end
        String beginning = null;
        int end = actual;
        while (end > 0) {
          try {
            beginning = decoder.reset().decode(ByteBuffer.wrap(buffer, 0, end)).toString();
            break;
          }
          catch (CharacterCodingException cce) {
            end = end - 1;
          }
        }
        if (end == 0) {
          log.debug3("Could not turn bytes into string");
          return new SequenceInputStream(new ByteArrayInputStream(buffer, 0, actual), in); // Should be equivalent to the original stream
        }

        log.debug3("The beginning of the XML file is:\n" + beginning);
        Matcher xmlEncodingMat = xmlEncodingPat.matcher(beginning);
        beginning = xmlEncodingMat.replaceFirst("$1'utf-8'$3");
        log.debug3("The beginning of the XML file is:\n" + beginning);
        if (end < actual) {
          // Don't forget the bytes at the end that may have been ignored during decoding
          in = new SequenceInputStream(new ByteArrayInputStream(buffer, end, actual - end), in);
        }
        return new SequenceInputStream(IOUtils.toInputStream(beginning, charset), in);
    }
}
