/*
 * $Id: IcpProtocolException.java,v 1.5 2006-01-31 01:29:19 thib_gc Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.proxy.icp;

import java.net.ProtocolException;

/**
 * <p>An exception class for all errors related to the ICP
 * protocol.</p>
 * @author Thib Guicherd-Callin
 */
public class IcpProtocolException extends ProtocolException {

  /**
   * <p>Builds a new IcpProtocolException instance.</p>
   */
  public IcpProtocolException() {
    super();
  }

  /**
   * <p>Builds a new IcpProtocolException instance
   * with the given message.</p>
   */
  public IcpProtocolException(String message) {
    super(message);
  }

  /**
   * <p>Builds a new IcpProtocolException instance
   * with the given message and cause.</p>
   */
  public IcpProtocolException(String message, Throwable cause) {
    super(message);
    initCause(cause);
  }

  /**
   * <p>Builds a new IcpProtocolException instance
   * with the given cause.</p>
   */
  public IcpProtocolException(Throwable cause) {
    super();
    initCause(cause);
  }

}
