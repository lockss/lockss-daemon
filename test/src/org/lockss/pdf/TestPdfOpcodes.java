/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.pdf;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.lockss.test.LockssTestCase;

public class TestPdfOpcodes extends LockssTestCase {

  protected static final String[] OPCODES = {PdfOpcodes.BEGIN_IMAGE_DATA, // renamed to PdfOpcodes.BEGIN_INLINE_IMAGE_DATA
                                             PdfOpcodes.BEGIN_IMAGE_OBJECT, // renamed to PdfOpcodes.BEGIN_INLINE_IMAGE
                                             PdfOpcodes.BEGIN_INLINE_IMAGE,
                                             PdfOpcodes.BEGIN_INLINE_IMAGE_DATA,
                                             PdfOpcodes.BEGIN_TEXT,
                                             PdfOpcodes.BEGIN_TEXT_OBJECT, // renamed to PdfOpcodes.BEGIN_TEXT
                                             PdfOpcodes.DRAW_OBJECT,
                                             PdfOpcodes.END_TEXT,
                                             PdfOpcodes.END_TEXT_OBJECT, // renamed to PdfOpcodes.END_TEXT
                                             PdfOpcodes.INVOKE_XOBJECT, // renamed to PdfOpcodes.DRAW_OBJECT
                                             PdfOpcodes.NEXT_LINE_SHOW_TEXT, // renamed to PdfOpcodes.SHOW_TEXT_LINE
                                             PdfOpcodes.RESTORE,
                                             PdfOpcodes.RESTORE_GRAPHICS_STATE, // renamed to PdfOpcodes.RESTORE
                                             PdfOpcodes.SAVE,
                                             PdfOpcodes.SAVE_GRAPHICS_STATE, // renamed to PdfOpcodes.SAVE
                                             PdfOpcodes.SET_GRAPHICS_STATE_PARAMS,
                                             PdfOpcodes.SET_SPACING_NEXT_LINE_SHOW_TEXT, // renamed to PdfOpcodes.SHOW_TEXT_LINE_AND_SPACE
                                             PdfOpcodes.SET_TEXT_FONT,
                                             PdfOpcodes.SET_TEXT_MATRIX,
                                             PdfOpcodes.SHOW_TEXT,
                                             PdfOpcodes.SHOW_TEXT_ADJUSTED,
                                             PdfOpcodes.SHOW_TEXT_GLYPH_POSITIONING, // renamed to PdfOpcodes.SHOW_TEXT_ADJUSTED
                                             PdfOpcodes.SHOW_TEXT_LINE,
                                             PdfOpcodes.SHOW_TEXT_LINE_AND_SPACE,
                                            };
  
  protected PdfTokenFactory tf;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.tf = new MockPdfTokenFactory();
  }
  
  public void testIsOpcode() throws Exception {
    for (String opcode : OPCODES) {
      PdfToken operator = tf.makeOperator(opcode);
      for (String other : OPCODES) {
        assertEquals(opcode.equals(other),
                     PdfOpcodes.isOpcode(operator, other));
      }
      assertEquals(PdfOpcodes.BEGIN_IMAGE_DATA.equals(opcode),
                   PdfOpcodes.isBeginImageData(operator));
      assertEquals(PdfOpcodes.BEGIN_IMAGE_OBJECT.equals(opcode),
                   PdfOpcodes.isBeginImageObject(operator));
      assertEquals(PdfOpcodes.BEGIN_INLINE_IMAGE.equals(opcode),
                   PdfOpcodes.isBeginInlineImage(operator));
      assertEquals(PdfOpcodes.BEGIN_INLINE_IMAGE_DATA.equals(opcode),
                   PdfOpcodes.isBeginInlineImageData(operator));
      assertEquals(PdfOpcodes.BEGIN_TEXT.equals(opcode),
                   PdfOpcodes.isBeginText(operator));
      assertEquals(PdfOpcodes.BEGIN_TEXT_OBJECT.equals(opcode),
                   PdfOpcodes.isBeginTextObject(operator));
      assertEquals(PdfOpcodes.DRAW_OBJECT.equals(opcode),
                   PdfOpcodes.isDrawObject(operator));
      assertEquals(PdfOpcodes.END_TEXT.equals(opcode),
                   PdfOpcodes.isEndText(operator));
      assertEquals(PdfOpcodes.END_TEXT_OBJECT.equals(opcode),
                   PdfOpcodes.isEndTextObject(operator));
      assertEquals(PdfOpcodes.INVOKE_XOBJECT.equals(opcode),
                   PdfOpcodes.isInvokeXObject(operator));
      assertEquals(PdfOpcodes.NEXT_LINE_SHOW_TEXT.equals(opcode),
                   PdfOpcodes.isNextLineShowText(operator));
      assertEquals(PdfOpcodes.RESTORE.equals(opcode),
                   PdfOpcodes.isRestore(operator));
      assertEquals(PdfOpcodes.RESTORE_GRAPHICS_STATE.equals(opcode),
                   PdfOpcodes.isRestoreGraphicsState(operator));
      assertEquals(PdfOpcodes.SAVE.equals(opcode),
                   PdfOpcodes.isSave(operator));
      assertEquals(PdfOpcodes.SAVE_GRAPHICS_STATE.equals(opcode),
                   PdfOpcodes.isSaveGraphicsState(operator));
      assertEquals(PdfOpcodes.SET_GRAPHICS_STATE_PARAMS.equals(opcode),
                   PdfOpcodes.isSetGraphicsStateParams(operator));
      assertEquals(PdfOpcodes.SET_SPACING_NEXT_LINE_SHOW_TEXT.equals(opcode),
                   PdfOpcodes.isSetSpacingNextLineShowText(operator));
      assertEquals(PdfOpcodes.SET_TEXT_FONT.equals(opcode),
                   PdfOpcodes.isSetTextFont(operator));
      assertEquals(PdfOpcodes.SET_TEXT_MATRIX.equals(opcode),
                   PdfOpcodes.isSetTextMatrix(operator));
      assertEquals(PdfOpcodes.SHOW_TEXT.equals(opcode),
                   PdfOpcodes.isShowText(operator));
      assertEquals(PdfOpcodes.SHOW_TEXT_ADJUSTED.equals(opcode),
                   PdfOpcodes.isShowTextAdjusted(operator));
      assertEquals(PdfOpcodes.SHOW_TEXT_GLYPH_POSITIONING.equals(opcode),
                   PdfOpcodes.isShowTextGlyphPositioning(operator));
      assertEquals(PdfOpcodes.SHOW_TEXT_LINE.equals(opcode),
                   PdfOpcodes.isShowTextLine(operator));
      assertEquals(PdfOpcodes.SHOW_TEXT_LINE_AND_SPACE.equals(opcode),
                   PdfOpcodes.isShowTextLineAndSpace(operator));
    }
  }
  
  public void testStringOpcodes() throws Exception {
    /*
     * LOCAL CLASS
     */
    // Operator is an opcode with the complement of 6 string-related operations
    // Operand is, or is equivalent to, the string "foobarqux"
    // The 6 abstract methods call the right PdfOpcodes methods for the opcode 
    abstract class StringOpcodeTester {
      PdfToken operator;
      PdfToken operand;
      StringOpcodeTester(PdfToken operator, PdfToken operand) {
        this.operator = operator;
        this.operand = operand;
      }
      abstract boolean contains(String substr);
      abstract boolean endsWith(String suffix);
      abstract boolean equals(String str);
      abstract boolean equalsIgnoreCase(String str);
      abstract boolean find(Pattern pattern);
      abstract boolean startsWith(String prefix);
    }

    StringOpcodeTester showText =
        new StringOpcodeTester(tf.makeOperator(PdfOpcodes.SHOW_TEXT),
                               tf.makeString("foobarqux")) {
            @Override boolean contains(String substr) {
              return PdfOpcodes.isShowTextContains(operator, operand, substr);
            }
            @Override boolean endsWith(String suffix) {
              return PdfOpcodes.isShowTextEndsWith(operator, operand, suffix);
            }
            @Override boolean equals(String str) {
              return PdfOpcodes.isShowTextEquals(operator, operand, str);
            }
            @Override boolean equalsIgnoreCase(String str) {
              return PdfOpcodes.isShowTextEqualsIgnoreCase(operator, operand, str);
            }
            @Override boolean find(Pattern pattern) {
              return PdfOpcodes.isShowTextFind(operator, operand, pattern);
            }
            @Override boolean startsWith(String prefix) {
              return PdfOpcodes.isShowTextStartsWith(operator, operand, prefix);
            }
    };

    StringOpcodeTester showTextAdjusted =
        new StringOpcodeTester(tf.makeOperator(PdfOpcodes.SHOW_TEXT_ADJUSTED),
                               tf.makeArray(Arrays.asList(tf.makeString("foo"),
                                                          tf.makeInteger(1),
                                                          tf.makeString("bar"),
                                                          tf.makeInteger(2),
                                                          tf.makeString("qux")))) {
            @Override boolean contains(String substr) {
              return PdfOpcodes.isShowTextAdjustedContains(operator, operand, substr);
            }
            @Override boolean endsWith(String suffix) {
              return PdfOpcodes.isShowTextAdjustedEndsWith(operator, operand, suffix);
            }
            @Override boolean equals(String str) {
              return PdfOpcodes.isShowTextAdjustedEquals(operator, operand, str);
            }
            @Override boolean equalsIgnoreCase(String str) {
              return PdfOpcodes.isShowTextAdjustedEqualsIgnoreCase(operator, operand, str);
            }
            @Override boolean find(Pattern pattern) {
              return PdfOpcodes.isShowTextAdjustedFind(operator, operand, pattern);
            }
            @Override boolean startsWith(String prefix) {
              return PdfOpcodes.isShowTextAdjustedStartsWith(operator, operand, prefix);
            }
    };
    
    for (StringOpcodeTester tester : Arrays.asList(showText,
                                                   showTextAdjusted)) {
      assertTrue(tester.contains("bar"));
      assertFalse(tester.contains("fred"));
      assertTrue(tester.endsWith("ux"));
      assertTrue(tester.endsWith("qux"));
      assertTrue(tester.endsWith("rqux"));
      assertTrue(tester.endsWith("foobarqux"));
      assertFalse(tester.endsWith("fred"));
      assertFalse(tester.endsWith("abcfoobarqux"));
      assertFalse(tester.endsWith("barbaz"));
      assertTrue(tester.equals("foobarqux"));
      assertFalse(tester.equals("FOOBARQUX"));
      assertFalse(tester.equals("fred"));
      assertTrue(tester.equalsIgnoreCase("foobarqux"));
      assertTrue(tester.equalsIgnoreCase("FOOBARQUX"));
      assertFalse(tester.equalsIgnoreCase("fred"));
      assertTrue(tester.find(Pattern.compile("bar")));
      assertFalse(tester.find(Pattern.compile("fred")));
      assertTrue(tester.startsWith("fo"));
      assertTrue(tester.startsWith("foo"));
      assertTrue(tester.startsWith("foob"));
      assertTrue(tester.startsWith("foobarqux"));
      assertFalse(tester.startsWith("fred"));
      assertFalse(tester.startsWith("foobarquxxyz"));
      assertFalse(tester.startsWith("bazbar"));
    }
  }
  
}
