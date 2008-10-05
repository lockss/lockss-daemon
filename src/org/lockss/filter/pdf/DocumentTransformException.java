/*
 * $Id: DocumentTransformException.java,v 1.3 2007-02-23 19:41:34 thib_gc Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;

/**
 * <p>A properly-nestable {@link IOException} for exceptions raised
 * by document transforms.</p>
 * @author Thib Guicherd-Callin
 * @see DocumentTransform#transform
 */
public class DocumentTransformException extends IOException {

  /**
   * <p>Builds a new exception.</p>
   */
  public DocumentTransformException() {
    super();
  }

  /**
   * <p>Builds a new exception using the given message.</p>
   * @param message A detail message.
   */
  public DocumentTransformException(String message) {
    super(message);
  }

  /**
   * <p>Builds a new exception using the given message and cause.</p>
   * @param message A detail message.
   * @param cause   A {@link Throwable} cause.
   */
  public DocumentTransformException(String message, Throwable cause) {
    super(message);
    initCause(cause);
  }

  /**
   * <p>Builds a new exception using the given cause.</p>
   * @param cause A {@link Throwable} cause.
   */
  public DocumentTransformException(Throwable cause) {
    super();
    initCause(cause);
  }

}