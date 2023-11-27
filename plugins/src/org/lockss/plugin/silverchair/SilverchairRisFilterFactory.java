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


import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.filter.RisFilterReader;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;

import java.io.*;
import java.util.regex.Pattern;

/*
 * Some RIS files have a line that contains the creation date of the file in the form:
 * Y2 - 03/23/2023, which changes based on current download date
 * Reads through RIS files line by line and removes the line with the start tag Y2
 */

/*
Sample Silvearchair books ris file:

Provider: Silverchair
Database: GeoScienceWorld
Content: text/plain; charset="UTF-8"

TY  - ECHAP
T1  - Front Matter
PY  - 1985
Y1  - 1985/01/01
T2  - Proceedings of the International Clay Conference Denver, 1985
A2  - Schultz, Leonard G.
A2  - Olphen, H. van
A2  - Mumpton, Frederick A.
SP  - 0
PB  - Clay Minerals Society
SN  - 9780935868291
Y2  - 3/24/2023
ER  -
 */
public class SilverchairRisFilterFactory implements FilterFactory {
  private static Logger log = Logger.getLogger(SilverchairRisFilterFactory.class);

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
      InputStream in,
      String encoding)
          throws PluginException {

    InputStream inBuf = null; // to make sure mark() is supported
    /* 
     * RIS files are collected with content type text/plain (encoding) and so
     * we have to filter all text/plain and then determine if they're a RIS file 
     * here.  We are working on a different long-term solution by allowing us to verify
     * the URL against a regexp.
     *
     */

    BufferedReader bReader;

    if (in.markSupported() != true) {
      log.debug3("goes here====in.markSupported() != true");
      inBuf = new BufferedInputStream(in); //wrap the one that came in
    } else {
      log.debug3("goes here====inBuf =  in;");
      inBuf =  in; //use the one passed in
    }
    int BUF_LEN = 2000;
    inBuf.mark(BUF_LEN); // not sure about the limit...just reading far enough to identify file type

    try {
      //Now create a BoundedInputReader to make sure that we don't overstep our reset mark
      bReader = new BufferedReader(new InputStreamReader(new BoundedInputStream(inBuf, BUF_LEN), encoding));
      
      String aLine = bReader.readLine();

      while (aLine != null && StringUtils.isBlank(aLine)) {
        aLine = bReader.readLine(); // get the next line
      }
      
      // do NOT close bReader - it would also close underlying inBuf!
      inBuf.reset();
      // if we have  data, see if it matches the RIS pattern
      if (aLine != null) {
        return getRisFilterReader(encoding, inBuf).toInputStream(encoding);
      }
      return inBuf; // If not a RIS file, just return reset file
    } catch (UnsupportedEncodingException e) {
      log.debug2("Internal error (unsupported encoding)", e);
      throw new PluginException("Unsupported encoding looking ahead in input stream", e);
    } catch (IOException e) {
      log.debug2("Internal error (IO exception)", e);
      throw new PluginException("IO exception looking ahead in input stream", e);
    }
  }

  /* In addition to Y2 which often changes with download
   * UR is now often changes from http to https 
   * and if it's DOI it might go from dx.doi.org to doi.org - just remove it
   */
  protected RisFilterReader getRisFilterReader(String encoding, InputStream inBuf)
      throws UnsupportedEncodingException {
    return new RisBlankFilterReader(inBuf, encoding, "Y2","UR");
  }

}
