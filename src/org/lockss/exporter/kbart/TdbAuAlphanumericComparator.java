package org.lockss.exporter.kbart;

import org.lockss.config.TdbAu;

/**
 * Sort a set of AUs alphanumerically, based on their names. String comparison is performed case-insensitively.  
 * <p>
 * Where multiple title names appear within a single journal title, they should end up appropriately 
 * grouped so they can be split into different title ranges.
 *	
 * @author neil
 */
public class TdbAuAlphanumericComparator extends AlphanumericComparator<TdbAu> {

  /**
   * Create a comparator with the default case-sensitivity of KbartTitle fields.
   */
  public TdbAuAlphanumericComparator() {
    super(KbartTitle.Field.CASE_SENSITIVITY_DEFAULT);
  }
  
  @Override
  protected String getComparisonString(TdbAu tdbAu) {
    return tdbAu.getName();
  }
  
}

