package org.lockss.exporter.kbart;

import java.io.PrintWriter;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.OutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.util.StringUtil;
import org.lockss.util.Logger;


/**
 * An exporter that simply writes the records to an HTML table.
 * 
 * @author Neil Mayo
 */
public class HtmlKbartExporter extends KbartExporter {

  private static Logger log = Logger.getLogger("HtmlKbartExporter");

  /** A switch to keep track of whether a line is odd or even. */
  private boolean odd = false;
  /** A count of how many records were exported. */
  private int exportCount = 0;
  /** The table header will be printed every <code>headerInterval</code> rows. */
  private int headerInterval = 20;
  /** A header row for the table. */
  private String header;
  /** Summary of the export for display. */
  private String summary;

  // TODO Eventually it will be useful to have the option to customise the HTML output in a few ways: 
  // - allow fields to be specified for omission
  // - allow user to specify which fields and in what order
  //private Set<Field> omitFields = EnumSet.of(Field.TITLE_ID, Field.TITLE_URL);
  // base this on which field columns are entirely empty
  
  
  /**
   * Default constructor takes a list of KbartTitles to be exported.
   * 
   * @param titles the titles which are to be exported
   */
  public HtmlKbartExporter(List<KbartTitle> titles, OutputFormat format) {
    super(titles, format);
  }  

  @Override
  protected void emitRecord(KbartTitle title) {
    exportCount++;
    odd = !odd;
    // Print a header every few rows
    if (exportCount % headerInterval == 0) {
      printWriter.printf(this.header); 
    }
    //printWriter.println( "<tr><td>" + StringUtil.separatedString(title.fieldValues(), SEPARATOR) + "</td></tr>" );
    printWriter.println("<tr class=\"" + (odd?"odd":"even") + "\">");
    for (Field fld : Field.values()) {
      String s = title.getField(fld);
      if (s==null || s.equals("")) s = "&nbsp;";
      printWriter.printf("<td>%s</td>", s);
    }
    printWriter.println("</tr>");
    // Flush this record
    printWriter.flush();
  }

  @Override
  protected void setup(OutputStream os) throws IOException {
    printWriter = new PrintWriter(os, true);
    // Write html and head tags, including a metatag declaring the content type UTF-8
    printWriter.println("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>");
    this.summary = String.format("Export created on %s by %s | Exported %d KBART titles from %d TDB titles.", 
	new Date(), getHostName(), titles.size(), tdbTitleTotal);
    printWriter.printf("<title>%s</title>", this.summary);
    printWriter.printf("%s</head><table>", css);
    this.header = String.format("<tr><th>%s</th></tr>", StringUtil.separatedString(Field.getLabels(), "</th><th>"));
    printWriter.printf(this.header);
  }

  @Override
  protected void clearup() throws IOException {
    printWriter.println("</table>");
    printWriter.printf("<br/><b><i>%s</i></b><br/>", this.summary);
    printWriter.println("</html>");
    printWriter.flush();
    printWriter.close();
  }

  private static final String css;
  static {
    StringBuilder sb = new StringBuilder();
    sb.append("<style type=\"text/css\">");
    sb.append("table, th, tr, td { border: 1px solid black; }");
    sb.append("th { font-size: x-small; }");
    sb.append("tr.odd { background-color: #cce; }");
    sb.append("tr.even { background-color: #aac; }");
    sb.append("</style>");
    css = sb.toString();
  };
  
}
