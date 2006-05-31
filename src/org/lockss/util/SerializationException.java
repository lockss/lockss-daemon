/*
 * $Id: SerializationException.java,v 1.1 2006-05-31 17:54:49 thib_gc Exp $
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

package org.lockss.util;

import java.io.*;

/**
 * <p>Denotes marshalling/unmarshalling error conditions.</p>
 * <p>By convention, {@link ObjectSerializer} throws either
 * {@link SerializationException}, or one of a possible set of
 * distinguished exceptions (see {@ObjectSerializer}'s documentation
 * for details).</p>
 * @author Thib Guicherd-Callin
 * @see ObjectSerializer
 */
public class SerializationException extends Exception {

  /**
   * <p>A {@link SerializationException} for the specific case when
   * a call to {@link InputStream#close}, {@link OutputStream#close},
   * {@link Reader#close} or {@link Writer#close} fails.</p>
   * @author Thib Guicherd-Callin
   */
  public static class CloseFailed extends SerializationException {

    public CloseFailed() {
      super();
    }

    public CloseFailed(String message) {
      super(message);
    }

    public CloseFailed(String message, Throwable cause) {
      super(message, cause);
    }

    public CloseFailed(Throwable cause) {
      super(cause);
    }

  }

  /**
   * <p>A {@link SerializationException} for failures to copy a
   * file to another file.</p>
   * @author Thib Guicherd-Callin
   */
  public static class CopyFailed extends SerializationException {

    public CopyFailed() {
      super();
    }

    public CopyFailed(String message) {
      super(message);
    }

    public CopyFailed(String message, Throwable cause) {
      super(message, cause);
    }

    public CopyFailed(Throwable cause) {
      super(cause);
    }

  }

  /**
   * <p>A {@link SerializationException} analogous to
   * {@link java.io.FileNotFoundException}.</p>
   * @author Thib Guicherd-Callin
   */
  public static class FileNotFound extends SerializationException {

    public FileNotFound() {
      super();
    }

    public FileNotFound(String message) {
      super(message);
    }

    public FileNotFound(String message, Throwable cause) {
      super(message, cause);
    }

    public FileNotFound(Throwable cause) {
      super(cause);
    }

  }

  /**
   * <p>A {@link SerializationException} analogous to
   * {@link java.io.NotSerializableException}.</p>
   * @author Thib Guicherd-Callin
   */
  public static class NotSerializableOrLockssSerializable extends SerializationException {

    public NotSerializableOrLockssSerializable() {
      super();
    }

    public NotSerializableOrLockssSerializable(String message) {
      super(message);
    }

    public NotSerializableOrLockssSerializable(String message, Throwable cause) {
      super(message, cause);
    }

    public NotSerializableOrLockssSerializable(Throwable cause) {
      super(cause);
    }

  }

  /**
   * <p>A {@link SerializationException} for the specific case when
   * a call to {@link File#renameTo} fails.</p>
   * @author Thib Guicherd-Callin
   */
  public static class RenameFailed extends SerializationException {

    public RenameFailed() {
      super();
    }

    public RenameFailed(String message) {
      super(message);
    }

    public RenameFailed(String message, Throwable cause) {
      super(message, cause);
    }

    public RenameFailed(Throwable cause) {
      super(cause);
    }

  }

  public SerializationException() {
    super();
  }

  public SerializationException(String message) {
    super(message);
  }

  public SerializationException(String message, Throwable cause) {
    super(message, cause);
  }

  public SerializationException(Throwable cause) {
    super(cause);
  }

}