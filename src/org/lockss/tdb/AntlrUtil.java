/*
 * $Id: AntlrUtil.java,v 1.1 2014-09-03 20:35:58 thib_gc Exp $
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

public class AntlrUtil {

  private AntlrUtil() {
    // Prevent instantiation
  }
  
  protected static class NamedAntlrInputStream extends ANTLRInputStream {
    
    protected String name;
    
    public NamedAntlrInputStream(String name, String input) {
      super(input);
      this.name = name;
    }
    
    public NamedAntlrInputStream(String name, InputStream input) throws IOException {
      super(input);
      this.name = name;
    }
    
    @Override
    public String getSourceName() {
      return name;
    }
    
  }
  
  public static ANTLRInputStream makeNamedAntlrInputStream(String name,
                                                           String input) {
    return new NamedAntlrInputStream(name, input);
  }
  
  public static ANTLRInputStream makeNamedAntlrInputStream(String name,
                                                           InputStream input)
      throws IOException {
    return new NamedAntlrInputStream(name, input);
  }
  
  protected static class EmacsErrorListener extends BaseErrorListener {
    
    @Override
    public void syntaxError(@NotNull Recognizer<?, ?> recognizer,
                            @Nullable Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            @NotNull String msg,
                            @Nullable RecognitionException e) {
      error(recognizer.getInputStream().getSourceName(),
            line,
            charPositionInLine,
            msg);
    }
    
    private static final EmacsErrorListener singleton = new EmacsErrorListener();
    
    public static EmacsErrorListener getInstance() {
      return singleton;
    }
    
  }
  
  public static void setEmacsErrorListener(Recognizer<?, ?> recognizer) {
    recognizer.removeErrorListeners();
    recognizer.addErrorListener(EmacsErrorListener.getInstance());
  }

  public static void error(Token token,
                           String format,
                           Object... args) {
    error(token.getInputStream().getSourceName(),
          token.getLine(),
          token.getCharPositionInLine(),
          format,
          args);
  }
  
  public static void error(String src,
                           int line,
                           int column,
                           String format,
                           Object... args) {
    System.err.println(String.format("%s:%d:%d: %s",
                                     src,
                                     line,
                                     column,
                                     String.format(format, args)));
    System.exit(1);
  }
  
}
