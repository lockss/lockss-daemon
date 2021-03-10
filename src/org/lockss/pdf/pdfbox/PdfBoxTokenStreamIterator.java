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

package org.lockss.pdf.pdfbox;

import java.io.*;
import java.util.*;

import org.apache.pdfbox.contentstream.*;
import org.apache.pdfbox.contentstream.operator.*;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.PDFontSetting;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.state.*;
import org.lockss.pdf.*;
import org.lockss.util.Logger;

public class PdfBoxTokenStreamIterator
    implements Iterator<PdfBoxOperandsAndOperator> {
  
  protected static final Logger log = Logger.getLogger(PdfBoxTokenStreamIterator.class);

  protected PDContentStream pdContentStream;
  
  protected PDPage pdPage;
  
  protected PDFStreamParser pdfBoxParser;
  
  protected PdfBoxOperandsAndOperator next;
  
  protected Deque<PDFont> pdGraphicsStateStack;
  
  protected PDResources pdResources;
  
  public PdfBoxTokenStreamIterator(PDPage pdPage)
      throws PdfException {
    this((PDContentStream)pdPage, pdPage);
  }
  
  public PdfBoxTokenStreamIterator(PDFormXObject pdFormXObject, PDPage enclosingPage)
      throws PdfException {
    this((PDContentStream)pdFormXObject, enclosingPage);
  }
  
  protected PdfBoxTokenStreamIterator(PDContentStream pdContentStream, PDPage enclosingPage)
      throws PdfException {
    this.pdContentStream = pdContentStream;
    this.next = null;
    
    // See PDFBox 2.0.22 PDFStreamEngine instance variable initialization
    this.pdGraphicsStateStack = new LinkedList<>(); // because ArrayDeque prohibits null
    
    initializePage(enclosingPage);
    if (pdPage.hasContents()) {
      try {
        this.pdfBoxParser = new PDFStreamParser(pdContentStream);
      }
      catch (IOException ioe) {
        throw new PdfException(ioe);
      }
    }
    initializeStream(pdContentStream);
  }
  
  @Override
  public boolean hasNext() throws UncheckedIOException {
    if (pdfBoxParser == null) {
      return false;
    }
    if (next == null) {
      try {
        findNext();
      }
      catch (IOException ioe) {
        throw new UncheckedIOException(ioe);
      }
    }
    return next != null;
  }

  @Override
  public PdfBoxOperandsAndOperator next() {
    if (hasNext()) {
      PdfBoxOperandsAndOperator ret = next;
      next = null;
      return ret;
    }
    else {
      throw new NoSuchElementException();
    }
  }

  public PDResources getPdResources() {
    return pdResources;
  }
  
  protected void findNext() throws IOException {
    List<COSBase> pdfBoxOperands = new ArrayList<>();
    List<PdfBoxToken> retOperands = new ArrayList<>();
    Object pdfBoxObject;
    
    // See PDFBox 2.0.22 PDFStreamEngine processStreamOperators()
    while ((pdfBoxObject = pdfBoxParser.parseNextToken()) != null) {

      if (pdfBoxObject instanceof Operator) {
        // ======== IS AN OPERATOR ========
        Operator pdfBoxOperator = (Operator)pdfBoxObject;
        boolean processOperands = true;
        
        switch (pdfBoxOperator.getName()) {
        
          // -------- Tj and ' and " --------
          case OperatorName.SHOW_TEXT:
          case OperatorName.SHOW_TEXT_LINE:
          case OperatorName.SHOW_TEXT_LINE_AND_SPACE:
            // See PDFBox 2.0.22 ShowText, ShowTextLine, ShowTextLineAndSpace
            for (Object pdfBoxOperand : pdfBoxOperands) {
              if (pdfBoxOperand instanceof COSString) {
                retOperands.add(processString((COSString)pdfBoxOperand));
              }
              else {
                // Not supposed to happen, but just in case
                retOperands.add(PdfBoxToken.convertOne(pdfBoxOperand));
              }
            }
            processOperands = false;
            break;

          // -------- TJ --------
          case OperatorName.SHOW_TEXT_ADJUSTED:
            // See PDFBox 2.0.22 ShowTextAdjusted
            for (Object pdfBoxOperand : pdfBoxOperands) {
              if (pdfBoxOperand instanceof COSArray) {
                retOperands.add(processStrings((COSArray)pdfBoxOperand));
              }
              else if (pdfBoxOperand instanceof COSString) {
                // Not supposed to happen, but just in case
                retOperands.add(processString((COSString)pdfBoxOperand));
              }
              else {
                // Not supposed to happen, but just in case
                retOperands.add(PdfBoxToken.convertOne(pdfBoxOperand));
              }
            }
            processOperands = false;
            break;
          
          // -------- Tf --------
          case OperatorName.SET_FONT_AND_SIZE:
            // See PDFBox 2.0.22 SetFontAndSize
            if (pdfBoxOperands.size() >= 2) {
              COSBase operand0 = pdfBoxOperands.get(0);
              if (operand0 instanceof COSName) {
                COSName fontName = (COSName)operand0;
                COSBase operand1 = pdfBoxOperands.get(1);
                if (operand1 instanceof COSNumber) {
                  PDFont newFont = pdContentStream.getResources().getFont(fontName);
                  pdGraphicsStateStack.removeFirst();
                  pdGraphicsStateStack.addFirst(newFont);
                  break; // success
                }
              }
            }
            // FIXME logging?
            break;
            
          // -------- q --------
          case OperatorName.SAVE:
            // See PDFBox 2.0.22 Save
            pdGraphicsStateStack.addFirst(pdGraphicsStateStack.peekFirst());
            break;
            
          // -------- Q --------
          case OperatorName.RESTORE:
            // See PDFBox 2.0.22 Restore
            try {
              pdGraphicsStateStack.removeFirst();
            }
            catch (NoSuchElementException nsee) {
              throw new IOException("Restore operator applied to empty graphics state stack", nsee); 
            }
            break;

          // -------- gs --------
          case OperatorName.SET_GRAPHICS_STATE_PARAMS:
            // See PDFBox 2.0.22 SetGraphicsStateParameters
            if (pdfBoxOperands.size() >= 1) {
              COSBase operand0 = pdfBoxOperands.get(0);
              if (operand0 instanceof COSName) {
                COSName graphicsName = (COSName)operand0;
                PDExtendedGraphicsState pdExtendedGraphicsState = pdResources.getExtGState(graphicsName);
                if (pdExtendedGraphicsState != null) {
                  // See PDFBox 2.0.22 PDExtendedGraphicsState copyIntoGraphicsState() and getFontSetting()
                  COSDictionary cosDictionary = pdExtendedGraphicsState.getCOSObject();
                  if (cosDictionary != null) {
                    COSBase cosBase = cosDictionary.getDictionaryObject(COSName.FONT);
                    if (cosBase != null && cosBase instanceof COSArray) {
                      COSArray cosArray = (COSArray)cosBase;
                      PDFontSetting pdFontSetting = new PDFontSetting(cosArray);
                      pdGraphicsStateStack.removeFirst();
                      pdGraphicsStateStack.addFirst(pdFontSetting.getFont());
                      break; // success
                    }
                  }
                }
              }
            }
            // FIXME logging?
            break;
            
          // -------- Default --------
          default:
            // Intentionally left blank
            break;

        }

        // In most cases, convert the operands
        if (processOperands) {
          for (Object pdfBoxOperand : pdfBoxOperands) {
            retOperands.add(PdfBoxToken.convertOne(pdfBoxOperand));
          }
        }
        
        // Finally, convert the operator itself and clear the operands lists
        next = new PdfBoxOperandsAndOperator(retOperands, PdfBoxToken.Op.of(pdfBoxOperator));
        pdfBoxOperands.clear();
        retOperands.clear();
        return;
      }
      else if (pdfBoxObject instanceof COSObject) {
        // ======== IS A COSObject OPERAND ========
        pdfBoxOperands.add(((COSObject)pdfBoxObject).getObject());
      }
      else if (pdfBoxObject instanceof COSBase) {
        // ======== IS A COSBase OPERAND ========
        pdfBoxOperands.add((COSBase)pdfBoxObject);
      }
      else {
        // Not supposed to happen, but just in case
        throw new IOException(String.format("PDF token of unknown type: %s (%s)",
                                            pdfBoxObject.getClass().getName(),
                                            pdfBoxObject.toString()));
      }
    }
    
    // FIXME if nonempty operands, stream did not end in an operator; throw?
    
    // Exited the while: end of stream
    pdfBoxParser = null;
  }
  
  protected PdfBoxToken processString(COSString cosString) throws IOException {
    // See PDFBox 2.0.22 PDFStreamEngine showText()
    PDFont font = pdGraphicsStateStack.peekFirst();
    if (font == null) {
      font = PDType1Font.HELVETICA;
    }
    InputStream bais = new ByteArrayInputStream(cosString.getBytes());
    StringBuilder sb = new StringBuilder();
    while (bais.available() > 0) {
      int code = font.readCode(bais);
      // See PDFBox 2.0.22 PDFStreamEngine showGlyph(4)
      String unicode = font.toUnicode(code);
      if (font instanceof PDType3Font) {
        // See PDFBox 2.0.22 PDFStreamEngine showType3Glyph(5)
        PDType3Font pdType3Font = (PDType3Font)font;
        PDType3CharProc charProc = pdType3Font.getCharProc(code);
        if (charProc != null) {
          // FIXME processType3Stream()
          return null;
        }
      }
      else {
        // See PDFBox 2.0.22 PDFStreamEngine showFontGlyph(4)
        sb.append(unicode);
      }
    }
    return PdfBoxToken.Str.of(sb.toString());
  }
  
  protected PdfBoxToken processStrings(COSArray cosArray) throws IOException {
    // See PDFBox 2.0.22 PDFStreamEngine lines 683-762
    List<PdfToken> tokens = new ArrayList<>(cosArray.size());
    for (COSBase cosBase : cosArray) {
      if (cosBase instanceof COSString) {
        tokens.add(processString((COSString)cosBase));
      }
      else {
        tokens.add(PdfBoxToken.convertOne(cosBase));
      }
    }
    return PdfBoxToken.Arr.of(tokens);
  }

  /**
   * 
   * @param pdPage
   * @since 1.76
   * @see PDFStreamEngine#initPage(PDPage)
   */
  protected void initializePage(PDPage pdPage) {
    // See PDFBox 2.0.22 PDFStreamEngine initPage()
    this.pdPage = pdPage;
    this.pdGraphicsStateStack.clear(); // probably moot
    this.pdGraphicsStateStack.addFirst(null);
    this.pdResources = null;
  }

  /**
   * 
   * @param pdContentStream
   * @since 1.76
   * @see PDFStreamEngine#processStream(PDContentStream)
   */
  protected void initializeStream(PDContentStream pdContentStream) {
    // See PDFBox 2.0.22 PDFStreamEngine processStream()
    PDResources oldResources = pushResources(pdContentStream);
    Deque<PDFont> oldStack = saveStack();
    
    // FIXME: these should happen when a stream is exhausted
//    pdGraphicsStateStack = oldStack; // See PDFBox 2.0.22 PDFStreamEngine restore()
//    pdResources = oldResources; // See PDFBox 2.0.22 PDFStreamEngine popResources()
  }
  
  /**
   * @since 1.76
   * @see PDFStreamEngine#pushResources(PDContentStream)
   */
  protected PDResources pushResources(PDContentStream pdContentStream) {
    // See PDFBox 2.0.22 PDFStreamEngine pushResources()
    PDResources ret = pdResources;
    PDResources streamResources = pdContentStream.getResources();
    if (streamResources != null) {
      pdResources = streamResources;
    }
    else if (pdResources == null) {
      pdResources = pdPage.getResources();
    }
    if (pdResources == null) {
      pdResources = new PDResources();
    }
    return ret;
  }
  
  /**
   * 
   * @return
   * @since 1.76
   * @see PDFStreamEngine#restoreGraphicsStack(Deque<PDGraphicsState>)
   */
  protected Deque<PDFont> saveStack() {
    Deque<PDFont> ret = pdGraphicsStateStack;
    pdGraphicsStateStack = new LinkedList<>();
    pdGraphicsStateStack.addFirst(ret.getFirst());
    return ret;
  }
  
}
