package org.lockss.plugin.ojs3;

import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.PluginException;
import org.lockss.filter.RisFilterReader;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.FilterFactory;
import org.lockss.util.Logger;

import java.io.*;
import java.util.regex.Pattern;

/**
 * This is a copy-paste of atypon.RisFilterFactory
 * @author mark
 */
public class Ojs3RisHashFilterFactory implements FilterFactory {
  private static Logger log = Logger.getLogger(Ojs3RisHashFilterFactory.class);

  private static Pattern RIS_PATTERN = Pattern.compile("^\\s*TY\\s*-", Pattern.CASE_INSENSITIVE);

  @Override
  public InputStream createFilteredInputStream(ArchivalUnit au,
                                               InputStream in,
                                               String encoding)
      throws PluginException {

    InputStream inBuf; // to make sure mark() is supported
    /*
     * RIS files are collected with content type text/plain (encoding) and so
     * we have to filter all text/plain and then determine if they're a RIS file
     * here.  We are working on a different long-term solution by allowing us to verify
     * the URL against a regexp.
     */

    BufferedReader bReader;

    if (!in.markSupported()) {
      inBuf = new BufferedInputStream(in); //wrap the one that came in
    } else {
      inBuf =  in; //use the one passed in
    }
    int BUF_LEN = 2000;
    inBuf.mark(BUF_LEN); // not sure about the limit...just reading far enough to identify file type

    try {
      //Now create a BoundedInputReader to make sure that we don't overstep our reset mark
      bReader = new BufferedReader(new InputStreamReader(new BoundedInputStream(inBuf, BUF_LEN), encoding));

      String aLine = bReader.readLine();
      // The first tag in a RIS file must be "TY - "; be nice about WS
      // The specification doesn't allow for comments or other preceding characters

      // isBlank() checks if whitespace, empty or null
      // keep initial null check or you'd never exit loop if you hit the end of input!
      while (aLine != null && StringUtils.isBlank(aLine)) {
        aLine = bReader.readLine(); // get the next line
      }

      // do NOT close bReader - it would also close underlying inBuf!
      inBuf.reset();
      // if we have  data, see if it matches the RIS pattern
      if (aLine != null && RIS_PATTERN.matcher(aLine).find()) {
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