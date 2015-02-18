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

import java.io.*;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;

/**
 * <p>
 * ANTLR-related utilities.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.67
 */
public class AntlrUtil {

  /**
   * <p>
   * Prevent instantiation.
   * </p>
   * 
   * @since 1.67
   */
  private AntlrUtil() {
    // Prevent instantiation
  }
  
  /**
   * <p>
   * A recognizable {@link RuntimeException} for syntax errors.
   * </p>
   * 
   * @author Thib Guicherd-Callin
   * @since 1.67
   */
  public static class SyntaxError extends RuntimeException {

    /**
     * <p>
     * Makes a new {@link SyntaxError} instance.
     * </p>
     * 
     * @since 1.67
     */
    public SyntaxError() {
      super();
    }
    
    /**
     * <p>
     * Makes a new {@link SyntaxError} instance with a message and a cause.
     * </p>
     * 
     * @param message
     *          A message.
     * @param cause
     *          A cause.
     * @since 1.67
     */
    public SyntaxError(String message, Throwable cause) {
      super(message, cause);
    }

    /**
     * <p>
     * Makes a new {@link SyntaxError} instance with a message.
     * </p>
     * 
     * @param message
     *          A message.
     * @since 1.67
     */
    public SyntaxError(String message) {
      super(message);
    }

    /**
     * <p>
     * Makes a new {@link SyntaxError} instance with a cause.
     * </p>
     * 
     * @param cause
     *          A cause.
     * @since 1.67
     */
    public SyntaxError(Throwable cause) {
      super(cause);
    }
    
  }
  
  /**
   * <p>
   * A named {@link ANTLRInputStream}.
   * </p>
   * 
   * @author Thib Guicherd-Callin
   * @since 1.67
   */
  public static class NamedAntlrInputStream extends ANTLRInputStream {
    
    /**
     * <p>
     * The stream's name.
     * </p>
     * 
     * @since 1.67
     */
    protected String name;
    
    /**
     * <p>
     * Makes a named {@link ANTLRInputStream} out of the given string of input.
     * </p>
     * 
     * @param name
     *          The stream's name.
     * @param input
     *          A string of input.
     * @since 1.67
     */
    public NamedAntlrInputStream(String name, String input) {
      super(input);
      this.name = name;
    }
    
    /**
     * <p>
     * Makes a named {@link ANTLRInputStream} out of the given input stream.
     * </p>
     * 
     * @param name
     *          The stream's name.
     * @param input
     *          An input stream.
     * @throws IOException
     *           if an I/O exception occurs.
     * @since 1.67
     */
    public NamedAntlrInputStream(String name, InputStream input) throws IOException {
      super(input);
      this.name = name;
    }
    
    @Override
    public String getSourceName() {
      return name;
    }
    
  }
  
  /**
   * <p>
   * An {@link ANTLRErrorListener} that throws {@link SyntaxError} instead of
   * engaging ANTLR's error recovery mechanisms, and produces error messages in
   * Emacs style.
   * </p>
   * 
   * @author Thib Guicherd-Callin
   * @since 1.67
   * @see AntlrUtil#errorMessage(String, int, int, String, Object...)
   */
  protected static class EmacsErrorListener extends BaseErrorListener {
    
    @Override
    public void syntaxError(@NotNull Recognizer<?, ?> recognizer,
                            @Nullable Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            @NotNull String msg,
                            @Nullable RecognitionException e) {
      String errMsg = errorMessage(recognizer.getInputStream().getSourceName(),
                                   line,
                                   charPositionInLine,
                                   msg);
      if (e == null) {
        throw new SyntaxError(errMsg);
      }
      else {
        throw new SyntaxError(errMsg, e);
      }
    }
    
    /**
     * <p>
     * A singleton instance of this class.
     * </p>
     * 
     * @since 1.67
     * @see #getInstance()
     */
    private static final EmacsErrorListener singleton = new EmacsErrorListener();
    
    /**
     * <p>
     * Obtains an instance of this class.
     * </p>
     * 
     * @return An instance of this class.
     * @since 1.67
     */
    public static EmacsErrorListener getInstance() {
      return singleton;
    }
    
  }
  
  /**
   * <p>
   * Sets up an {@link EmacsErrorListener} as the sole error listener on a
   * {@link Recognizer} ({@link Lexer} or {@link Parser}).
   * </p>
   * 
   * @param recognizer
   * @since 1.67
   * @see EmacsErrorListener
   */
  public static void setEmacsErrorListener(Recognizer<?, ?> recognizer) {
    recognizer.removeErrorListeners();
    recognizer.addErrorListener(EmacsErrorListener.getInstance());
  }

  /**
   * <p>
   * Throws a {@link SyntaxError} based on the offending {@link Token}.
   * </p>
   * 
   * @param token
   *          An offending token.
   * @param format
   *          A format string.
   * @param args
   *          Arguments for the format string.
   * @throws SyntaxError
   *           <b>always</b> thrown by this method.
   * @since 1.67
   * @see #errorMessage(String, int, int, String, Object...)
   */
  public static void syntaxError(Token token,
                                 String format,
                                 Object... args)
      throws SyntaxError {
    String errMsg = errorMessage(token.getInputStream().getSourceName(),
                                 token.getLine(),
                                 token.getCharPositionInLine(),
                                 format,
                                 args);
    throw new SyntaxError(errMsg);
  }
  
  /**
   * <p>
   * Builds an error message in Emacs style
   * (&lt;file&gt;:&lt;line&gt;:&lt;column&gt;: &lt;message&gt;).
   * </p>
   * 
   * @param file
   *          A file/source string.
   * @param line
   *          A line number (1-based).
   * @param column
   *          A column number (1-based).
   * @param format
   *          A format string.
   * @param args
   *          Arguments for the format string.
   * @return A formatted error message.
   * @since 1.67
   */
  public static String errorMessage(String file,
                                    int line,
                                    int column,
                                    String format,
                                    Object... args) {
    return String.format("%s:%d:%d: %s",
                         file,
                         line,
                         column,
                         String.format(format, args));
    
  }
  
}
