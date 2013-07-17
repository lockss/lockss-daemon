/*
 * $Id: FitsUtil.java,v 1.1.2.1 2013-07-17 10:12:47 easyonthemayo Exp $
 */

/*

Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.util;

import de.schlichtherle.truezip.file.TFile;
import edu.harvard.hul.ois.fits.Fits;
import edu.harvard.hul.ois.fits.FitsMetadataElement;
import edu.harvard.hul.ois.fits.FitsOutput;
import edu.harvard.hul.ois.fits.exceptions.FitsConfigurationException;
import edu.harvard.hul.ois.fits.exceptions.FitsException;
import edu.harvard.hul.ois.fits.identity.ExternalIdentifier;
import edu.harvard.hul.ois.fits.identity.FitsIdentity;
import edu.harvard.hul.ois.fits.tools.ToolInfo;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.CachedUrl;
import org.lockss.truezip.TFileCache;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utilities to use FITS to recognise content types of files and streams.
 * Note FITS_HOME env var must be set.
 * @author Neil Mayo
 */
public class FitsUtil {

  protected static Logger log = Logger.getLogger("FitsUtil");

  static final String PREFIX = Configuration.PREFIX + "fits.";
  /** Param: save the FITS analyses to file. */
  public static final String PARAM_SAVE_ANALYSES = PREFIX + "saveAnalyses";
  public static final boolean DEFAULT_SAVE_ANALYSES = false;

  /** The string returned when a content type could not be determined. */
  public static final String UNKNOWN_TYPE = "unknown";
  /** The string returned when an AU is null. */
  public static final String UNKNOWN_AU = "unknown AU";
  /** A default prefix for temporary files created as a destination for streams. */
  protected static final String DEFAULT_TEMP_FILE_PREFIX = "FitsAnalysisContent";
  /** The prefix for URL to the National Archives PRONOM tool. Just append fmt/NN. */
  protected static final String PRONOM_PREFIX = "http://www.nationalarchives.gov.uk/PRONOM/";
  /** Acknowledgement to FITS tool. */
  public static final String FITS_ACK = "Report generated using the File Information Tool Set (FITS).";
  public static final String FITS_URL = "https://code.google.com/p/fits/";
  public static final String FITS_ACK_HTML = String.format(
      "Report generated using the <a href=\"%s\">File Information Tool Set (FITS)</a>.",
      FITS_URL
  );

  public static String fitsHome;


  /** Shared instance of FITS; it's not clear whether this object is thread safe,
   * so we should synchronize on its usage. */
  private static Fits fits;
  /** A temp file cache to manage files of dumped CachedUrl streams. */
  private static TFileCache tFileCache;
  /** Any error message generated when instantiating the FITS object. */
  private static String errorMsg;

  /** Intialise the FITS object and temp file cache. */
  static {
    // First try and get the FITS home dir and create a FITS instance
    try {
      // Could throw NPE or SecurityException
      String s = org.apache.commons.lang.StringUtils.class
          .getProtectionDomain().getCodeSource().getLocation().getPath();
      fitsHome = s.substring(0, s.lastIndexOf(File.separator)).concat(File.separator).concat("fits");
      log.debug("Using FITS_HOME: "+fitsHome);
      // Could throw FitsConfigurationException
      fits = new Fits(fitsHome);
    } catch (RuntimeException e) {
      fitsHome = null;
      // The arg-less ctr looks for env var FITS_HOME:
      try {
        log.debug("Relying on FITS_HOME in environment variable");
        fits = new Fits();
      } catch (FitsException e1) {
        e1.printStackTrace();
        errorMsg = e.getMessage();
        fits = null;
      }
    } catch (FitsConfigurationException e) {
      e.printStackTrace();
      errorMsg = e.getMessage();
    }

    /*
    try {
      tFileCache = new TFileCache(FileUtil.createTempDir("fits", null));
    } catch (IOException e) {
      e.printStackTrace();
      errorMsg = e.getMessage();
      tFileCache = null;
    }
    */
  }


  /////////////////////////////////////////////////////////////////////////////

  /**
   * Get the filepath of the analysed file.
   * @param fitsOut
   * @return
   */
  public static final String getFilepath(FitsOutput fitsOut) {
    for (FitsMetadataElement el : fitsOut.getFileInfoElements()) {
      if ("filepath".equals(el.getName())) {
        return el.getValue();
      }
    }
    return null;
  }


  /**
   * Perform FITS analysis on a particular URL within an AU.
   * @param au an ArchivalUnit
   * @param url a URL string identifying a URL within the AU
   * @return a content type description
   */
  public static final String getContentType(ArchivalUnit au, String url) {
    if (au==null) return FitsUtil.UNKNOWN_AU;
    try {
      final FitsOutput fitsOut = doFitsAnalysis(au, url);
      return getContentType(fitsOut);
    } catch (FitsException e) {
      log.warning("FITS analysis error: "+e);
      e.printStackTrace();
    } catch (IOException e) {
      log.warning("Stream could not be written to file: "+e);
      e.printStackTrace();
    } catch (NullPointerException e) {
      log.warning("Could not find the requested AU cached URL: "+e);
      e.printStackTrace();
    }
    return UNKNOWN_TYPE;
  }

  /**
   * Perform FITS analysis on an ArticleFiles and return only the recognized
   * content type.
   * @param inputStream an input stream with the content
   * @return a content type
   */
  public static final String getContentType(ArticleFiles file) {
    try {
      final FitsOutput fitsOut = doFitsAnalysis(file.getFullTextCu().getUnfilteredInputStream());
      //final FitsOutput fitsOut = doFitsAnalysis(file.getFullTextCu());
      return getContentType(fitsOut);
    } catch (FitsException e) {
      log.warning("FITS analysis error: "+e);
      e.printStackTrace();
    } catch (IOException e) {
      log.warning("Stream could not be written to file: "+e);
      e.printStackTrace();
    }
    return UNKNOWN_TYPE;
  }
  
  /**
   * Perform FITS analysis on an input stream and return only the recognized
   * content type.
   * @param inputStream an input stream with the content
   * @return a content type
   */
  public static final String getContentType(InputStream inputStream) {
    try {
      return getContentType(doFitsAnalysis(inputStream));
    } catch (FitsException e) {
      e.printStackTrace();
      return UNKNOWN_TYPE;
    } catch (IOException e) {
      e.printStackTrace();
      return UNKNOWN_TYPE;
    }
  }


  /**
   * Perform FITS analysis on a File and return only the recognized content type.
   * @param file a File with the content
   * @return a content type
   */
  public static final String getContentType(File file) {
    try {
      return getContentType(doFitsAnalysis(file));
    } catch (FitsException e) {
      e.printStackTrace();
      return UNKNOWN_TYPE;
    }
  }


  /**
   * Get a description of the content type from a FitsOutput result.
   * If there is a conflict,
   *
   * @param fitsOut the result of a FITS analysis
   * @return a content type
   */
  public static final String getContentType(FitsOutput fitsOut) {
    try {
      if (fitsOut==null) return UNKNOWN_TYPE;
      if (fitsOut.getIdentities().size()!=0) {
        FitsIdentity ident = fitsOut.getIdentities().get(0);
        /*for (FitsIdentity id : fitsOut.getIdentities()) {
          sb.append(id.getFormat()+" ");
        }*/
        return String.format("%s (%s)", ident.getMimetype(), ident.getFormat());
      }
      return UNKNOWN_TYPE;
    } catch(Exception e) {
      return UNKNOWN_TYPE;
    }
  }

  /**
   * Produce a PRONOM URL if there is a "fmt/NN" external identifier entry
   * from DROID in any of the identities.
   * @param fitsOut
   * @return
   */
  public static final String getPronomUrl(FitsOutput fitsOut) {
    // Take the first non-null external identifier value
    for (FitsIdentity fi : fitsOut.getIdentities()) {
      List<ExternalIdentifier> ids = fi.getExternalIdentifiers();
      if (ids.size()>0) {
        String fmt = ids.get(0).getValue();
        if (!StringUtil.isNullString(fmt)) return PRONOM_PREFIX + fmt;
      }
    }
    return "";
  }

  /**
   * Get the MIME-type from the first identity in the output.
   * @param fitsOut
   * @return
   */
  public static final String getMimeType(FitsOutput fitsOut) {
    try {
      // Take the first result
      return fitsOut.getIdentities().get(0).getMimetype();
    } catch (Exception e) {
      return "";
    }
  }

  /**
   * Perform FITS analysis on a File. Synchronizes on use of the FITS object,
   * and saves the result to a temporary file if possible, for debugging or
   * later use.
   * @param file a File with the content
   * @return a FitsOutput object
   * @throws FitsException if the analysis could not be completed
   */
  public static final FitsOutput doFitsAnalysis(File file) throws FitsException {
    long s1 = TimeBase.nowMs();
    FitsOutput fitsOut;
    synchronized(fits) {
      try {
        fitsOut = fits.examine(file);
      } catch (NullPointerException e) {
        throw new FitsException("FITS not initalised", e);
      }
    }
    // File-level logging
    log.debug(String.format("FITS took %s to analyse %s",
        StringUtil.timeIntervalToString(TimeBase.msSince(s1)),
        file.getName()
    ));
    // Save the result to temp file
    if (isSaveFitsAnalyses()) {
      try {
        String f = FileUtil.createTempFile(DEFAULT_TEMP_FILE_PREFIX, ".xml").toString();
        fitsOut.saveToDisk(f);
        log.info(String.format("Saved FITS results to file %s", f));
      } catch (IOException e) {
        log.warning(String.format("FITS results could not be saved to file: %s", e));
      }
    }
    return fitsOut;
  }


  /**
   * Perform FITS analysis on an input stream. Writes the stream to a temporary
   * file and analyses the file, then deletes it.
   * @param inputStream an input stream with the content
   * @return a FitsOutput object
   * @throws IOException if the stream could not be written to file
   * @throws FitsException if the analysis could not be completed
   */
  public static final FitsOutput doFitsAnalysis(InputStream inputStream) throws IOException, FitsException {
    File tmpFile = getFile(inputStream);
    FitsOutput fitsOut = doFitsAnalysis(tmpFile);
    tmpFile.delete();
    return fitsOut;
  }

  // TODO Before this method is usable, need to adapt TFileCache to handle non-archive files
  /*public static final FitsOutput doFitsAnalysis(CachedUrl cachedUrl) throws IOException, FitsException {
    File tmpFile = tFileCache.getCachedTFile(cachedUrl);
    FitsOutput fitsOut = doFitsAnalysis(tmpFile);
    tmpFile.delete();
    return fitsOut;
  }*/

  /**
   * Perform FITS analysis on a particular URL within an AU.
   * @param au an ArchivalUnit
   * @param url a URL string identifying a URL within the AU
   * @return a FitsOutput object
   * @throws IOException if the URL's stream could not be written to file
   * @throws FitsException if the analysis could not be completed
   */
  public static final FitsOutput doFitsAnalysis(ArchivalUnit au, String url) throws IOException, FitsException {
    CachedUrl cu = au.makeCachedUrl(url);
    return doFitsAnalysis(cu.getUnfilteredInputStream());
    //return doFitsAnalysis(cu);
  }


  /**
   * Redirect an input stream to a temp file, and return a reference to the file.
   * @param inputStream an input stream with the content
   * @param prefix a prefix to use for the temp file
   * @return a reference to the temporary File
   * @throws IOException if the stream could not be written to file
   */
  public static final File getFile(InputStream inputStream, String prefix) throws IOException {
    // Note specifying a file extension suffix will influence whether the FITS
    // tools are run, so do not include a suffix.
    File tmpFile = File.createTempFile(prefix, null);
    // write the inputStream to a FileOutputStream
    FileOutputStream outputStream = new FileOutputStream(tmpFile);
    int read = 0;
    byte[] bytes = new byte[1024];
    while ((read = inputStream.read(bytes)) != -1) {
      outputStream.write(bytes, 0, read);
    }
    outputStream.flush();
    outputStream.close();
    inputStream.close();
    return tmpFile;
  }

  /**
   * Redirect an input stream to a temp file, and return a reference to the file.
   * A default file prefix is used.
   * @param inputStream an input stream with the content
   * @return a reference to the temporary File
   * @throws IOException if the stream could not be written to file
   */
  public static final File getFile(InputStream inputStream) throws IOException {
    return getFile(inputStream, DEFAULT_TEMP_FILE_PREFIX);
  }

  /**
   * Whether the config option to save all FITS analyses to file as XML is true.
   * @return
   */
  public static boolean isSaveFitsAnalyses() {
    return CurrentConfig.getBooleanParam(PARAM_SAVE_ANALYSES, DEFAULT_SAVE_ANALYSES);
  }

  /**
   * Check whether there are archived files available for the specified AU.
   * @param au
   * @return
   */
  public static boolean areAuFilesAvailable(ArchivalUnit au) {
    Iterator<CachedUrl> it = au.getAuCachedUrlSet().archiveMemberIterator();
    return it != null && it.hasNext();
  }


  /**
   * Get a list of descriptions of the tools that made an identification.
   * @param id an identity element from the FITS results
   * @return list of string describing the toole
   */
  private static List<String> getToolsForId(FitsIdentity id) {
    List<String> toolStrings = new ArrayList<String>();
    for (ToolInfo tool: id.getReportingTools()) {
      toolStrings.add(String.format(
          "%s (v%s) ", tool.getName(), tool.getVersion()
      ));
    }
    return toolStrings;
  }


}
