/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.rewriter;

import java.io.File;

import org.apache.commons.io.input.NullInputStream;
import org.lockss.test.LockssTestCase;

public class TestJsoupTransformInputStream extends LockssTestCase {

  public void testInvalidInvocations() throws Exception {
    try {
      new JsoupTransformInputStream().setInputStream(null);
      fail("Should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException exc) {
      assertEquals("Input stream is null", exc.getMessage());
    }
    try {
      new JsoupTransformInputStream().setInputStream(new NullInputStream())
                                     .setInputStream(new NullInputStream());
      fail("Should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException exc) {
      assertEquals("Input stream is already set", exc.getMessage());
    }
    try {
      new JsoupTransformInputStream().setInputFile(null);
      fail("Should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException exc) {
      assertEquals("Input file is null", exc.getMessage());
    }
    try {
      new JsoupTransformInputStream().setInputFile(new File("/dev/null"))
                                     .setInputFile(new File("/dev/null"));
      fail("Should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException exc) {
      assertEquals("Input stream is already set", exc.getMessage());
    }
    try {
      new JsoupTransformInputStream().setInputString(null);
      fail("Should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException exc) {
      assertEquals("Input string is null", exc.getMessage());
    }
    try {
      new JsoupTransformInputStream().setInputString("Hello world"); // input charset name not set
      fail("Should have thrown IllegalStateException");
    }
    catch (IllegalStateException exc) {
      assertEquals("Input charset name is not set", exc.getMessage());
    }
    try {
      new JsoupTransformInputStream().setInputCharsetName("UTF-8") // input charset name set first
                                     .setInputString("Hello world")
                                     .setInputString("Hello world");
      fail("Should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException exc) {
      assertEquals("Input stream is already set", exc.getMessage());
    }
    try {
      new JsoupTransformInputStream().setInputCharsetName(null);
      fail("Should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException exc) {
      assertEquals("Input charset name is null", exc.getMessage());
    }
    try {
      new JsoupTransformInputStream().setInputCharsetName("UTF-8")
                                     .setInputCharsetName("UTF-8");
      fail("Should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException exc) {
      assertEquals("Input charset name is already set", exc.getMessage());
    }
    try {
      new JsoupTransformInputStream().setBaseUri(null);
      fail("Should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException exc) {
      assertEquals("Base URI is null", exc.getMessage());
    }
    try {
      new JsoupTransformInputStream().setBaseUri("https://example.com/")
                                     .setBaseUri("https://example.com/");
      fail("Should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException exc) {
      assertEquals("Base URI is already set", exc.getMessage());
    }
    try {
      new JsoupTransformInputStream().setOutputCharsetName(null);
      fail("Should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException exc) {
      assertEquals("Output charset name is null", exc.getMessage());
    }
    try {
      new JsoupTransformInputStream().setOutputCharsetName("UTF-8")
                                     .setOutputCharsetName("UTF-8");
      fail("Should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException exc) {
      assertEquals("Output charset name is already set", exc.getMessage());
    }
    try {
      new JsoupTransformInputStream().setDtfosThreshold(-1);
      fail("Should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException exc) {
      assertEquals("DTFOS threshold is negative", exc.getMessage());
    }
    try {
      new JsoupTransformInputStream().setDtfosThreshold(0);
      fail("Should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException exc) {
      assertEquals("DTFOS threshold is zero", exc.getMessage());
    }
    try {
      new JsoupTransformInputStream().setDtfosThreshold(1)
                                     .setDtfosThreshold(1);
      fail("Should have thrown IllegalArgumentException");
    }
    catch (IllegalArgumentException exc) {
      assertEquals("DTFOS threshold is already set", exc.getMessage());
    }
  }
  
}
