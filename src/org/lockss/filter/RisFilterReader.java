/*
 * $Id: RisFilterReader.java,v 1.2 2014-11-12 00:13:20 thib_gc Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.filter;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.commons.io.input.ReaderInputStream;
import org.lockss.util.LineRewritingReader;

/**
 * <p>
 * Pre-processes RIS files by filtering out given fields.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.66
 */
public class RisFilterReader extends LineRewritingReader {

  /**
   * <p>
   * Pattern to recognize tag lines.
   * </p>
   * 
   * @since 1.66
   */
  protected static final Pattern TAG_PATTERN = Pattern.compile("^([A-Z][A-Z0-9])  - ");
  
  /**
   * <p>
   * Set of tags that are to be filtered out.
   * </p>
   * 
   * @since 1.66
   */
  protected Set<String> tagSet;
  
  /**
   * <p>
   * A flag denoting that the removal of a field is in progress.
   * </p>
   * 
   * @since 1.66
   */
  protected boolean removingTag;
  
  /**
   * <p>
   * Builds a new filter from the given input stream and encoding, removing the
   * given tags.
   * </p>
   * 
   * @param inputStream
   *          A RIS input stream.
   * @param encoding
   *          The encoding of the input stream.
   * @param tags
   *          Zero or more tags to be removed.
   * @throws UnsupportedEncodingException
   *           if the encoding is not supported
   * @since 1.66
   */
  public RisFilterReader(InputStream inputStream, String encoding, String... tags)
      throws UnsupportedEncodingException {
    this(new InputStreamReader(inputStream, encoding), tags);
  }
  
  /**
   * <p>
   * Builds a new filter from the given reader, removing the given tags.
   * </p>
   * 
   * @param reader
   *          A RIS reader.
   * @param tags
   *          Zero or more tags to be removed.
   * @since 1.66
   */
  public RisFilterReader(Reader reader, String... tags) {
    super(reader);
    this.tagSet = new HashSet<String>();
    addTags(tags);
    this.removingTag = false;
  }
  
  /**
   * <p>
   * Adds tags top the set of tags to be removed.
   * </p>
   * 
   * @param tags Zero or more additional tags to be removed.
   * @since 1.66
   */
  public void addTags(String... tags) {
    for (String tag : tags) {
      tagSet.add(tag);
    }
  }
  
  @Override
  public String rewriteLine(String line) {
    Matcher mat = TAG_PATTERN.matcher(line);
    if (mat.find()) {
      String tag = mat.group(1);
      removingTag = tagSet.contains(tag);
    }
    return removingTag ? null : line;
  }
  
  /**
   * <p>
   * A convenience method to turn this {@link Reader} back into an
   * {@link InputStream}.
   * </p>
   * 
   * @param encoding
   *          An output encoding.
   * @return This {@link Reader} as an {@link InputStream}.
   * @since 1.66
   */
  public InputStream toInputStream(String encoding) {
    // The ReaderInputStream from Commons IO, not org.lockss.util
    return new ReaderInputStream(this, encoding);
  }

}
