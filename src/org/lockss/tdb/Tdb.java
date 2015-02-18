/*
 * $Id$
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

package org.lockss.tdb;

import java.util.*;

/**
 * <p>
 * A utility class holding the results of parsing TDB files.
 * </p>
 * <p>
 * Currently this class implements the same rudimentary behavior that was
 * standard in the Python tools -- publishers, titles and AUs are listed in the
 * order added (which is also the order parsed), doing no useful processing for
 * duplicates if any. 
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class Tdb {
  
  /**
   * <p>
   * The list of {@link Publisher} instances.
   * </p>
   * 
   * @since 1.67
   */
  protected List<Publisher> publishers;

  /**
   * <p>
   * The list of {@link Title} instances.
   * </p>
   * 
   * @since 1.67
   */
  protected List<Title> titles;

  /**
   * <p>
   * The list of {@link Au} instances.
   * </p>
   * 
   * @since 1.67
   */
  protected List<Au> aus;

  /**
   * <p>
   * Makes a new TDB structure.
   * </p>
   * 
   * @since 1.67
   */
  public Tdb() {
    this.publishers = new ArrayList<Publisher>();
    this.titles = new ArrayList<Title>();
    this.aus = new ArrayList<Au>();
  }
  
  /**
   * <p>
   * Appends a publisher to the list of publishers.
   * </p>
   * 
   * @param publisher
   *          A publisher.
   * @since 1.67
   */
  public void addPublisher(Publisher publisher) {
    publishers.add(publisher);
  }
  
  /**
   * <p>
   * Appends a title to the list of title.
   * </p>
   * 
   * @param title
   *          A title.
   * @since 1.67
   */
  public void addTitle(Title title) {
    titles.add(title);
  }
  
  /**
   * <p>
   * Appends an AU to the list of AUs.
   * </p>
   * 
   * @param au
   *          An AU.
   * @since 1.67
   */
  public void addAu(Au au) {
    aus.add(au);
  }
  
  /**
   * <p>
   * Gives an unmodifiable view of the list of publishers.
   * </p>
   * 
   * @return The list of publishers (unmodifiable).
   * @since 1.67
   */
  public List<Publisher> getPublishers() {
    return Collections.unmodifiableList(publishers);
  }
  
  /**
   * <p>
   * Gives an unmodifiable view of the list of titles.
   * </p>
   * 
   * @return The list of titles (unmodifiable).
   * @since 1.67
   */
  public List<Title> getTitles() {
    return Collections.unmodifiableList(titles);
  }
  
  /**
   * <p>
   * Gives an unmodifiable view of the list of AUs.
   * </p>
   * 
   * @return The list of AUs (unmodifiable).
   * @since 1.67
   */
  public List<Au> getAus() {
    return Collections.unmodifiableList(aus);
  }
  
}
