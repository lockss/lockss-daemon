/*
 * $Id$
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
package org.lockss.devtools.plugindef;

import javax.swing.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: SUL-LOCKSS</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class TextInputVerifer {

  static public class IntegerVerifier extends InputVerifier {
    /**
     * verify
     *
     * @param input JComponent
     * @return boolean
     */
    public boolean verify(JComponent input) {
      if (! (input instanceof JTextField)) {
        return true;
      }
      String val = ( (JTextField) input).getText();
      try {
        Integer.parseInt(val);
        return true;
      }
      catch (NumberFormatException nfe) {
        return false;
      }
    }

  }

  static public class UnsignedIntegerVerifier extends InputVerifier {
    /**
     * verify
     *
     * @param input JComponent
     * @return boolean
     */
    public boolean verify(JComponent input) {
      if (! (input instanceof JTextField)) {
        return true;
      }
      String val = ( (JTextField) input).getText();
      try {
        return Integer.parseInt(val) >= 0;
      }
      catch (NumberFormatException nfe) {
        return false;
      }
    }
  }

  static public class YearVerifier extends InputVerifier {
    /**
     * verify
     *
     * @param input JComponent
     * @return boolean
     */
    public boolean verify(JComponent input) {
      if (! (input instanceof JTextField)) {
        return true;
      }
      String val = ( (JTextField) input).getText();
      if (val.length() == 4) {
        try {
          return Integer.parseInt(val) > 0;
        }
        catch (NumberFormatException fe) {
        }
      }
      return false;
    }
  }
}
