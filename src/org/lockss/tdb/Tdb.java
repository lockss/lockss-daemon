/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University,
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.tdb;

import java.io.Serializable;
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
public class Tdb implements Serializable {
  
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
