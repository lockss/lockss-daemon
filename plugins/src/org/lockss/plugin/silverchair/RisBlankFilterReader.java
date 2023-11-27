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

package org.lockss.plugin.silverchair;

import org.apache.commons.lang3.StringUtils;
import org.lockss.filter.RisFilterReader;
import org.lockss.util.Logger;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;

/**
 * This filter will use an updated RisFilterReader
 * but until we respin the daemon, need to remove blank lines with this version
 */
public class RisBlankFilterReader extends RisFilterReader {
  private static final Logger log = Logger.getLogger(RisBlankFilterReader.class);

  public RisBlankFilterReader(InputStream inputStream, String encoding, String... tags)
			throws UnsupportedEncodingException {
		super(inputStream, encoding, tags);
	}
  
  @Override
  public String rewriteLine(String line) {
	  // filter out blank lines as well - unimportant for hashed comparison
	if (StringUtils.isBlank(line)) {
		return null;
	}
    Matcher mat = tagPattern.matcher(line);
    if (mat.find()) {
      String tag = getTag(mat);
      removingTag = tagSet.contains(tag);
    }
    return removingTag ? null : line;
  
  }
}
