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

package org.lockss.pdf;

import java.util.*;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.iterators.*;

/**
 * <p>
 * Abstraction for a PDF token stream. In the PDF hierarchy
 * represented by this package, a PDF token stream is in a PDF page
 * and is a sequence of PDF tokens ({@link PdfToken}).
 * </p>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.56
 */
public interface PdfTokenStream {

  /**
   * 
   * @return
   * @throws PdfException
   * @since 1.76
   * @see #getOperandsAndOperatorIterator()
   */
  default Iterable<? extends PdfOperandsAndOperator<? extends PdfToken>> getOperandsAndOperatorIterable() throws PdfException {
    return IteratorUtils.asIterable(getOperandsAndOperatorIterator());
  }

  /**
   * 
   * @return
   * @throws PdfException
   * @since 1.76
   */
  Iterator<? extends PdfOperandsAndOperator<? extends PdfToken>> getOperandsAndOperatorIterator() throws PdfException;

  /**
   * 
   * @return
   * @throws PdfException
   * @since 1.76
   * @see #getOperandsAndOperatorIterator()
   */
  default List<? extends PdfOperandsAndOperator<? extends PdfToken>> getOperandsAndOperatorList() throws PdfException {
    return IteratorUtils.toList(getOperandsAndOperatorIterator());
  }
  
  /**
   * <p>
   * Returns the PDF page associated with this PDF token stream.
   * </p>
   * 
   * @return The parent PDF page.
   * @since 1.56
   */
  PdfPage getPage();

  /**
   * <p>
   * Returns an iterable (suitable for a "for each" loop) of tokens in this
   * token stream. See {@link #getTokenIterator()} for details.
   * <p>
   * 
   * @return An iterable of tokens in this token stream.
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.76
   * @see #getTokenIterator()
   */
  default Iterable<? extends PdfToken> getTokenIterable() throws PdfException {
    return IteratorUtils.asIterable(getTokenIterator());
  }

  /**
   * <p>
   * Returns an iterator of tokens in this token stream.
   * <p>
   * 
   * @return
   * @throws PdfException
   * @since 1.76
   * @see #getOperandsAndOperatorIterator()
   */
  default Iterator<? extends PdfToken> getTokenIterator() throws PdfException {
    return new LazyIteratorChain<PdfToken>() {
      private final Iterator<? extends PdfOperandsAndOperator<? extends PdfToken>> oaoIter = getOperandsAndOperatorIterator();
      @Override
      protected Iterator<PdfToken> nextIterator(int count) {
        if (!oaoIter.hasNext()) {
          return null;
        }
        PdfOperandsAndOperator<? extends PdfToken> oao = oaoIter.next();
        return new IteratorChain<>(oao.getOperands().iterator(),
                                   new SingletonIterator<>(oao.getOperator()));
      }
    };
  }
  
  /**
   * <p>
   * <b>Deprecated.</b> Renamed to {@link #getTokenList()} in 1.76.
   * </p>
   * <p>
   * This method can be memory-intensive; {@link #getTokenIterable()} or
   * {@link #getTokenIterator()} are recommended instead. In particular,
   * {@code for (PdfToken tok : mystrm.getTokens())} or {@code for (PdfToken tok : mystrm.getTokenList())} should be replaced with
   * {@code for (PdfToken tok : mystrm.getTokenIterable())}.
   * </p>
   * 
   * @return A list of tokens (possibly empty).
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.67?
   * @deprecated Renamed to {@link #getTokenList()} in 1.76.
   * @see #getTokenIterator()
   */
  default List<PdfToken> getTokens() throws PdfException {
    return IteratorUtils.toList(getTokenIterator());
  }

  /**
   * <p>
   * Returns a list of tokens contained in this token stream. See
   * {@lin #getTokenIterator()} for details.
   * </p>
   * <p>
   * Note that changing the resulting list does not change the tokens of the
   * stream; only a call to {@link #setTokens(List)} does.
   * </p>
   * <p>
   * This method can be memory-intensive; {@link #getTokenIterable()} or
   * {@link #getTokenIterator()} are recommended instead. In particular,
   * {@code for (PdfToken tok : mystrm.getTokens())} should be replaced with
   * {@code for (PdfToken tok : mystrm.getTokenIterable())}.
   * </p>
   * 
   * @return A list of tokens (possibly empty).
   * @throws PdfException
   *           If PDF processing fails.
   * @since 1.76
   * @see #getTokenIterator()
   */
  default List<? extends PdfToken> getTokenList() throws PdfException {
    return IteratorUtils.toList(getTokenIterator());
  }

  /**
   * <p>
   * Replaces the tokens in the token stream with those from the given list.
   * </p>
   * 
   * @param tokenList A list of PDF tokens for this stream.
   * @throws PdfException If PDF processing fails.
   * @since ?
   */
  default void setTokens(List<? extends PdfToken> tokenList) throws PdfException {
    setTokens(tokenList.iterator());
  }
  
  /**
   * <p>
   * Replaces the tokens in the token stream with those from the given iterator.
   * </p>
   * 
   * @param tokenIterator An iterator of tokens for this stream.
   * @throws PdfException If PDF processing fails.
   * @since 1.76
   */
  void setTokens(Iterator<? extends PdfToken> tokenIterator) throws PdfException;
  
}
