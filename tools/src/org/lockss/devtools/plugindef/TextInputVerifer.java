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
