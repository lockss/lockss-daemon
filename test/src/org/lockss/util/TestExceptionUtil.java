/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.util;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;
import org.lockss.test.LockssTestCase;

/**
 * <p>
 * Unit tests related to {@link ExceptionUtil}.
 * </p>
 *
 * @since 1.75.8
 * @see ExceptionUtil
 */
public class TestExceptionUtil extends LockssTestCase {

  /**
   * <p>
   * Tests {@link ExceptionUtil#initCause(Throwable, Throwable)}.
   * </p>
   * 
   * @throws Exception
   *           if an exception occurs
   * @since 1.75.8
   */
  public void testInitCause() throws Exception {
    class ExceptionWithoutCause extends Exception {
      ExceptionWithoutCause() { 
        super();
      }
      ExceptionWithoutCause(String message) {
        super(message);
      }
    }
    for (ExceptionWithoutCause e : Arrays.asList(new ExceptionWithoutCause(), new ExceptionWithoutCause("MESSAGESTRING"))) {
      for (IOException c : Arrays.asList((IOException)null, new IOException("CAUSESTRING"))) {
        ExceptionWithoutCause r = ExceptionUtil.initCause(e, c);
        assertSame(e, r);
        assertSame(c, r.getCause());
      }
    }
  }
  
}
