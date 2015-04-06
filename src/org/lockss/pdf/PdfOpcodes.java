/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * <p>
 * Opcodes of various PDF operators.
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 */
public class PdfOpcodes {

  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String BEGIN_IMAGE_DATA = "ID";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String BEGIN_IMAGE_OBJECT = "BI";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String BEGIN_TEXT_OBJECT = "BT";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String END_TEXT_OBJECT = "ET";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String INVOKE_XOBJECT = "Do";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String NEXT_LINE_SHOW_TEXT = "'";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56.3
   */
  public static final String RESTORE_GRAPHICS_STATE = "Q";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56.3
   */
  public static final String SAVE_GRAPHICS_STATE = "q";
  
  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String SET_SPACING_NEXT_LINE_SHOW_TEXT = "\"";

  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.62
   */
  public static final String SET_TEXT_FONT = "Tf";

  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.62
   */
  public static final String SET_TEXT_MATRIX = "Tm";

  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String SHOW_TEXT = "Tj";

  /**
   * <p>
   * The {@value} opcode.
   * </p>
   * 
   * @since 1.56
   */
  public static final String SHOW_TEXT_GLYPH_POSITIONING = "TJ";

  /**
   * <p>
   * This class cannot be instantiated.
   * </p>
   * 
   * @since 1.56
   */
  private PdfOpcodes() {
    // Prevent instantiation
  }

}
