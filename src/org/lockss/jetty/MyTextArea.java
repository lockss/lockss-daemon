package org.lockss.jetty;
import java.io.*;
import org.mortbay.html.*;
import org.lockss.util.*;

/** Specialization of TextArea to work around a bug in Lynx.  A newline
 * inside the closing textarea tag causes Lynx to include the rest of the
 * html on the page in the textarea, so suppress the newline in this
 * case. */
public class MyTextArea extends TextArea {
  static Logger log = Logger.getLogger("IpAccessServlet");

  private String mytag;			// private in Block, dammit

  /** @param name The name of the TextArea within the form */
  public MyTextArea(String name) {
    super(name);
    mytag = "textarea";
  }

  /** @param name The name of the TextArea within the form
   * @param s The string in the text area */
  public MyTextArea(String name, String s) {
    super(name, s);
  }

  // Copy of Block.write(Writer), changed to onit the newline in the
  // closing tag.  Block.write(Writer) calls super.write(Writer), but we
  // can't do that here (because that would call Block.write(Writer)), so
  // the body of Block.write(Writer) is also copied here.
  public void write(Writer out) throws IOException {
    out.write('<'+mytag+attributes()+'>');
    // this loop is Composite.write(Writer)
    for (int i=0; i <elements.size() ; i++) {
      Object element = elements.get(i);
          
      if (element instanceof Element)
	((Element)element).write(out);
      else if (element==null)
	out.write("null");
      else 
	out.write(element.toString());
    }
    out.write("</"+mytag+">");
  }

}

