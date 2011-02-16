package org.lockss.exporter.kbart;

import org.lockss.exporter.kbart.KbartTitle.Field;

/**
 * Sort a set of <code>KbartTitle</code>s alphanumerically, based on their title fields.
 * The string comparison is case-insensitive.   
 *	
 * @author Neil Mayo
 */
public class KbartTitleAlphanumericComparator extends AlphanumericComparator<KbartTitle> {

  /**
   * Create a comparator with the default case-sensitivity of KbartTitle fields.
   */
  public KbartTitleAlphanumericComparator() {
    super(KbartTitle.Field.CASE_SENSITIVITY_DEFAULT);
  }

  @Override
  protected String getComparisonString(KbartTitle title) {
    return title.getField(Field.PUBLICATION_TITLE);
  }

}
