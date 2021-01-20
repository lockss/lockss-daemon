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

package org.lockss.plugin;

/**
 * <p>
 * A URL normalizer for HTTP-to-HTTPS plugin transitions, that accepts a set of
 * target URL-typed plugin parameters, and uses {@link HttpHttpsUrlHelper} to
 * normalize incoming URLs from the same host as a target URL to the same
 * protocol (HTTP or HTTPS) as that target URL.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.75.4
 * @see HttpHttpsUrlHelper
 */
public class HttpHttpsParamUrlNormalizer implements UrlNormalizer {

  protected String[] params;
  
  /**
   * <p>
   * Makes a new instance, based on the given plugin parameter keys.
   * </p>
   * 
   * @param params
   *          Plugin parameter keys, e.g.
   *          {@code ConfigParamDescr.BASE_URL.getKey()} or
   *          {@code "download_url"}.
   * @since 1.75.4
   */
  public HttpHttpsParamUrlNormalizer(String... params) {
    this.params = params;
  }
  
  @Override
  public String normalizeUrl(String url, ArchivalUnit au) {
    return new HttpHttpsUrlHelper(au, params).normalize(url);
  }

}
