package org.lockss.exporter.kbart;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/**
 * Exports records as fields separated by a separator character. By default this is a tab.
 * 
 * @author Neil Mayo
 *
 */
public class SeparatedValuesKbartExporter extends KbartExporter {

  private static Logger log = Logger.getLogger("SeparatedValuesKbartExporter");

  protected static final String SEPARATOR_TAB = "\t";
  protected static final String SEPARATOR_COMMA = ",";

  /** The separator that will be used to separate fields in the output. */
  private final String SEPARATOR;

  /**
   * Default constructor takes a list of KbartTitles to be exported.
   * 
   * @param titles the titles which are to be exported
   */
  public SeparatedValuesKbartExporter(List<KbartTitle> titles, OutputFormat format) {
    this(titles, format, SEPARATOR_TAB);
  }

  /**
   * Constructor which allows the separator to be defined.
   * 
   * @param titles the titles which are to be exported
   */
  public SeparatedValuesKbartExporter(List<KbartTitle> titles, OutputFormat format, String sep) {
    super(titles, format);
    this.SEPARATOR = sep;
  }

  @Override
  protected void setup(OutputStream os) throws IOException {
    // allow the default setup, but write a header line
    super.setup(os); 
    printWriter.println(StringUtil.separatedString(KbartTitle.Field.getLabels(), SEPARATOR));
  }

  @Override
  protected void emitRecord(List<String> values) throws IOException {
    printWriter.println( StringUtil.separatedString(values, SEPARATOR));
    printWriter.flush();
  }

}
