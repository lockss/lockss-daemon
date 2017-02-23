/*
 * Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
 * all rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Stanford University shall not
 * be used in advertising or otherwise to promote the sale, use or other dealings
 * in this Software without prior written authorization from Stanford University.
 *
 */

package org.lockss.util;

import org.lockss.daemon.PluginException;
import org.lockss.extractor.LinkExtractor;
import org.lockss.plugin.ArchivalUnit;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import static org.lockss.util.StringUtil.isNullString;

/**
 * A Wrapper around the Data Uri as defined by
 * <a href="http://www.ietf.org/rfc/rfc2397.txt">RFC2397</a>.
 * The schema as defined:
 * <pre>
 *  dataurl    := "data:" [ mediatype ] [ ";base64" ] "," data
 *  mediatype  := [ type "/" subtype ] *( ";" parameter )
 *  data       := *urlchar
 *  parameter  := attribute "=" value
 * </pre>
 *
 * A data uri may be found in various media examples from html:
 * <pre>
 *   &lt;img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA
 *   AAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO
 *   9TXL0Y4OHwAAAABJRU5ErkJggg==" alt="Red dot" /&gt;
 * </pre>
 *  Other varients include
 * <pre>
 *  data:,Hello%2C%20World!
 *  data:text/plain;base64,SGVsbG8sIFdvcmxkIQ%3D%3D
 *  data:text/html,&lt;script&gt;alert('hi');&lt;/script&gt;
 * </pre>
 *
 * from css:
 * <pre>
 *  body {
 *    background-image:url('data:image/png;base64, SGVsbG8sIFdvcmxkIQ%3D%3D...')
 *  }
 * 
 *  font-face {
 *  font-family: 'customFont';
 *  src: url(data:font/svg;charset=utf-8;base64,PD94bWwgdmV...)
 * </pre>
 *
 * from javascript:
 * <pre>
 *  window.open('data:text/html;charset=utf-8,' +
 *    encodeURIComponent( // Escape for URL formatting
 *      '&lt;!DOCTYPE html&gt;'+
 *      '&lt;html lang="en"&gt;'+
 *      '&lt;head&gt;&lt;title&gt;Embedded Window&lt;/title&gt;&lt;/head&gt;'+
 *      '&lt;body&gt;&lt;h1&gt;42&lt;/h1&gt;&lt;/body&gt;'+
 *      '&lt;/html&gt;'
 *     )
 *   );
 *  </pre>
 * @author: claire griffin date: 2016-04-06.
 */
public class DataUri {

  /**
   * The default media value when none is given
   */
  public static final String DEFAULT_MEDIA = "text/plain;charset=US-ASCII";
  /**
   * The default value if no mime type is found as defined by RFC2397.
   */
  public static final String DEFAULT_MIMETYPE = "text/plain";

  /**
   * The default value if no charset is given for text media as defined by RFC2397.
   */
  public static final String DEFAULT_CHARSET = "US-ASCII";

  /**
   * Regular expression to match a Data URI
   */
  public static final String DATA_URI_RE = "^data:(.*?);?(base64)?,(.*)";


  /**
   * Regular expression to match the mime type within a data url
   */
  public static final String MIME_TYPE_RE = "([-\\w.+]+/[-\\w.+]*)";

  /**
   * Regular expression to match the array of parameters in form of param=value
   */
  public static final String MIME_PARAM_RE = ";([-\\w.+]+)(=)?([^;,]+)?";

  protected static Logger log = Logger.getLogger(DataUri.class.getName());

  private static final Pattern DATA_URI_PATTERN = Pattern.compile(DATA_URI_RE, Pattern.CASE_INSENSITIVE);
  private static final Pattern MIME_TYPE_PATTERN = Pattern.compile(MIME_TYPE_RE);
  private static final Pattern MIME_PARAM_PATTERN = Pattern.compile(MIME_PARAM_RE);
  private static final Pattern IS_DATA_URI_PATTERN = Pattern.compile("^data:.*", Pattern.CASE_INSENSITIVE);
  private String mimeType;
  private String charsetName;
  private Properties mediaParams = new Properties();
  private String data;
  private boolean useBase64;
  private Charset charset;


  private DataUri(Builder builder) {
    mimeType = builder.mimeType;
    charsetName = builder.charsetName;
    mediaParams = builder.mediaParams;
    data = builder.data;
    useBase64 = builder.useBase64;
    charset = Charset.forName(charsetName);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public String getMimeType() {
    return mimeType;
  }

  public String getCharset() {
    return charsetName;
  }

  public Properties getMediaParams() {
    return mediaParams;
  }

  public String getData() { return data; }

  public boolean usesBase64() {
    return useBase64;
  }


  /**
   * Returns true iff the string begins with data:
   *
   * @param uri the string to test.
   * @return true if this a data uri.
   */
  public static boolean isDataUri(String uri) {

    Matcher matcher = IS_DATA_URI_PATTERN.matcher(uri);
    return matcher.matches();
  }

  /**
   * Returns true iff the string conforms to format of a data url
   *
   * @param uri the string to test.
   * @return true if this a data uri.
   */
  public static boolean isValidDataUri(String uri) {
    Matcher matcher = DATA_URI_PATTERN.matcher(uri);
    return matcher.matches();
  }


  /**
   * Turn a data uri string int a DataUri object which can be queried for the component parts.
   *
   * @param uri the data uri
   * @return a newly created DataUri
   */
  public static DataUri makeDataUri(String uri)  {
    if (isNullString(uri) && !isDataUri(uri)) {
      return null;
    }
    String mime_type = null;
    boolean usesBase64;
    Properties props = new Properties();
    String media;
    Builder builder  = new Builder();
    Matcher urimatcher = DATA_URI_PATTERN.matcher(uri);

    if(urimatcher.matches()) {
      // group 1 = media (mime-type and parameters)
      media = urimatcher.group(1);
      if(isNullString(media)) {
        media = DEFAULT_MEDIA;
      }
      Matcher mimematcher = MIME_TYPE_PATTERN.matcher(media);
      if(mimematcher.find()) {
        mime_type = mimematcher.group();
      }
      Matcher param_matcher = MIME_PARAM_PATTERN.matcher(media);
      String key;
      String value = null;
      while (param_matcher.find()) {
        key = param_matcher.group(1).toLowerCase();
        if(param_matcher.group(2) != null)
          value = param_matcher.group(3);
        else if(key.equalsIgnoreCase("utf8")) {// used for font/svg
          key = "charset";
          value = Constants.ENCODING_UTF_8;
        }
        else {
          value = "";
        }
        if(key != null && value != null) {
          props.setProperty(key,value);
          log.debug3("property: " + key + "=" + value);
        }
      }

      builder.mimeType(mime_type).mediaParams(props).charset(props.getProperty("charset"));

      // group 2 = base64
      usesBase64 = urimatcher.group(2) != null;
      builder.useBase64(usesBase64);
      // group 3 = data
      builder.data(urimatcher.group(3));
      if(log.isDebug3()) {
        log.debug3("mime-type:" + mime_type);
        log.debug3("usesBase64:" + usesBase64);
        log.debug3("data: " + urimatcher.group(3));
      }
    }
    return builder.build();
  }


  /**
   * makeDataUri a data uri to a stream
   * @param uri the data uri to makeDataUri
   * @param os  the stream to wrtie to
   * @throws IOException  if stream is unwriteable
   */
  public static void decodeToStream(String uri, OutputStream os) throws IOException {
    DataUri d_uri = makeDataUri(uri);
    d_uri.decodeToStream(os);
  }

  /**
   * makeDataUri a data uri into a file
   * @param uri the data uri to makeDataUri
   * @param file  the file to write the decoded
   * @throws IOException  if file is unwrittable
   */
  public static void decodeToFile(String uri, File file) throws IOException {
    DataUri d_uri = makeDataUri(uri);
    d_uri.decodeToStream(new BufferedOutputStream(new FileOutputStream(file)));
  }

  /**
   * Call the appropriate link extractor defined by the plugin for the given
   * mime type.
   * @param uri the data uri to extract from
   * @param baseUri the base uri for resolving relative found links
   * @param au the AU to use for searching for an extractor
   * @param cb the callback to pass to the extractor
   */
  public static void dispatchToLinkExtractor(String uri,
                                  final String baseUri,
                                  final ArchivalUnit au,
                                  final LinkExtractor.Callback cb) {
    if(isValidDataUri(uri)) {
      DataUri d_uri = makeDataUri(uri);
      InputStream in = null;
      OutputStream out = null;
      File tmpfile = null;
      try {
        // check to see if we have a link extractor for this mime-type.
        LinkExtractor extractor = au.getLinkExtractor(d_uri.getMimeType());
        if (extractor != null) {
          tmpfile = FileUtil.createTempFile("d_uri", ".buf");
          // now we unpack the data
          out = new BufferedOutputStream(new FileOutputStream(tmpfile));
          decodeToStream(uri, out);
          out.flush();
          out.close();
          in = new BufferedInputStream(new FileInputStream(tmpfile));
          // pass it to the appropriate extractor
          extractor.extractUrls(au, in, d_uri.getCharset(), baseUri, cb);
        }
      } catch (IOException e) {
        log.debug3("IOException in extractor", e);
      } catch (PluginException e) {
        log.debug3("PluginException in extractor", e);
      } catch (IllegalStateException ise) {
        log.debug("Attempt to call data uri handler for non data uri");
      }
      finally {
        IOUtil.safeClose(in);
        IOUtil.safeClose(out);
        FileUtil.safeDeleteFile(tmpfile);
      }
    }
  }

  /**
   * decode to the stream passes in
   * @param os
   * @throws IOException
   */
  protected void decodeToStream(OutputStream os) throws IOException {
    DataOutputStream dos = new DataOutputStream(os);
    if(useBase64) {
      dos.write(DatatypeConverter.parseBase64Binary(data));
    }
    else {
      dos.writeBytes(URLDecoder.decode(data,charsetName));
    }
  }

  protected void decodeToFile(File file) throws IOException {
    OutputStream os = null;
    try {
      os = new BufferedOutputStream(new FileOutputStream(file));
      decodeToStream(os);
    }
    finally {
      if(os != null)
        os.close();
    }
  }


  /**
   * {@code DataUri} builder static inner class.
   */
  public static final class Builder {
    private String mimeType;
    private String charsetName;
    private Properties mediaParams;
    private String data;
    private boolean useBase64;

    private Builder() {
    }

    /**
     * Sets the {@code mimeType} and returns a reference to this Builder so that the methods can be
     * chained together.
     *
     * @param val the {@code mimeType} to set
     * @return a reference to this Builder
     */
    public Builder mimeType(String val) {
      mimeType = val;
      return this;
    }

    /**
     * Sets the {@code charset} and returns a reference to this Builder so that the methods can be
     * chained together.
     *
     * @param val the {@code charset} to set
     * @return a reference to this Builder
     */
    public Builder charset(String val) {
      charsetName = val;
      return this;
    }

    /**
     * Sets the {@code mediaParams} and returns a reference to this Builder so that the methods can
     * be chained together.
     *
     * @param val the {@code mediaParams} to set
     * @return a reference to this Builder
     */
    public Builder mediaParams(Properties val) {
      mediaParams = val;
      return this;
    }

    /**
     * Sets the {@code data} and returns a reference to this Builder so that the methods can be
     * chained together.
     *
     * @param val the {@code data} to set
     * @return a reference to this Builder
     */
    public Builder data(String val) {
      data = val;
      return this;
    }

    /**
     * Sets the {@code useBase64} and returns a reference to this Builder so that the methods can be
     * chained together.
     *
     * @param val the {@code useBase64} to set
     * @return a reference to this Builder
     */
    public Builder useBase64(boolean val) {
      useBase64 = val;
      return this;
    }

    /**
     * Returns a {@code DataUri} built from the parameters previously set.
     *
     * @return a {@code DataUri} built with parameters of this {@code DataUri.Builder}
     * @throws java.lang.IllegalStateException if we unable to make the class
     */
    public DataUri build() throws java.lang.IllegalStateException {
      if(data == null)
        throw new java.lang.IllegalStateException("No uri data to build");
      if(isNullString(charsetName) || !Charset.isSupported(charsetName))  { // unknown charsetName = DEFAULT charset
        charsetName = DEFAULT_CHARSET;
      }
      if(isNullString(mimeType)) { // unknown mime type is DEFAULT mime type
        mimeType = DEFAULT_MIMETYPE;
      }

      return new DataUri(this);
    }
  }
}
