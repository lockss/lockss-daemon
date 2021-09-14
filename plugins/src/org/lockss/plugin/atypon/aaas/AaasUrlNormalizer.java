/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.atypon.aaas;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.HttpHttpsParamUrlNormalizer;
import org.lockss.util.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AaasUrlNormalizer extends HttpHttpsParamUrlNormalizer {
  protected static Logger log = Logger.getLogger(AaasUrlNormalizer.class);

  protected static final String DOWNLOAD_STRING = "?download=true";
  protected static final Pattern DOWNLOAD_PAT = Pattern.compile("\\?download=true", Pattern.CASE_INSENSITIVE);

  protected static final String EPDF_STRING = "/doi/epdf/";
  protected static final String PDF_STRING = "/doi/pdf/";
  protected static final Pattern EPDF_PAT = Pattern.compile(EPDF_STRING, Pattern.CASE_INSENSITIVE);

  @Override
  public String normalizeUrl(String url, ArchivalUnit au) {

    Matcher epdf_mat = EPDF_PAT.matcher(url);
    if (epdf_mat.find()) {
      url = url.replace(EPDF_STRING, PDF_STRING);
    }

    Matcher download_mat = DOWNLOAD_PAT.matcher(url);
    if (download_mat.find()) {
      url = url.replace(DOWNLOAD_STRING, "");
    }

    return url;
  }
}
