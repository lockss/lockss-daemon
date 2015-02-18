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

package org.lockss.pdf.pdfbox;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.collections.map.LRUMap;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.util.PDFOperator;
import org.lockss.pdf.*;
import org.lockss.util.Logger;

/**
 * <p>
 * This class centralizes the knowledge needed to convert and mediate
 * between high-level PDF tokens ({@link PdfToken}), their external
 * representation (Java types) and their internal representation
 * (PDFBox 1.6.0 objects). It provides {@link PdfToken} and
 * {@link PdfTokenFactory} implementations based on PDFBox 1.6.0.
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>PDF type</th>
 * <th>External representation</th>
 * <th>Internal representation</th>
 * <th>Characterization</th>
 * <th>External downcast</th>
 * <th>Internal downcast</th>
 * <th>Upcast</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>PDF array</td>
 * <td>{@link List}&lt;{@link PdfToken}&gt;</td>
 * <td>{@link COSArray}</td>
 * <td>{@link #isArray(Object)}</td>
 * <td>{@link #getArray(PdfToken)} / {@link #getArray(COSArray)}</td>
 * <td>{@link #asCOSArray(PdfToken)} / {@link #asCOSArray(List)}</td>
 * <td>{@link #makeArray(List)} / {@link #makeArray(COSArray)}</td>
 * </tr>
 * <tr>
 * <td>PDF boolean</td>
 * <td><code>boolean</code></td>
 * <td>{@link COSBoolean}</td>
 * <td>{@link #isBoolean(Object)}</td>
 * <td>{@link #getBoolean(PdfToken)} / {@link #getBoolean(COSBoolean)}
 * </td>
 * <td>{@link #asCOSBoolean(PdfToken)} /
 * {@link #asCOSBoolean(boolean)}</td>
 * <td>{@link #makeBoolean(boolean)} /
 * {@link #makeBoolean(COSBoolean)}</td>
 * </tr>
 * <tr>
 * <td>PDF dictionary</td>
 * <td>{@link Map}&lt;{@link String}, {@link PdfToken}&gt;</td>
 * <td>{@link COSDictionary}</td>
 * <td>{@link #isDictionary(Object)}</td>
 * <td>{@link #getDictionary(PdfToken)} /
 * {@link #getDictionary(COSDictionary)}</td>
 * <td>{@link #asCOSDictionary(PdfToken)} /
 * {@link #asCOSDictionary(Map)}</td>
 * <td>{@link #makeDictionary(Map)} /
 * {@link #makeDictionary(COSDictionary)}</td>
 * </tr>
 * <tr>
 * <td>PDF float</td>
 * <td><code>float</code></td>
 * <td>{@link COSFloat}</td>
 * <td>{@link #isFloat(Object)}</td>
 * <td>{@link #getFloat(PdfToken)} / {@link #getFloat(COSFloat)}</td>
 * <td>{@link #asCOSFloat(PdfToken)} / {@link #asCOSFloat(float)}</td>
 * <td>{@link #makeFloat(float)} / {@link #makeFloat(COSFloat)}</td>
 * </tr>
 * <tr>
 * <td>PDF integer</td>
 * <td><code>long</code></td>
 * <td>{@link COSInteger}</td>
 * <td>{@link #isInteger(Object)}</td>
 * <td>{@link #getInteger(PdfToken)} / {@link #getInteger(COSInteger)}
 * </td>
 * <td>{@link #asCOSInteger(PdfToken)} / {@link #asCOSInteger(long)}</td>
 * <td>{@link #makeInteger(long)} / {@link #makeInteger(COSInteger)}</td>
 * </tr>
 * <tr>
 * <td>PDF name</td>
 * <td>{@link String}</td>
 * <td>{@link COSName}</td>
 * <td>{@link #isString(Object)}</td>
 * <td>{@link #getName(PdfToken)} / {@link #getName(COSName)}</td>
 * <td>{@link #asCOSName(PdfToken)} / {@link #asCOSName(String)}</td>
 * <td>{@link #makeName(String)} / {@link #makeName(COSName)}</td>
 * </tr>
 * <tr>
 * <td>PDF null</td>
 * <td><code>null</code></td>
 * <td>{@link COSNull}</td>
 * <td>{@link #isNull(Object)}</td>
 * <td>n/a</td>
 * <td>{@link #asCOSNull(PdfToken)}</td>
 * <td>{@link #makeNull()}</td>
 * </tr>
 * <tr>
 * <td>PDF object</td>
 * <td>{@link PdfToken}</td>
 * <td>{@link COSObject}</td>
 * <td>{@link #isObject(Object)}</td>
 * <td>{@link #getOobject(PdfToken)} /
 * {@link #getObject(COSObject)}</td>
 * <td>{@link #downgradeToCOSObject(PdfToken)} /
 * {@link #upgradeToCOSObject(PdfToken)}</td>
 * <td>{@link #makeOobject(PdfToken)} /
 * {@link #makeObject(COSObject)}</td>
 * </tr>
 * <tr>
 * <td>PDF operator</td>
 * <td>{@link String}</td>
 * <td>{@link PDFOperator}</td>
 * <td>{@link #isOperator(Object)}</td>
 * <td>{@link #getOperator(PdfToken)} /
 * {@link #getOperator(PDFOperator)}</td>
 * <td>{@link #asPDFOperator(PdfToken)} /
 * {@link #asPDFOperator(String)}</td>
 * <td>{@link #makeOperator(String)} /
 * {@link #makeOperator(PDFOperator)}</td>
 * </tr>
 * <tr>
 * <td>PDF string</td>
 * <td>{@link String}</td>
 * <td>{@link COSString}</td>
 * <td>{@link #isString(Object)}</td>
 * <td>{@link #getString(PdfToken)} / {@link #getString(COSString)}</td>
 * <td>{@link #asCOSString(PdfToken)} / {@link #asCOSString(String)}</td>
 * <td>{@link #makeString(String)} / {@link #makeString(COSString)}</td>
 * </tr>
 * </tbody>
 * </table>
 * @author Thib Guicherd-Callin
 * @since 1.56
 */
public class PdfBoxTokens {

  /**
   * <p>
   * An implementation of {@link PdfTokenFactory} based on PDFBox 1.6.0.
   * </p>
   * @author Thib Guicherd-Callin
   * @since 1.56
   */
  private static class Adapter implements PdfTokenFactory {

    @Override
    public PdfToken makeArray() {
      return PdfBoxTokens.makeArray();
    }

    @Override
    public PdfToken makeArray(List<PdfToken> arrayElements) {
      return PdfBoxTokens.makeArray(arrayElements);
    }

    @Override
    public PdfToken makeBoolean(boolean value) {
      return PdfBoxTokens.makeBoolean(value);
    }

    @Override
    public PdfToken makeDictionary() {
      return PdfBoxTokens.makeDictionary();
    }

    @Override
    public PdfToken makeDictionary(Map<String, PdfToken> mapping) {
      return PdfBoxTokens.makeDictionary(mapping);
    }

    @Override
    public PdfToken makeFloat(float value) {
      return PdfBoxTokens.makeFloat(value);
    }

    @Override
    public PdfToken makeInteger(long value) {
      return PdfBoxTokens.makeInteger(value);
    }

    @Override
    public PdfToken makeName(String value) {
      return PdfBoxTokens.makeName(value);
    }

    @Override
    public PdfToken makeNull() {
      return PdfBoxTokens.makeNull();
    }
    
    @Override
    public PdfToken makeObject(PdfToken value) {
      return PdfBoxTokens.makeObject(value);
    }

    @Override
    public PdfToken makeOperator(String operator) {
      return PdfBoxTokens.makeOperator(operator);
    }
    
    @Override
    public PdfToken makeString(String value) {
      return PdfBoxTokens.makeString(value);
    }
        
  }
  
  /**
   * <p>
   * A simple converter interface.
   * </p>
   * @author Thib Guicherd-Callin
   * @see #converters
   * @see #convertOne(Object) 
   */
  private interface Converter {
    
    /**
     * <p>
     * Convert a particular type of object to a PDF token.
     * </p>
     * @param obj An object.
     * @return A PDF token.
     */
    PdfToken convert(Object obj);
    
  }

  /**
   * <p>
   * An implementation of {@link PdfToken} based on PDFBox 1.6.0.
   * </p>
   * @author Thib Guicherd-Callin
   * @since 1.56
   */
  private static class Token implements PdfToken {

    /**
     * <p>
     * The object being wrapped.
     * </p>
     * @since 1.56
     */
    protected final Object token;

    /**
     * <p>{@link COSBase}-based constructor.</p>
     * @param cosBase A {@link COSBase} instance.
     * @since 1.56
     */
    private Token(COSBase cosBase) {
      this.token = cosBase;
    }
    
    /**
     * <p>{@link PDFOperator}-based constructor.</p>
     * @param pdfOperator A {@link PDFOperator} instance.
     * @since 1.56
     */
    private Token(PDFOperator pdfOperator) {
      this.token = pdfOperator;
    }
    
    @Override
    public List<PdfToken> getArray() {
      return PdfBoxTokens.getArray(this);
    }

    @Override
    public boolean getBoolean() {
      return PdfBoxTokens.getBoolean(this);
    }

    @Override
    public Map<String, PdfToken> getDictionary() {
      return PdfBoxTokens.getDictionary(this);
    }

    @Override
    public float getFloat() {
      return PdfBoxTokens.getFloat(this);
    }

    @Override
    public long getInteger() {
      return PdfBoxTokens.getInteger(this);
    }

    @Override
    public String getName() {
      return PdfBoxTokens.getName(this);
    }

    @Override
    public PdfToken getObject() {
      return PdfBoxTokens.getObject(this);
    }
    
    @Override
    public String getOperator() {
      return PdfBoxTokens.getOperator(this);
    }

    @Override
    public String getString() {
      return PdfBoxTokens.getString(this);
    }

    @Override
    public boolean isArray() {
      return PdfBoxTokens.isArray(token);
    }

    @Override
    public boolean isBoolean() {
      return PdfBoxTokens.isBoolean(token);
    }

    @Override
    public boolean isDictionary() {
      return PdfBoxTokens.isDictionary(token);
    }

    @Override
    public boolean isFloat() {
      return PdfBoxTokens.isFloat(token);
    }

    @Override
    public boolean isInteger() {
      return PdfBoxTokens.isInteger(token);
    }

    @Override
    public boolean isName() {
      return PdfBoxTokens.isName(token);
    }

    @Override
    public boolean isNull() {
      return PdfBoxTokens.isNull(token);
    }
    
    @Override
    public boolean isObject() {
      return PdfBoxTokens.isObject(token);
    }

    @Override
    public boolean isOperator() {
      return PdfBoxTokens.isOperator(token);
    }

    @Override
    public boolean isString() {
      return PdfBoxTokens.isString(token);
    }
    
    /**
     * <p>
     * Convenience cast.
     * </p>
     * @return Wrapped object cast to {@link COSBase}.
     * @since 1.56
     */
    private COSBase asCOSBase() {
      return (COSBase)token;
    }
    
    /**
     * <p>
     * Convenience cast.
     * </p>
     * @return Wrapped object cast to {@link PDFOperator}.
     * @since 1.56
     */
    private PDFOperator asPDFOperator() {
      return (PDFOperator)token;
    }
    
  }
  
  /**
   * <p>
   * A cache of {@link PDFOperator}-based {@link Token} instances.
   * </p>
   * @since 1.56
   */
  protected static final ConcurrentMap<PDFOperator, Token> cachedOperators =
      new ConcurrentHashMap<PDFOperator, Token>();

  /**
   * <p>As of this writing, COSName defines 292 common names. Other
   * names are not abundant; 400 total should be ample.</p>
   * @since 1.56
   */
  protected static final LRUMap/*<COSName, Token>*/ lruNames = new LRUMap/*<COSName, Token>*/(400);

  /**
   * <p>
   * Our PDF adapter singleton.
   * </p>
   * @since 1.56
   */
  private static final PdfTokenFactory adapterInstance = new Adapter();
  
  /**
   * <p>
   * Our singleton for {@link COSBoolean#FALSE}.
   * </p>
   * @since 1.56
   */
  private static final PdfToken FALSE = new Token(COSBoolean.FALSE);

  /**
   * <p>
   * Logger for use by this class.
   * </p>
   * @since 1.56
   */
  private static final Logger logger = Logger.getLogger(PdfBoxTokens.class);
  
  /**
   * <p>
   * Our singleton for {@link COSNull#NULL}.
   * </p>
   * @since 1.56
   */
  private static final PdfToken NULL = new Token(COSNull.NULL);
  
  /**
   * <p>
   * Our singleton for {@link COSInteger#ONE}.
   * </p>
   * @since 1.56
   */
  private static final PdfToken ONE = new Token(COSInteger.ONE);
  
  /**
   * <p>
   * Our singleton for {@link COSInteger#THREE}.
   * </p>
   * @since 1.56
   */
  private static final PdfToken THREE = new Token(COSInteger.THREE);
  
  /**
   * <p>
   * Our singleton for {@link COSBoolean#TRUE}.
   * </p>
   * @since 1.56
   */
  private static final PdfToken TRUE = new Token(COSBoolean.TRUE);
  
  /**
   * <p>
   * Our singleton for {@link COSInteger#TWO}.
   * </p>
   * @since 1.56
   */
  private static final PdfToken TWO = new Token(COSInteger.TWO);
  
  /**
   * <p>
   * Our singleton for {@link COSInteger#ZERO}.
   * </p>
   * @since 1.56
   */
  private static final PdfToken ZERO = new Token(COSInteger.ZERO);

  /**
   * <p>
   * A converter mapping.
   * </p>
   * @since 1.56
   */
  private static final Map<Class<?>, Converter> converters =
      new HashMap<Class<?>, Converter>() {{
        put(COSArray.class, new Converter() {
          @Override public PdfToken convert(Object obj) {
            return makeArray((COSArray)obj);
          }
        });
        put(COSBoolean.class, new Converter() {
          @Override public PdfToken convert(Object obj) {
            return makeBoolean((COSBoolean)obj);
          }
        });
        put(COSDictionary.class, new Converter() {
          @Override public PdfToken convert(Object obj) {
            return makeDictionary((COSDictionary)obj);
          }
        });
        put(COSFloat.class, new Converter() {
          @Override public PdfToken convert(Object obj) {
            return makeFloat((COSFloat)obj);
          }
        });
        put(COSInteger.class, new Converter() {
          @Override public PdfToken convert(Object obj) {
            return makeInteger((COSInteger)obj);
          }
        });
        put(COSName.class, new Converter() {
          @Override public PdfToken convert(Object obj) {
            return makeName((COSName)obj);
          }
        });
        put(COSNull.class, new Converter() {
          @Override public PdfToken convert(Object obj) {
            return makeNull();
          }
        });
        put(COSObject.class, new Converter() {
          @Override public PdfToken convert(Object obj) {
            return makeObject((COSObject)obj);
          }
        });
        put(PDFOperator.class, new Converter() {
          @Override public PdfToken convert(Object obj) {
            return makeOperator((PDFOperator)obj); 
          }
        });
        put(COSString.class, new Converter() {
          @Override public PdfToken convert(Object obj) {
            return makeString((COSString)obj);
          }
        });
        put(COSStream.class, new Converter() {
          @Override public PdfToken convert(Object obj) {
            logger.warning("Encountered a token of type COSStream");
            return makeNull();
          }
        });
      }};
  
  /**
   * <p>
   * Provides a PDF adapter instance.
   * </p>
   * @return A PDF adapter.
   * @since 1.56
   */
  public static PdfTokenFactory getAdapterInstance() {
    return adapterInstance;
  }
  
  /**
   * <p>
   * Converts from a list of PDF tokens to a {@link COSArray}.
   * </p>
   * @param arrayElements A list of PDF tokens.
   * @return A {@link COSArray} instance.
   * @since 1.56
   */
  protected static COSArray asCOSArray(List<PdfToken> arrayElements) {
    COSArray cosArray = new COSArray();
    for (PdfToken pdfToken : arrayElements) {
      Token token = asToken(pdfToken);
      cosArray.add(token.asCOSBase());
    }
    return cosArray;
  }
  
  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isArray()}
   * is <b>true</b> to a {@link COSArray}.
   * </p>
   * @param pdfToken A PDF token.
   * @return The {@link COSArray} instance represented by the token.
   * @since 1.56
   */
  protected static COSArray asCOSArray(PdfToken pdfToken) {
    return (COSArray)asCOSBase(pdfToken);
  }
  
  /**
   * <p>
   * Converts from a <code>boolean</code> to a {@link COSBoolean}.
   * </p>
   * @param value A value.
   * @return A {@link COSBoolean} instance.
   * @since 1.56
   */
  protected static COSBoolean asCOSBoolean(boolean value) {
    return value ? COSBoolean.TRUE : COSBoolean.FALSE;
  }
  
  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isBoolean()}
   * is <b>true</b> to a {@link COSBoolean}.
   * </p>
   * @param pdfToken A PDF token.
   * @return The {@link COSBoolean} instance represented by the token.
   * @since 1.56
   */
  protected static COSBoolean asCOSBoolean(PdfToken token) {
    return (COSBoolean)asCOSBase(token);
  }

  /**
   * <p>
   * Converts from a map from strings to PDF tokens to a 
   * {@link COSDictionary}.
   * </p>
   * @param mapping A map from strings (PDF names) to PDF tokens.
   * @return A {@link COSDictionary} instance.
   * @since 1.56
   */
  protected static COSDictionary asCOSDictionary(Map<String, PdfToken> mapping) {
    COSDictionary cosDictionary = new COSDictionary();
    for (Map.Entry<String, PdfToken> entry : mapping.entrySet()) {
      cosDictionary.setItem(asCOSName(entry.getKey()), asCOSBase(entry.getValue()));
    }
    return cosDictionary;
  }

  /**
   * <p>
   * Converts from a PDF token for which
   * {@link PdfToken#isDictionary()} is <b>true</b> to a
   * {@link COSDictionary}.
   * </p>
   * @param pdfToken A PDF token.
   * @return The {@link COSDictionary} instance represented by the
   *         token.
   * @since 1.56
   */
  protected static COSDictionary asCOSDictionary(PdfToken token) {
    return (COSDictionary)asCOSBase(token);
  }
  
  /**
   * <p>
   * Converts from a <code>float</code> to a {@link COSFloat}.
   * </p>
   * @param value A value.
   * @return A {@link COSFloat} instance.
   * @since 1.56
   */
  protected static COSFloat asCOSFloat(float value) {
    return new COSFloat(value);
  }

  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isFloat()}
   * is <b>true</b> to a {@link COSFloat}.
   * </p>
   * @param pdfToken A PDF token.
   * @return The {@link COSFloat} instance represented by the token.
   * @since 1.56
   */
  protected static COSFloat asCOSFloat(PdfToken pdfToken) {
    return (COSFloat)asCOSBase(pdfToken);
  }
  
  /**
   * <p>
   * Converts from a <code>long</code> to a {@link COSInteger}.
   * </p>
   * @param value A value.
   * @return A {@link COSInteger} instance.
   * @since 1.56
   */
  protected static COSInteger asCOSInteger(long value) {
    return COSInteger.get(value);
  }
  
  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isInteger()}
   * is <b>true</b> to a {@link COSInteger}.
   * </p>
   * @param pdfToken A PDF token.
   * @return The {@link COSInteger} instance represented by the token.
   * @since 1.56
   */
  protected static COSInteger asCOSInteger(PdfToken pdfToken) {
    return (COSInteger)asCOSBase(pdfToken);
  }

  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isName()}
   * is <b>true</b> to a {@link COSName}.
   * </p>
   * @param pdfToken A PDF token.
   * @return The {@link COSName} instance represented by the token.
   * @since 1.56
   */
  protected static COSName asCOSName(PdfToken pdfToken) {
    return (COSName)asCOSBase(pdfToken);
  }
  
  /**
   * <p>
   * Converts from a string to a {@link COSName}.
   * </p>
   * @param value A value.
   * @return A {@link COSName} instance.
   * @since 1.56
   */
  protected static COSName asCOSName(String value) {
    return COSName.getPDFName(value);
  }

  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isNull()}
   * is <b>true</b> to a {@link COSNull}.
   * </p>
   * @param pdfToken A PDF token.
   * @return The {@link COSNull} instance represented by the token.
   * @since 1.56
   */
  protected static COSNull asCOSNull(PdfToken pdfToken) {
    return (COSNull)asCOSBase(pdfToken);
  }
  
  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isString()}
   * is <b>true</b> to a {@link COSString}.
   * </p>
   * @param pdfToken A PDF token.
   * @return The {@link COSString} instance represented by the token.
   * @since 1.56
   */
  protected static COSString asCOSString(PdfToken pdfToken) {
    return (COSString)asCOSBase(pdfToken);
  }

  /**
   * <p>
   * Converts from a {@link String} to a {@link COSString}.
   * </p>
   * @param value A value.
   * @return A {@link COSFloat} instance.
   * @since 1.56
   */
  protected static COSString asCOSString(String value) {
    return new COSString(value);
  }
  
  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isObject()}
   * is <b>true</b> to a {@link COSObject}.
   * </p>
   * @param pdfToken A PDF token.
   * @return The {@link COSObject} instance represented by the token.
   * @since 1.56.3
   */
  protected static COSObject downgradeToCOSObject(PdfToken pdfToken) {
    return (COSObject)asCOSBase(pdfToken);
  }
  
  /**
   * <p>
   * Converts from a PDF token to a {@link COSObject}.
   * </p>
   * @param operator An operator.
   * @return A {@link PDFOperator} instance.
   * @since 1.56.3
   */
  protected static COSObject upgradeToCOSObject(PdfToken value) {
    try {
      return new COSObject(asCOSBase(value));
    }
    catch (IOException ioe) {
      /*
       * IMPLEMENTATION NOTE
       * 
       * As it turns out, IOException can never be thrown even though
       * the signatures of COSObject.COSObject() (PDFBox 1.6.0:
       * COSObject line 42) COSObject.setObject() (line 99) say it
       * might.
       */
      logger.warning("Error while converting to COSObject", ioe);
      return null;
    }
  }

  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isOperator()}
   * is <b>true</b> to a {@link PDFOperator}.
   * </p>
   * @param pdfToken A PDF token.
   * @return The {@link PDFOperator} instance represented by the token.
   * @since 1.56
   */
  protected static PDFOperator asPDFOperator(PdfToken pdfToken) {
    return asToken(pdfToken).asPDFOperator();
  }
  
  /**
   * <p>
   * Converts from a string to a {@link PDFOperator}.
   * </p>
   * @param operator An operator.
   * @return A {@link PDFOperator} instance.
   * @since 1.56
   */
  protected static PDFOperator asPDFOperator(String operator) {
    return PDFOperator.getOperator(operator);
  }

  /**
   * <p>
   * Turns a list of objects that are known to all be internal types
   * (e.g. {@link COSInteger}, {@link PDFOperator}, etc.) into a list
   * of {@link PdfToken} instances.
   * </p>
   * 
   * @param listObj A list of objects to be wrapped.
   * @return A list of wrapped objects.
   * @since 1.56
   */
  protected static List<PdfToken> convertList(List<Object> listObj) {
    List<PdfToken> ret = new ArrayList<PdfToken>(listObj.size());
    for (Object obj : listObj) {
      ret.add(convertOne(obj));
    }
    return ret;
  }
  
  /**
   * <p>
   * Turns one object that is known to be of an internal type (e.g.
   * {@link COSInteger}, {@link PDFOperator}, etc.) into a
   * {@link PdfToken} instance.
   * </p>
   * @param obj An object to be wrapped.
   * @return A wrapped object.
   * @since 1.56
   */
  protected static PdfToken convertOne(Object obj) {
    if (obj == null) {
      return makeNull();
    }
    Converter converter = converters.get(obj.getClass());
    if (converter == null) {
      logger.warning("Encountered a token of unexpected type: " + obj.getClass().getCanonicalName());
      return null; // Controversial
    }
    return converter.convert(obj);
  }
  
  /**
   * <p>
   * Converts from a {@link COSArray} to a list of PDF tokens.
   * </p>
   * @param cosArray A {@link COSArray} instance.
   * @return A list of PDF tokens.
   * @since 1.56
   */
  protected static List<PdfToken> getArray(COSArray cosArray) {
    List<PdfToken> ret = new ArrayList<PdfToken>(cosArray.size());
    for (COSBase cosBase : cosArray) {
      ret.add(convertOne(cosBase));
    }
    return ret;
  }
  
  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isArray()}
   * is <b>true</b> to a list of PDF tokens.
   * </p>
   * @param pdfToken A PDF token.
   * @return A list of PDF tokens.
   * @since 1.56
   */
  protected static List<PdfToken> getArray(PdfToken pdfToken) {
    return getArray((COSArray)asCOSBase(pdfToken));
  }
  
  /**
   * <p>
   * Converts from a {@link COSBoolean} to a <code>boolean</code>
   * value.
   * </p>
   * @param cosBoolean A {@link COSBoolean} instance.
   * @return A <code>boolean</code> value.
   * @since 1.56
   */
  protected static boolean getBoolean(COSBoolean cosBoolean) {
    return cosBoolean.getValue();
  }
  
  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isBoolean()}
   * is <b>true</b> to a <code>boolean</code> value.
   * </p>
   * @param pdfToken A PDF token.
   * @return A <code>boolean</code> value.
   * @since 1.56
   */
  protected static boolean getBoolean(PdfToken pdfToken) {
    return getBoolean((COSBoolean)asCOSBase(pdfToken));
  }
  
  /**
   * <p>
   * Converts from a {@link COSDictionary} to a map from strings
   * (PDF names) to PDF tokens.
   * </p>
   * @param cosDictionary A {@link COSDictionary} instance.
   * @return A map from string to PDF tokens.
   * @since 1.56
   */
  protected static Map<String, PdfToken> getDictionary(COSDictionary cosDictionary) {
    Map<String, PdfToken> ret = new LinkedHashMap<String, PdfToken>();
    for (Map.Entry<COSName, COSBase> entry : cosDictionary.entrySet()) {
      ret.put(getName(entry.getKey()), convertOne(entry.getValue()));
    }
    return ret;
  }
  
  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isDictionary()}
   * is <b>true</b> to a map from strings (PDF names) to PDF tokens.
   * </p>
   * @param pdfToken A PDF token.
   * @return A map from strings (PDF names) to PDF tokens.
   * @since 1.56
   */
  protected static Map<String, PdfToken> getDictionary(PdfToken pdfToken) {
    return getDictionary((COSDictionary)asCOSBase(pdfToken));
  }
  
  /**
   * <p>
   * Converts from a {@link COSFloat} to a <code>float</code>
   * value.
   * </p>
   * @param cosFloat A {@link COSFloat} instance.
   * @return A <code>float</code> value.
   * @since 1.56
   */
  protected static float getFloat(COSFloat cosFloat) {
    return cosFloat.floatValue();
  }

  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isFloat()}
   * is <b>true</b> to a <code>float</code> value.
   * </p>
   * @param pdfToken A PDF token.
   * @return A <code>float</code> value.
   * @since 1.56
   */
  protected static float getFloat(PdfToken pdfToken) {
    return getFloat(asCOSFloat(pdfToken));
  }
  
  /**
   * <p>
   * Converts from a {@link COSInteger} to a <code>long</code>
   * value.
   * </p>
   * @param cosInteger A {@link COSInteger} instance.
   * @return A <code>long</code> value.
   * @since 1.56
   */
  protected static long getInteger(COSInteger cosInteger) {
    return cosInteger.longValue();
  }
  
  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isInteger()}
   * is <b>true</b> to a <code>long</code> value.
   * </p>
   * @param pdfToken A PDF token.
   * @return A <code>long</code> value.
   * @since 1.56
   */
  protected static long getInteger(PdfToken pdfToken) {
    return getInteger(asCOSInteger(pdfToken));
  }

  /**
   * <p>
   * Converts from a {@link COSName} to a string value.
   * </p>
   * @param cosName A {@link COSName} instance.
   * @return A string value.
   * @since 1.56
   */
  protected static String getName(COSName cosName) {
    return cosName.getName();
  }

  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isName()}
   * is <b>true</b> to a string value.
   * </p>
   * @param pdfToken A PDF token.
   * @return A string value.
   * @since 1.56
   */
  protected static String getName(PdfToken pdfToken) {
    return getName(asCOSName(pdfToken));
  }
  
  /**
   * <p>
   * Converts from a {@link COSObject} to a PDF token value.
   * </p>
   * @param cosObject A {@link COSObject} instance.
   * @return A PDFToken value.
   * @since 1.56.3
   */
  protected static PdfToken getObject(COSObject cosObject) {
    return convertOne(cosObject.getObject());
  }
  
  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isOperator()}
   * is <b>true</b> to a string value.
   * </p>
   * @param pdfToken A PDF token.
   * @return A string value.
   * @since 1.56.3
   */
  protected static PdfToken getObject(PdfToken pdfToken) {
    return getObject(downgradeToCOSObject(pdfToken));
  }

  /**
   * <p>
   * Converts from a {@link PDFOperator} to a string value.
   * </p>
   * @param pdfOperator A {@link PDFOperator} instance.
   * @return A string value.
   * @since 1.56
   */
  protected static String getOperator(PDFOperator pdfOperator) {
    return pdfOperator.getOperation();
  }
  
  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isOperator()}
   * is <b>true</b> to a string value.
   * </p>
   * @param pdfToken A PDF token.
   * @return A string value.
   * @since 1.56
   */
  protected static String getOperator(PdfToken pdfToken) {
    return getOperator(asPDFOperator(pdfToken));
  }

  /**
   * <p>
   * Converts from a {@link COSString} to a string value.
   * </p>
   * @param cosString A {@link COSString} instance.
   * @return A string value.
   * @since 1.56
   */
  protected static String getString(COSString cosString) {
    return cosString.getString();
  }

  /**
   * <p>
   * Converts from a PDF token for which {@link PdfToken#isString()}
   * is <b>true</b> to a string value.
   * </p>
   * @param pdfToken A PDF token.
   * @return A string value.
   * @since 1.56
   */
  protected static String getString(PdfToken pdfToken) {
    return getString((COSString)asCOSBase(pdfToken));
  }

  /**
   * <p>
   * Determines if an object of an internal type is a PDF array.
   * </p>
   * @param obj An object (internal type).
   * @return <code>true</code> if and only if the object is a PDF
   *         array.
   * @since 1.56
   */
  protected static boolean isArray(Object obj) {
    return obj instanceof COSArray;
  }

  /**
   * <p>
   * Determines if an object of an internal type is a PDF boolean.
   * </p>
   * @param obj An object (internal type).
   * @return <code>true</code> if and only if the object is a PDF
   *         boolean.
   * @since 1.56
   */
  protected static boolean isBoolean(Object obj) {
    return obj instanceof COSBoolean;
  }
    
  /**
   * <p>
   * Determines if an object of an internal type is a PDF dictionary.
   * </p>
   * @param obj An object (internal type).
   * @return <code>true</code> if and only if the object is a PDF
   *         dictionary.
   * @since 1.56
   */
  protected static boolean isDictionary(Object obj) {
    return obj instanceof COSDictionary;
  }
    
  /**
   * <p>
   * Determines if an object of an internal type is a PDF float.
   * </p>
   * @param obj An object (internal type).
   * @return <code>true</code> if and only if the object is a PDF
   *         float.
   * @since 1.56
   */
  protected static boolean isFloat(Object obj) {
    return obj instanceof COSFloat;
  }

  /**
   * <p>
   * Determines if an object of an internal type is a PDF integer.
   * </p>
   * @param obj An object (internal type).
   * @return <code>true</code> if and only if the object is a PDF
   *         integer.
   * @since 1.56
   */
  protected static boolean isInteger(Object obj) {
    return obj instanceof COSInteger;
  }

  /**
   * <p>
   * Determines if an object of an internal type is a PDF name.
   * </p>
   * @param obj An object (internal type).
   * @return <code>true</code> if and only if the object is a PDF
   *         name.
   * @since 1.56
   */
  protected static boolean isName(Object obj) {
    return obj instanceof COSName;
  }
  
  /**
   * <p>
   * Determines if an object of an internal type is the PDF null
   * object.
   * </p>
   * @param obj An object (internal type).
   * @return <code>true</code> if and only if the object is the PDF
   *         null object.
   * @since 1.56
   */
  protected static boolean isNull(Object obj) {
    return obj instanceof COSNull;
  }

  /**
   * <p>
   * Determines if an object of an internal type is a PDF object.
   * </p>
   * @param obj An object (internal type).
   * @return <code>true</code> if and only if the object is a PDF
   *         object.
   * @since 1.56.3
   */
  protected static boolean isObject(Object obj) {
    return obj instanceof COSObject;
  }
  
  /**
   * <p>
   * Determines if an object of an internal type is a PDF operator.
   * </p>
   * @param obj An object (internal type).
   * @return <code>true</code> if and only if the object is a PDF
   *         operator.
   * @since 1.56
   */
  protected static boolean isOperator(Object obj) {
    return obj instanceof PDFOperator;
  }
  
  /**
   * <p>
   * Determines if an object of an internal type is a PDF string.
   * </p>
   * @param obj An object (internal type).
   * @return <code>true</code> if and only if the object is a PDF
   *         string.
   * @since 1.56
   */
  protected static boolean isString(Object obj) {
    return obj instanceof COSString;
  }
  
  /**
   * <p>
   * Convenience method to make an empty PDF array.
   * </p>
   * @return An empty PDF array.
   * @since 1.56
   */
  protected static PdfToken makeArray() {
    return makeArray(new COSArray());
  }

  /**
   * <p>
   * Wraps a {@link COSArray} instance as a PDF token.
   * </p>
   * @param cosArray A {@link COSArray} instance.
   * @return A PDF token.
   * @since 1.56
   */
  protected static PdfToken makeArray(COSArray cosArray) {
    return new Token(cosArray);
  }

  /**
   * <p>
   * Creates a PDF array from the given list of PDF tokens.
   * </p>
   * @param arrayElements A list of PDF tokens.
   * @return A PDF token.
   * @since 1.56
   */
  protected static PdfToken makeArray(List<PdfToken> arrayElements) {
    COSArray cosArray = new COSArray();
    for (PdfToken pdfToken : arrayElements) {
      cosArray.add(asCOSBase(pdfToken));
    }
    return makeArray(cosArray);
  }

  /**
   * <p>
   * Creates a PDF boolean from the given value.
   * </p>
   * @param value A <code>boolean</code> value.
   * @return A PDF token.
   * @since 1.56
   */
  protected static PdfToken makeBoolean(boolean value) {
    return (value ? TRUE : FALSE);
  }
  
  /**
   * <p>
   * Wraps a {@link COSBoolean} instance as a PDF token.
   * </p>
   * @param cosArray A {@link COSBoolean} instance.
   * @return A PDF token.
   * @since 1.56
   */
  protected static PdfToken makeBoolean(COSBoolean cosBoolean) {
    return makeBoolean(getBoolean(cosBoolean));
  }

  /**
   * <p>
   * Convenience method to make an empty PDF dictionary.
   * </p>
   * @return An empty PDF dictionary.
   * @since 1.56
   */
  protected static PdfToken makeDictionary() {
    return makeDictionary(new COSDictionary());
  }
  
  /**
   * <p>
   * Wraps a {@link COSDictionary} instance as a PDF token.
   * </p>
   * @param cosArray A {@link COSDictionary} instance.
   * @return A PDF token.
   * @since 1.56
   */
  protected static PdfToken makeDictionary(COSDictionary cosDictionary) {
    return new Token(cosDictionary);
  }
  
  /**
   * <p>
   * Creates a PDF array from the given map from strings (PDF names)
   * to PDF tokens.
   * </p>
   * @param mapping A map from strings to PDF tokens.
   * @return A PDF token.
   * @since 1.56
   */
  protected static PdfToken makeDictionary(Map<String, PdfToken> mapping) {
    return makeDictionary(asCOSDictionary(mapping));
  }
  
  /**
   * <p>
   * Wraps a {@link COSFloat} instance as a PDF token.
   * </p>
   * @param cosArray A {@link COSFloat} instance.
   * @return A PDF token.
   * @since 1.56
   */
  protected static PdfToken makeFloat(COSFloat cosFloat) {
    return new Token(cosFloat);
  }

  /**
   * <p>
   * Creates a PDF float from the given value.
   * </p>
   * @param value A <code>float</code> value.
   * @return A PDF token.
   * @since 1.56
   */
  protected static PdfToken makeFloat(float value) {
    return makeFloat(asCOSFloat(value));
  }
  
  /**
   * <p>
   * Wraps a {@link COSInteger} instance as a PDF token.
   * </p>
   * @param cosArray A {@link COSInteger} instance.
   * @return A PDF token.
   * @since 1.56
   */
  protected static PdfToken makeInteger(COSInteger cosInteger) {
    long value = getInteger(cosInteger);
    if (0L <= value && value <= 3L) {
      // 'switch' truncates, but this really is 0, 1, 2 or 3 
      switch ((int)value) {
        case 0: return ZERO;
        case 1: return ONE;
        case 2: return TWO;
        case 3: return THREE;
      }
    }
    return new Token(cosInteger);
  }
  
  /**
   * <p>
   * Creates a PDF integer from the given value.
   * </p>
   * @param value A <code>long</code> value.
   * @return A PDF token.
   * @since 1.56
   */
  protected static PdfToken makeInteger(long value) {
    return makeInteger(asCOSInteger(value));
  }
  
  /**
   * <p>
   * Wraps a {@link COSName} instance as a PDF token.
   * </p>
   * @param cosArray A {@link COSName} instance.
   * @return A PDF token.
   * @since 1.56
   */
  protected synchronized static PdfToken makeName(COSName cosName) {
    Token ret = (Token)lruNames.get(cosName);
    if (ret == null) {
      ret = new Token(cosName);
      lruNames.put(cosName, ret);
    }
    return ret;
  }
  
  /**
   * <p>
   * Creates a PDF name from the given value.
   * </p>
   * @param value A string value.
   * @return A PDF token.
   * @since 1.56
   */
  protected static PdfToken makeName(String value) {
    return makeName(asCOSName(value));
  }
  
  /**
   * <p>
   * Convenience method to obtain the PDF null object.
   * </p>
   * @return The PDF null object.
   * @since 1.56
   */
  protected static PdfToken makeNull() {
    return NULL;
  }
  
  /**
   * <p>
   * Wraps a {@link COSObject} instance as a PDF token.
   * </p>
   * @param cosObject A {@link COSObject} instance.
   * @return A PDF token.
   * @since 1.56.3
   */
  protected static PdfToken makeObject(COSObject cosObject) {
    return new Token(cosObject);
  }
  
  /**
   * <p>
   * Creates a PDF object from the given value.
   * </p>
   * @param value A PDF token value.
   * @return A PDF token.
   * @since 1.56.3
   */
  protected static PdfToken makeObject(PdfToken value) {
    return convertOne(unwrapOne(value));
  }
  
  /**
   * <p>
   * Wraps a {@link PDFOperator} instance as a PDF token.
   * </p>
   * @param operator A {@link PDFOperator} instance.
   * @return A PDF token.
   * @since 1.56
   */
  protected static PdfToken makeOperator(PDFOperator operator) {
    Token cachedToken = cachedOperators.get(operator);
    if (cachedToken != null) {
      // Operator already cached
      return cachedToken;
    }
    Token newToken = new Token(operator);

    /*
     * IMPLEMENTATION NOTE
     * 
     * 'BI' and 'ID' operators are never cached by PDFBox because
     * they contain their own image data, so we should not cache
     * them either. (PDFBox 1.6.0: PDFOperator.getOperator() line
     * 63).
     */
    String opcode = operator.getOperation();
    if (   PdfOpcodes.BEGIN_IMAGE_OBJECT.equals(opcode)
        || PdfOpcodes.BEGIN_IMAGE_DATA.equals(opcode)) {
      // Don't cache 'BI' and 'ID' operators
      return newToken;
    }

    // Operator not cached yet
    cachedToken = cachedOperators.putIfAbsent(operator, newToken);
    return (cachedToken == null) ? newToken : cachedToken;
  }
  
  /**
   * <p>
   * Creates a PDF operator from the given value.
   * </p>
   * @param value A string value.
   * @return A PDF token.
   * @since 1.56
   */
  protected static PdfToken makeOperator(String operator) {
    return makeOperator(asPDFOperator(operator));
  }
  
  /**
   * <p>
   * Wraps a {@link COSString} instance as a PDF token.
   * </p>
   * @param cosArray A {@link COSString} instance.
   * @return A PDF token.
   * @since 1.56
   */
  protected static PdfToken makeString(COSString cosString) {
    return new Token(cosString);
  }
  
  /**
   * <p>
   * Creates a PDF string from the given value.
   * </p>
   * @param value A string value.
   * @return A PDF token.
   * @since 1.56
   */
  protected static PdfToken makeString(String value) {
    return makeString(asCOSString(value));
  }

  /**
   * <p>
   * Unwraps a list of PDF tokens (that are really of type
   * {@link Token}) into a list of the inner objects.
   * </p>
   * 
   * @param listTokens A list of PDF tokens of type {@link Token}.
   * @return The list of the objects wrapped in the argument list.
   * @since 1.56.3
   */
  protected static List<Object> unwrapList(final List<PdfToken> listTokens) {
    List<Object> ret = new ArrayList<Object>(listTokens.size());
    for (PdfToken tok : listTokens) {
      ret.add(unwrapOne(tok));
    }
    return ret;
  }
  
  /**
   * <p>
   * Unwraps one PDF token (that is really of our type {@link Token})
   * to reveal its inner object.
   * </p>
   * @param pdfToken A PDF token of type {@link Token}.
   * @return The object wrapped by the argument.
   * @since 1.56.3
   */
  protected static Object unwrapOne(PdfToken pdfToken) {
    return asToken(pdfToken).token;
  }
  
  /**
   * <p>
   * Unwraps the token and casts it to {@COSBase}.
   * </p>
   * @param pdfToken A PDF token (of type {@link Token}).
   * @return The unwrapped token as {@link COSBase}.
   */
  private static COSBase asCOSBase(PdfToken pdfToken) {
    return asToken(pdfToken).asCOSBase();
  }

  /**
   * <p>
   * Convenience cast.
   * </p>
   * @param pdfToken A PDF token (of type {@link Token}).
   * @return The {@link PDfToken} token cast to {@link Token}.
   */
  private static Token asToken(PdfToken token) {
    return (Token)token;
  }

}
