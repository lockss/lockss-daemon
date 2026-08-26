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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.*;
import org.lockss.test.LockssTestCase;

/**
 * <p>
 * Illustrating Jsoup's behavior through functional tests.
 * </p>
 */
public class FuncJsoup extends LockssTestCase {

  /**
   * <p>
   * Illustrates that by default, Jsoup pretty-prints, but it can be turned off.
   * </p>
   * 
   * @throws Exception If any unexpected exception occurs.
   */
  public void testPrettyPrints() throws Exception {
    String input = "<html><head></head><body><p>Hello</p></body></html>";
    String expectedDefault =
          "<html>\n"
        + " <head></head>\n"
        + " <body>\n"
        + "  <p>Hello</p>\n"
        + " </body>\n"
        + "</html>";
    String expectedConfigured = input;
    // Default
    assertEquals(expectedDefault, Jsoup.parse(input).outerHtml());
    // Configured
    Document docConfigured = Jsoup.parse(input);
    docConfigured.outputSettings().prettyPrint(false);
    assertEquals(expectedConfigured, docConfigured.outerHtml());
  }
  
  /**
   * <p>
   * Illustrates that by default, Jsoup normalizes tags and attributes to lower
   * case, but this can be turned off.
   * </p>
   * 
   * @throws Exception If any unexpected exception occurs.
   */
  public void testNormalizesTagsAndAttributes() throws Exception {
    String input = "<html><head></head><body><P CLASS=\"para\">Hello</P></body></html>";
    String expectedDefault = "<html><head></head><body><p class=\"para\">Hello</p></body></html>";
    String expectedConfigured = input;
    // Default
    Document docDefault = Jsoup.parse(input);
    docDefault.outputSettings().prettyPrint(false);
    assertEquals(expectedDefault, docDefault.outerHtml());
    // Configured
    Parser parserConfigured = Parser.htmlParser().settings(ParseSettings.preserveCase);
    Document docConfigured = Jsoup.parse(input, parserConfigured);
    docConfigured.outputSettings().prettyPrint(false);
    assertEquals(expectedConfigured, docConfigured.outerHtml());
  }
  
  /**
   * <p>
   * Illustrates that by default, Jsoup adds an empty <head> section if missing.
   * No known way to turn it off.</p>
   * 
   * @throws Exception If any unexpected exception occurs.
   */
  public void testAddsHead() throws Exception {
    String input = "<html><body><p>Hello</p></body></html>";
    String expectedDefault = "<html><head></head><body><p>Hello</p></body></html>";
    // Default
    Document docDefault = Jsoup.parse(input);
    docDefault.outputSettings().prettyPrint(false);
    assertEquals(expectedDefault, docDefault.outerHtml());
  }
  
  /**
   * <p>
   * Illustrates that by default, Jsoup normalizes the quoting of attribute
   * values to double quotes. No known ways to turn it off.
   * </p>
   * 
   * @throws Exception If any unexpected exception occurs.
   * @see https://github.com/jhy/jsoup/discussions/2348
   */
  public void testNormalizesAttributeQuoting() throws Exception {
    String input = "<html><head></head><body><p class=a>0</p><p class='b'>1</p><p class=\"c\">2</p></body></html>";
    String expectedDefault = "<html><head></head><body><p class=\"a\">0</p><p class=\"b\">1</p><p class=\"c\">2</p></body></html>";
    // Default
    Document docDefault = Jsoup.parse(input);
    docDefault.outputSettings().prettyPrint(false);
    assertEquals(expectedDefault, docDefault.outerHtml());
  }
  
}
