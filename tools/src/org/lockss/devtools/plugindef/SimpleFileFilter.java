package org.lockss.devtools.plugindef;

import javax.swing.filechooser.*;
import java.io.File;

public class SimpleFileFilter extends FileFilter {
  String[] extensions;
  String description;

  public SimpleFileFilter(String ext) {
    this (new String[] {ext} , null);
  }

  public SimpleFileFilter(String[] exts, String descr) {
    extensions = new String[exts.length];
    for(int i=exts.length -1; i>=0; i--) {
      extensions[i] = exts[i].toLowerCase();
    }

    description = (descr == null ? exts[0] + " files" : descr);
  }

  /**
   * accept
   *
   * @param f File
   * @return boolean
   */
  public boolean accept(File f) {
    if(f.isDirectory()) {
      return true;
    }
    String name = f.getName().toLowerCase();
    for (int i = extensions.length-1; i>=0; i--) {
      if(name.endsWith(extensions[i])) {
        return true;
      }
    }
    return false;
  }

  /**
   * getDescription
   *
   * @return String
   */
  public String getDescription() {
    return description;
  }

}
