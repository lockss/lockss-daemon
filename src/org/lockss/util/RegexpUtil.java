/*
 * $Id: RegexpUtil.java,v 1.3 2004-08-04 23:47:56 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import org.apache.oro.text.regex.*;

/**
 * Utilities for regexp processing.
 */
public class RegexpUtil {

  private static ThreadLocal reInst = new ThreadLocal() {
      protected Object initialValue() {
	return new REInst();
      }
    };

  /** Return a thread-local Perl5Compiler */
  public static Perl5Compiler getCompiler() {
    return ((REInst)reInst.get()).compiler;
  }

  /** Return a thread-local Perl5Matcher */
  public static Perl5Matcher getMatcher() {
    return ((REInst)reInst.get()).matcher;
  }

  /** Compile a regexp into a pattern, without throwing a checked
   * exception.  Intended for compiling a constant pattern in a static
   * initializer, where exception handling is awkward, and there is no
   * possibility of the pattern being malformed.
   * @throws RuntimeException if RE is malformed */
  public static Pattern uncheckedCompile(String re) {
    return uncheckedCompile(re, Perl5Compiler.DEFAULT_MASK);
  }

  /** Compile a regexp into a pattern, without throwing a checked
   * exception.  Intended for compiling a constant pattern in a static
   * initializer, where exception handling is awkward, and there is no
   * possibility of the pattern being malformed.
   * @throws RuntimeException if RE is malformed */
  public static Pattern uncheckedCompile(String re, int options) {
    try {
      return getCompiler().compile(re, options);
    } catch (MalformedPatternException e) {
      throw new RuntimeException("Malformed RE: " + e.getMessage());
    }
  }

  private static class REInst {
    private Perl5Compiler compiler = new Perl5Compiler();
    private Perl5Matcher matcher = new Perl5Matcher();
  }
    
}
