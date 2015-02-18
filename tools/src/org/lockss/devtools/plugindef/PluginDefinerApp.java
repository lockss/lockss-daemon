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

import org.lockss.config.ConfigManager;
import org.lockss.util.*;

import java.util.List;
import java.awt.*;
import java.net.URL;

public class PluginDefinerApp {

  protected static Logger logger = Logger.getLogger("PluginDefinerApp");

  boolean packFrame = true;

  //Construct the application
  public PluginDefinerApp() {
    logger.info("Starting plugin tool");
    PluginDefiner frame = new PluginDefiner();
    //Validate frames that have preset sizes
    //Pack frames that have useful preferred size info, e.g. from their layout
    if (packFrame) {
      frame.pack();
    }
    else {
      frame.validate();
    }
    //Center the window
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension frameSize = frame.getSize();
    if (frameSize.height > screenSize.height) {
      frameSize.height = screenSize.height;
    }
    if (frameSize.width > screenSize.width) {
      frameSize.width = screenSize.width;
    }
    frame.setLocation((screenSize.width - frameSize.width) / 2,
                      (screenSize.height - frameSize.height) / 2);
    frame.setVisible(true);
    logger.info("Plugin tool started");
  }

  //Main method
  public static void main(String[] args) {
    try {
      initConfig();
      JFrame.setDefaultLookAndFeelDecorated(true);
      JDialog.setDefaultLookAndFeelDecorated(true);
    }
    catch (Exception exc) {
      logger.debug("Error while setting up look and feel decorations");
    }
    new PluginDefinerApp();
  }

  protected static void initConfig() {
    // Kludge
    Logger lll = Logger.getLoggerWithInitialLevel("PluginTool", Logger.LEVEL_INFO);
    lll.info("Plugin Tool starting up");

    // Get resources from JAR "plugin-tool-props.xml"
    ClassLoader classLoader = PluginDefinerApp.class.getClassLoader();
    URL propsFile = classLoader.getResource("plugin-tool-props.xml");
    if (propsFile != null) {
      List propsUrls = ListUtil.list(propsFile.toString(), "plugin-tool-props.opt");
      ConfigManager configMgr = ConfigManager.makeConfigManager(propsUrls);
      configMgr.initService(null);
      configMgr.startService();
      configMgr.waitConfig();
    }
  }

}
