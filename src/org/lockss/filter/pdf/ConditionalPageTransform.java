/*
 * $Id: ConditionalPageTransform.java,v 1.2 2006-09-14 23:10:39 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.filter.pdf;

import org.lockss.filter.pdf.PageTransformUtil.*;
import org.lockss.util.PdfUtil.ResultPolicy;

/**
 * <p>A page transform decorator that applies a "then" page
 * transform only if the PDF page to be transformed is recognized
 * by an "if" page transform.</p>
 * @author Thib Guicherd-Callin
 */
public class ConditionalPageTransform extends PageTransformDecorator {

  /**
   * <p>Builds a new conditional page transform using the given
   * strictness, out of the given "if" page transform and
   * "then" page transform.</p>
   * @param ifTransform    An "if" page transform.
   * @param thenStrictness True to wrap the "then" page transform
   *                       as a {@link StrictPageTransform} if it
   *                       is not one already, false otherwise.
   * @param thenTransform  A "then" page transform.
   * @see PageTransformDecorator#PageTransformDecorator(PageTransform)
   * @see AggregatePageTransform#AggregatePageTransform(PageTransform, PageTransform)
   */
  public ConditionalPageTransform(PageTransform ifTransform,
                                  boolean thenStrictness,
                                  PageTransform thenTransform) {
    super(new AggregatePageTransform(ifTransform,
                                     !thenStrictness || thenTransform instanceof StrictPageTransform
                                     ? thenTransform
                                     : new StrictPageTransform(thenTransform)));
  }

  /**
   * <p>Builds a new conditional page transform using the given
   * strictness, out of the given "if" page transform and the
   * aggregation of the given "then" page transforms (using the
   * default aggregation result policy).</p>
   * @param ifTransform    An "if" page transform.
   * @param thenStrictness True to wrap the aggregated "then" page
   *                       transform as a
   *                       {@link StrictPageTransform}, false
   *                       otherwise.
   * @param thenTransform1  A "then" page transform.
   * @param thenTransform2  A "then" page transform.
   * @see #ConditionalPageTransform(PageTransform, boolean, PageTransform)
   * @see AggregatePageTransform#AggregatePageTransform(PageTransform, PageTransform)
   * @see AggregatePageTransform#POLICY_DEFAULT
   */
  public ConditionalPageTransform(PageTransform ifTransform,
                                  boolean thenStrictness,
                                  PageTransform thenTransform1,
                                  PageTransform thenTransform2) {
    this(ifTransform,
         thenStrictness,
         new AggregatePageTransform(thenTransform1,
                                    thenTransform2));
  }

  /**
   * <p>Builds a new conditional page transform using the given
   * strictness, out of the given "if" page transform and the
   * aggregation of the given "then" page transforms (using the
   * given aggregation result policy).</p>
   * @param ifTransform      An "if" page transform.
   * @param thenStrictness   True to wrap the aggregated "then"
   *                        page transform as a
   *                         {@link StrictPageTransform}, false
   *                         otherwise.
   * @param thenResultPolicy A result policy for the aggregate "then"
   *                         page transform.
   * @param thenTransform1   A "then" page transform.
   * @param thenTransform2   A "then" page transform.
   * @see #ConditionalPageTransform(PageTransform, boolean, PageTransform)
   */
  public ConditionalPageTransform(PageTransform ifTransform,
                                  boolean thenStrictness,
                                  ResultPolicy thenResultPolicy,
                                  PageTransform thenTransform1,
                                  PageTransform thenTransform2) {
    this(ifTransform,
         thenStrictness,
         new AggregatePageTransform(thenResultPolicy,
                                    thenTransform1,
                                    thenTransform2));
  }

  /**
   * <p>Builds a new conditional page transform using the default
   * strictness, out of the given "if" page transform and
   * "then" page transform.</p>
   * @param ifTransform    An "if" page transform.
   * @param thenTransform  A "then" page transform.
   * @see #ConditionalPageTransform(PageTransform, boolean, PageTransform)
   * @see #STRICTNESS_DEFAULT
   */
  public ConditionalPageTransform(PageTransform ifTransform,
                                  PageTransform thenTransform) {
    this(ifTransform,
         STRICT_DEFAULT,
         thenTransform);
  }

  /**
   * <p>Builds a new conditional page transform using the default
   * strictness, out of the given "if" page transform and the
   * aggregation of the given "then" page transforms (using the
   * default aggregation result policy).</p>
   * @param ifTransform      An "if" page transform.
   * @param thenTransform1   A "then" page transform.
   * @param thenTransform2   A "then" page transform.
   * @see #ConditionalPageTransform(PageTransform, boolean, PageTransform, PageTransform)
   * @see #STRICTNESS_DEFAULT
   */
  public ConditionalPageTransform(PageTransform ifTransform,
                                  PageTransform thenTransform1,
                                  PageTransform thenTransform2) {
    this(ifTransform,
         STRICT_DEFAULT,
         thenTransform1,
         thenTransform2);
  }

  /**
   * <p>Builds a new conditional page transform using the default
   * strictness, out of the given "if" page transform and the
   * aggregation of the given "then" page transforms (using the
   * given aggregation result policy).</p>
   * @param ifTransform    An "if" page transform.
   * @param thenResultPolicy A result policy for the aggregate "then"
   *                         page transform.
   * @param thenTransform1  A "then" page transform.
   * @param thenTransform2  A "then" page transform.
   * @see #ConditionalPageTransform(PageTransform, boolean, ResultPolicy, PageTransform, PageTransform)
   * @see #STRICTNESS_DEFAULT
   */
  public ConditionalPageTransform(PageTransform ifTransform,
                                  ResultPolicy thenResultPolicy,
                                  PageTransform thenTransform1,
                                  PageTransform thenTransform2) {
    this(ifTransform,
         STRICT_DEFAULT,
         thenResultPolicy,
         thenTransform1,
         thenTransform2);
  }

  public static final boolean STRICT_DEFAULT = true;

}