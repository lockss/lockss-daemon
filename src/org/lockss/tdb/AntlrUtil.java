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

import java.io.*;

import org.antlr.v4.runtime.*;

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
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
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
