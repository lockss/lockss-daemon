/*
 * $Id$
 */

/*

Copyright (c) 2010-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.exporter.kbart;

import java.util.HashMap;

import static org.lockss.util.CachingComparator.NormalisationOption.*;
import org.lockss.exporter.kbart.KbartTitle.Field;
import org.lockss.util.CachingComparator;

/**
 * Comparator that implements a suitable ordering for alphanumeric fields in 
 * KBART output, using the caching behaviour of the {@link CachingComparator}.
 * This class uses case-sensitive matching and character de-accenting;
 * it is not yet clear what restrictions KBART imposes on its "alphabetical 
 * ordering". KBART does explicitly say (5.3.2.2) to retain leading articles
 * so this is reflected in the default constructor.
 * <p>
 * To allow differing normalisation on different fields, a singleton is 
 * available per field. These are initialised lazily, when needed.
 * In fact it is only necessary to maintain a singleton per normalisation 
 * policy, and provide the appropriate one which matches the field's policy,
 * but this is not yet implemented here.
 */
public class KbartFieldOrderComparator extends CachingComparator<KbartTitle> {

  /** KBART recommendation 5.3.2.2 says no. */
  static final boolean KBART_REMOVE_INITIAL_DETERMINERS = false;

  /** The field on which to sort. */
  final Field sortField;
  
  /**
   * A map of KBART fields to instances of this class which will cache their 
   * comparison strings.
   */
  private static final HashMap<Field, KbartFieldOrderComparator> fieldSingletons = 
    new HashMap<Field, KbartFieldOrderComparator>();
  
  /**
   * Create a CachingComparator with normalisation appropriate to KBART 
   * fields.
   */
  public KbartFieldOrderComparator(Field field) {
    super(new HashMap<NormalisationOption, Boolean>() {{
      put(CASE_SENSITIVE, Field.CASE_SENSITIVITY_DEFAULT);
      put(TRANSLATE_ACCENTS, Field.UNACCENTED_COMPARISON_DEFAULT);
      put(REMOVE_INITIAL_DETERMINERS, KBART_REMOVE_INITIAL_DETERMINERS);
    }});
    this.sortField = field;
  }
   
  /**
   * Get a singleton instance of a KbartOrderComparator. It is created if not 
   * already available. Synchronizes on the map to ensure only one 
   * instance of the class gets created per field.
   * <p>
   * Note that maintaining singletons here means that there will be several
   * maps of String to String which are retained between sortings of KBART 
   * objects, in other words between exports. This is not considered a problem 
   * here as they record field strings for KBART titles, the number of which 
   * is on the order of several thousand. If the number of titles gets much 
   * larger, this approach might need rethinking. 
   * 
   * @param field the field whose caching comparator to return
   * @return a caching comparator for the field
   */
  static KbartFieldOrderComparator getSingleton(Field field) {
    synchronized(fieldSingletons) {
      if (!fieldSingletons.containsKey(field)) 
	fieldSingletons.put(field, new KbartFieldOrderComparator(field));
    }
    return fieldSingletons.get(field);
  }
  
  
  /** 
   * Get from a title the string which informs the comparison.
   * @param title the title under comparison
   * @return the string value which informs the comparison
   */
  protected String getComparisonString(KbartTitle title) {
    return title.getField(sortField);
  }

  @Override
  public String toString() {
    return sortField.getLabel() + " (alphanumeric)";
  }
 
}
