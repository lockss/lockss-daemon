package org.lockss.exporter.kbart;

import org.lockss.servlet.ListHoldings;

import java.io.Serializable;

/**
 * A simple class for encapsulating customisation options for KBART reports.
 *
 * @author Neil Mayo
 */
public class KbartCustomOptions implements Serializable {

  private boolean omitEmptyColumns;
  private boolean showHealthRatings;
  private KbartExportFilter.FieldOrdering fieldOrdering;

  public KbartCustomOptions(boolean omit, boolean health,
                            KbartExportFilter.FieldOrdering ord) {
    this.omitEmptyColumns = omit;
    this.showHealthRatings = health;
    this.fieldOrdering = ord;
  }

  public static KbartCustomOptions getDefaultOptions() {
    return new KbartCustomOptions(
        KbartExporter.omitEmptyFieldsByDefault,
        KbartExporter.showHealthRatingsByDefault,
        KbartExportFilter.CustomFieldOrdering.getDefaultOrdering()
    );
  }

  public boolean isOmitEmptyColumns() {
    return omitEmptyColumns;
  }

  public void setOmitEmptyColumns(boolean omitEmptyColumns) {
    this.omitEmptyColumns = omitEmptyColumns;
  }

  public boolean isShowHealthRatings() {
    return showHealthRatings;
  }

  public void setShowHealthRatings(boolean showHealthRatings) {
    this.showHealthRatings = showHealthRatings;
  }

  public KbartExportFilter.FieldOrdering getFieldOrdering() {
    return fieldOrdering;
  }

  public void setFieldOrdering(KbartExportFilter.FieldOrdering fieldOrdering) {
    this.fieldOrdering = fieldOrdering;
  }

}