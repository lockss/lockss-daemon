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

package org.lockss.plugin.smartjournal.univsal;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.lockss.daemon.PluginException;
import org.lockss.filter.FilterUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.ReaderInputStream;

public class SulaimaniDentalJournalsRisHashFilterFactory implements FilterFactory {

    // Matches lines like: Y2  - 2026/7/7/
    private static final Pattern Y2_LINE =
            Pattern.compile("(?m)^Y2\\s*-\\s*.*$\r?\n?");

    @Override
    public InputStream createFilteredInputStream(ArchivalUnit au,
                                                 InputStream in,
                                                 String encoding)
            throws PluginException {
        try {
            String contents = IOUtils.toString(in, encoding);
            String filtered = Y2_LINE.matcher(contents).replaceAll("");
            return new ReaderInputStream(new StringReader(filtered), encoding);
        } catch (IOException e) {
            throw new PluginException("Error filtering RIS content", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}