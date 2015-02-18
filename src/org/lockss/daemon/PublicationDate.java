/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.daemon;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.lockss.util.StringUtil;

/**
 * This class implements a journal or book publication date. The date
 * consists of a year, and optionally either a season or quarter, or
 * a month and optionally a day.  The elements can either be in narrative
 * form (e.g. 15 January, 2010), or structured (15/Jan/2010).
 * <p>
 * Seasons and quarters are not handled by standard date formats, so
 * special handling is provided for English, French, German, Italian,
 * Portuguese, Russian, and Spanish seasons and quarters.
 * <p>
 * Parsing is permissive, and fail-soft. It attempts to extract as much 
 * information as it recognizes, and ignores what it can not..
 *
 * @author  Philip Gust
 * @version 1.0
 */
public class PublicationDate {
  /** The season (1-4) */
  private int season = 0;
  /** The quarter (1-4) */
  private int quarter = 0;
  /** The month (1-12) */
  private int month = 0;
  /** The day of month (1-31) */
  private int day = 0;
  /** the year (1000 - current year+1) */
  private int year = 0;
  
  /** english seasons */
  static private final Map<String,Integer> en_seasons = new LinkedHashMap<String,Integer>();
  {
    en_seasons.put("spring", 1);
    en_seasons.put("1S", 1);
    en_seasons.put("S1", 1);
    en_seasons.put("summer", 2);
    en_seasons.put("2S", 2);
    en_seasons.put("S2", 2);
    en_seasons.put("fall", 3);
    en_seasons.put("3S", 3);
    en_seasons.put("S3", 3);
    en_seasons.put("winter", 4);
    en_seasons.put("4S", 4);
    en_seasons.put("S4", 4);
  }

  /** french seasons */
  static private final  Map<String,Integer> fr_seasons = new LinkedHashMap<String,Integer>();
  {
    fr_seasons.put("printemps", 1);             // spring
    fr_seasons.put("d'été", 2);                 // summer season
    fr_seasons.put("été", 2);                   // summer
    fr_seasons.put("d'automne", 3);             // autumn quarter
    fr_seasons.put("l'automne", 3);             // autumn 
    fr_seasons.put("automne", 3);               // autumn season
    fr_seasons.put("d'hiver", 4);               // winter season
    fr_seasons.put("hiver", 4);                 // winter
  }

  /** german seasons */
  static private final  Map<String,Integer> de_seasons = new LinkedHashMap<String,Integer>();
  {
    de_seasons.put("Frühling", 1);              // spring season 
    de_seasons.put("Frühjahr", 1);              // spring
    de_seasons.put("Sommersaison", 2);          // summer season
    de_seasons.put("Sommer", 2);                // summer
    de_seasons.put("Herbst-Quartal", 3);        // fall quarter
    de_seasons.put("Herbst-Saison", 3);         // fall season
    de_seasons.put("Herbst", 3);                // fall
    de_seasons.put("Winterquartier", 4);        // winter quarter
    de_seasons.put("Wintersaison", 4);          // winter season
    de_seasons.put("Winter", 4);                // winter
  }
  
  /** italian seasons */
  static private final  Map<String,Integer> it_seasons = new LinkedHashMap<String,Integer>();
  {
    it_seasons.put("primavera", 1);             // spring
    it_seasons.put("estiva", 2);                // summer season
    it_seasons.put("estate", 2);                // summer
    it_seasons.put("autunno", 3);               // fall
    it_seasons.put("invernale", 4);             // winter quarter
    it_seasons.put("inverno", 4);               // winter
  }
  
  /** portuguese seasons */
  static private final  Map<String,Integer> pt_seasons = new LinkedHashMap<String,Integer>();
  {
    pt_seasons.put("primavera", 1);             // spring
    pt_seasons.put("verão", 2);                 // summer
    pt_seasons.put("outono", 3);                // fall
    pt_seasons.put("inverno", 4);               // winter
  }
  
  /** russian */
  static private final  Map<String,Integer> ru_seasons = new LinkedHashMap<String,Integer>();
  {
    ru_seasons.put("весенний", 1);              // spring season
    ru_seasons.put("весной", 1);                // spring
    ru_seasons.put("Летний", 2);                // summer season
    ru_seasons.put("Летом", 2);                 // summer
    ru_seasons.put("осень", 3);                 // fall season
    ru_seasons.put("осенью", 3);                // fall
    ru_seasons.put("зимой", 4);                 // winter quarter
    ru_seasons.put("зимний", 4);                // winter season
    ru_seasons.put("зима", 4);                  // winter
  }
  
  /** spanish */
  static private final Map<String,Integer> es_seasons = new LinkedHashMap<String,Integer>();
  {
    es_seasons.put("primavera", 1);             // spring
    es_seasons.put("verano", 2);                // summer
    es_seasons.put("otoño", 3);                 // fall
    es_seasons.put("invierno", 4);              // winter
  }
  
  /** seasons */
  static private final  Map<String,Map<String,Integer>> seasons = new LinkedHashMap<String,Map<String,Integer>>();
  {
    seasons.put("de", de_seasons);
    seasons.put("en", en_seasons);
    seasons.put("es", es_seasons);
    seasons.put("fr", fr_seasons);
    seasons.put("it", it_seasons);
    seasons.put("pt", pt_seasons);
    seasons.put("ru", ru_seasons);
  }
  
  /** english quarters */
  static private final  Map<String,Integer> en_quarters = new LinkedHashMap<String,Integer>();
  {
    en_quarters.put("first quarter", 1);
    en_quarters.put("1st quarter", 1);
    en_quarters.put("quarter 1", 1);
    en_quarters.put("quarter one", 1);
    en_quarters.put("1Q", 1);
    en_quarters.put("Q1", 1);
    en_quarters.put("second quarter", 1);
    en_quarters.put("2nd quarter", 1);
    en_quarters.put("quarter 2", 1);
    en_quarters.put("quarter Two", 1);
    en_quarters.put("2Q", 2);
    en_quarters.put("Q2", 2);
    en_quarters.put("third quarter", 1);
    en_quarters.put("3rd quarter", 1);
    en_quarters.put("quarter 3", 1);
    en_quarters.put("quarter three", 1);
    en_quarters.put("3Q", 3);
    en_quarters.put("Q3", 3);
    en_quarters.put("fourth quarter", 1);
    en_quarters.put("4th quarter", 1);
    en_quarters.put("quarter 4", 1);
    en_quarters.put("quarter four", 1);
    en_quarters.put("4Q", 4);
    en_quarters.put("Q4", 4);
  }
  
  /** french quarters */
  static private final Map<String,Integer> fr_quarters = new LinkedHashMap<String,Integer>();
  {
    fr_quarters.put("premier trimestre",1);     // first quarter
    fr_quarters.put("1er trimestre",1);         // 1st quarter
    fr_quarters.put("trimestre 1",1);           // quarter 1
    fr_quarters.put("quart", 1);                // quarter one

    fr_quarters.put("deuxième trimestre",2);    // second quarter
    fr_quarters.put("2e trimestre",2);          // 2nd quarter
    fr_quarters.put("2 er trimestre",2);        // quarter 2
    fr_quarters.put("quart deux",2);            // quarter two

    fr_quarters.put("au troisième trimestre",3);// third quarter
    fr_quarters.put("3ème trimestre",3);        // 3rd quarter
    fr_quarters.put("3 e trimestre",3);         // quarter 3
    fr_quarters.put("trois quart",3);           // quarter three

    fr_quarters.put("quatrième trimestre",4);   // fourth quarter
    fr_quarters.put("4e trimestre",4);          // 4th quarter
    fr_quarters.put("4 e trimestre",4);         // quarter 4
    fr_quarters.put("quatre trimestres",4);     // quarter four
  }
  
  /** german quarters */
  static private final Map<String,Integer> de_quarters = new LinkedHashMap<String,Integer>();
  {
    de_quarters.put("ersten Quartal",1);        // first quarter
    de_quarters.put("1. Quartal",1);            // 1st quarter
    de_quarters.put("Quartal 1",1);             // quarter 1
    de_quarters.put("Quartal ein",1);          // quarter one

    de_quarters.put("zweiten Quartal",2);       // second quarter
    de_quarters.put("2. Quartal",2);            // 2nd quarter
    de_quarters.put("Quartal 2 ",2);            // quarter 2
    de_quarters.put("Quartal zwei",2);          // quarter two

    de_quarters.put("dritten Quartal",3);       // third quarter
    de_quarters.put("3. Quartal",3);            // 3rd quarter
    de_quarters.put("Quartal 3",3);             // quarter 3
    de_quarters.put("Quartal drei",3);          // quarter three

    de_quarters.put("vierten Quartal",4);       // fourth quarter
    de_quarters.put("4. Quartal",4);            // 4th quarter
    de_quarters.put("Quartal 4",4);             // quarter 4
    de_quarters.put("vierte Quartal",4);        // quarter four
  }
  
  /** italian quarters */
  static private final Map<String,Integer> it_quarters = new LinkedHashMap<String,Integer>();
  {
    it_quarters.put("primer trimestre",1);      // first quarter
    it_quarters.put("1 º trimestre",1);         // 1st quarter
    it_quarters.put("trimestre 1",1);           // quarter 1
    it_quarters.put("quarto", 1);               // quarter one

    it_quarters.put("secondo trimestre",2);     // second quarter
    it_quarters.put("2 º trimestre",2);         // 2nd quarter
    it_quarters.put("quartiere 2",2);           // quarter 2
    it_quarters.put("trimestre due",2);         // quarter two

    it_quarters.put("terzo trimestre",3);       // third quarter
    it_quarters.put("3 º trimestre",3);         // 3rd quarter
    it_quarters.put("tre quarti",3);            // quarter three

    it_quarters.put("nel quarto trimestre",4);  // fourth quarter
    it_quarters.put("4 º trimestre",4);         // 4th quarter
    it_quarters.put("quartiere 4",4);           // quarter 4
    it_quarters.put("trimestre quattro",4);     // quarter four
  }
  
  /** portuguese quarters */
  static private final Map<String,Integer> pt_quarters = new LinkedHashMap<String,Integer>();
  {
    pt_quarters.put("primeiro trimestre",1);    // first quarter
    pt_quarters.put("1 º trimestre",1);         // 1st quarter
    pt_quarters.put("trimestre 1",1);           // quarter 1
    pt_quarters.put("quarto", 1);               // quarter one

    pt_quarters.put("segundo trimestre",2);     // second quarter
    pt_quarters.put("2 º trimestre",2);         // 2nd quarter
    pt_quarters.put("2 º trimestre",2);         // quarter 2
    pt_quarters.put("trimestre de dois",2);     // quarter two

    pt_quarters.put("terceiro trimestre",3);    // third quarter
    pt_quarters.put("3 º trimestre",3);         // 3rd quarter
    pt_quarters.put("quarto 3",3);              // quarter 3
    pt_quarters.put("três quartos",3);          // quarter three

    pt_quarters.put("quarto trimestre",4);      // fourth quarter
    pt_quarters.put("4 º trimestre",4);         // 4th quarter
    pt_quarters.put("4 trimestre",4);           // quarter 4
    pt_quarters.put("trimestre quatro",4);      // quarter four
  }
  
  /** russian quarters */
  static private final Map<String,Integer> ru_quarters = new LinkedHashMap<String,Integer>();
  {
    ru_quarters.put("первом квартале",1);       // first quarter
    ru_quarters.put("1-й квартал",1);           // 1st quarter
    ru_quarters.put("1 квартал",1);             // quarter 1
    ru_quarters.put("один квартал",1);          // quarter one

    ru_quarters.put("во втором квартале",2);    // second quarter
    ru_quarters.put("2-й квартал",2);           // 2nd quarter
    ru_quarters.put("2 квартале",2);            // quarter 2
    ru_quarters.put("два квартала",2);          // quarter two

    ru_quarters.put("третьем квартале",3);      // third quarter
    ru_quarters.put("3-й квартал",3);           // 3rd quarter
    ru_quarters.put("квартал 3",3);             // quarter 3
    ru_quarters.put("три четверти",3);          // quarter three

    ru_quarters.put("четвертом квартале",4);    // fourth quarter
    ru_quarters.put("4-й квартал",4);           // 4th quarter
    ru_quarters.put("4 квартале",4);            // quarter 4
    ru_quarters.put("четвертом квартале",4);    // quarter four
  }
  
  /** spanish quarters */
  static private final Map<String,Integer> es_quarters = new LinkedHashMap<String,Integer>();
  {
    es_quarters.put("primer trimestre",1);      // first quarter
    es_quarters.put("1 º trimestre",1);         // 1st quarter
    es_quarters.put("trimestre 1",1);           // quarter 1
    es_quarters.put("cuarto", 1);               // quarter one

    es_quarters.put("segundo trimestre",2);     // second quarter
    es_quarters.put("2 º trimestre",2);         // 2nd quarter
    es_quarters.put("trimestre 2",2);           // quarter 2
    es_quarters.put("trimestre dos",2);         // quarter two

    es_quarters.put("tercer trimestre",3);      // third quarter
    es_quarters.put("3 º trimestre",3);         // 3rd quarter
    es_quarters.put("trimestre 3",3);           // quarter 3
    es_quarters.put("de tres cuartos",3);       // quarter three

    es_quarters.put("cuarto trimestre",4);      // fourth quarter
    es_quarters.put("4 º trimestre",4);         // 4th quarter
    es_quarters.put("trimestre 4",4);           // quarter 4
    es_quarters.put("cuatro trimestres",4);     // quarter four
  }

  /** quarters */
  static private final Map<String,Map<String,Integer>> quarters = new LinkedHashMap<String,Map<String,Integer>>();
  {
    quarters.put("de", de_quarters);
    quarters.put("en", en_quarters);
    quarters.put("es", es_quarters);
    quarters.put("fr", fr_quarters);
    quarters.put("it", it_quarters);
    quarters.put("pt", pt_quarters);
    quarters.put("ru", ru_quarters);
  }
  
  /**
   * Construct a publication date by parsing a string formatted in
   * the default locale.
   * 
   * @param dateStr the date string
   * @throws ParseException if the date cannot be parsed
   */
  public PublicationDate(String dateStr) throws ParseException {
    this(dateStr, Locale.getDefault());
  }
  
  /**
   * Construct a publication date by parsing a string formatted in
   * the specified locale.
   * 
   * @param s the date string
   * @param locale the locale for interpreting the date string
   * @throws ParseException if the date cannot be parsed
   */
  public PublicationDate(String s, Locale locale) throws ParseException {
    
    // normalize by stripping accents and lower-casing
    String dateStr = StringUtil.toUnaccented(s).toLowerCase(locale);

    // get season
    Map<String,Integer> myseasons = seasons.get(locale.getLanguage());
    if (myseasons != null) {
      // match season phrases as whole words.
      for (Map.Entry<String,Integer> entry : myseasons.entrySet()) {
        String key = StringUtil.toUnaccented(entry.getKey()).toLowerCase(locale);
        String match = key + " ";
        if (!dateStr.startsWith(match)) {
          match = key + ",";
          if (!dateStr.startsWith(match)) {
            match = " " + key;
            if (!dateStr.endsWith(match)) {
              match = " " + key + " ";
              if (!dateStr.contains(match)) {
                match = " " + key + ",";
                if (!dateStr.contains(match)) {
                  continue;
                }
              }
            }
          }
        }
        dateStr = dateStr.replace(match," ");
        season = entry.getValue();
        break;
      }
    }
    
    // get quarter
    Map<String,Integer> myquarters = quarters.get(locale.getLanguage());
    if (myquarters != null) {
      // match quarter phrases as whole words.
      for (Map.Entry<String,Integer> entry : myquarters.entrySet()) {
        String key = StringUtil.toUnaccented(entry.getKey()).toLowerCase(locale);
        String match = key + " ";
        if (!dateStr.startsWith(match)) {
          match = key + ",";
          if (!dateStr.startsWith(match)) {
            match = " " + key;
            if (!dateStr.endsWith(match)) {
              match = " " + key + " ";
              if (!dateStr.contains(match)) {
                match = " " + key + ",";
                if (!dateStr.contains(match)) {
                  continue;
                }
              }
            }
          }
        }
        dateStr = dateStr.replace(match," ");
        quarter = entry.getValue();
        break;
      }
    }
    
    Calendar c = Calendar.getInstance(locale);

    // earliest and latest recognizable years
    // first year; limits to 4-digit years
    int firstYear = 1000;
    // year after current year (some pubs come out in advance)
    int lastYear = c.get(Calendar.YEAR)+1;      

    // get month names
    Map<String, Integer> months = 
        c.getDisplayNames(Calendar.MONTH, Calendar.ALL_STYLES, locale);
    
    // process words from date string after removing quarter or season
    for (StringTokenizer tok = new StringTokenizer(dateStr); 
         tok.hasMoreElements(); ) {
      // remove extraneous trailing punctuation
      String word= tok.nextToken();

      if (month == 0) {
        // get month after eliminating any trailing punctuation.
        String w = word.replaceAll("\\p{Punct}+$", "");
        for (Map.Entry<String,Integer> entry : months.entrySet()) {
          // strip punctuation because keys for a few locales 
          // include trailing punctuation with their abbreviations
          if (StringUtil.toUnaccented(
              entry.getKey()).replaceAll(
                  "\\p{Punct}+$", "").equalsIgnoreCase(w)) {
            month = entry.getValue()+1;
            break;
          }
        }
        if (month != 0) {
          continue;
        }
      }
      
      // bit of a kludge: also covers "28/nov./2010 where
      // './' is treated as a single delimiter; luckily '.' 
      // is also a date delimiter so this works.
      String[] wds = word.split("[./-]+");
      if (wds.length == 1) {
        try {
          // crude trick to strip ordinal indicators from numbers
          // by removing all non-digit characters before parsing
          int num = Integer.parseInt(word.replaceAll("[^\\p{Digit}]", ""));
          
          if (year == 0) {
            if ((num >= firstYear) && (num <= lastYear)) {
              year = num;
              continue;
            }
          }
          
          if (day == 0) {
            if ((num >= 1) && (num <= 31)) {
              day = num;
              continue;
            }
          }
        } catch (NumberFormatException ex) {
          // not a number
        }
      }
      
      int w[] = new int[3];
      int len = Math.min(wds.length, 3);
      int monthIndex = -1;
      // parse initial fields for date elements
      for (int i = 0; i < len; i++) {
        Integer n = null;
        for (Map.Entry<String, Integer> entry : months.entrySet()) {
          if (StringUtil.toUnaccented(
              entry.getKey()).replaceAll(
                  "\\p{Punct}+$", "").equalsIgnoreCase(wds[i])) {
            n = entry.getValue();
            break;
          }
        }
        if (n != null) {
          w[i] = 1+n;
          monthIndex = i;
        } else {
          try {
            // crude trick to strip ordinal indicators from numbers
            // by removing all non-digit characters before parsing
            w[i] = Integer.parseInt(wds[i].replaceAll("[^\\p{Digit}]", ""));
          } catch (NumberFormatException ex) {
          }
        }
      }
      
      if (w[0] > 0) {
        if ((w[0] >= firstYear) && (w[0] <= lastYear)) {
          // parse year and season, year and quarter, 
          // year and month, or year month and day
          year = w[0];
          if (w[1] != 0) {
            if (wds[1].equalsIgnoreCase("S"+w[1])) {
              season = w[1];
            } else if (wds[1].equalsIgnoreCase("Q"+w[1])) {
              quarter = w[1];
            } else if ((w[1] >= 1) && (w[1] <= 12)) {
              month = w[1];
              if ((w[2] >= 1) && (w[2] <= 31)) {
                day = w[2];
              }
            }
          }
        } else if (w[2] == 0) {
          // parse month and year, season and year, or quarter and year
          if ((w[1] >= firstYear) && (w[1] <= lastYear)) {
            year = w[1];
            if (w[0] != 0) {
              if (wds[0].equalsIgnoreCase("S"+w[0])) {
                season = w[0];
              } else if (wds[0].equalsIgnoreCase("Q"+w[0])) {
                quarter = w[0];
              } else if ((w[0] >= 1) && (w[0] <= 12)) {
                month = w[0];
              }
            }
          }
        } else if ((w[2] >= firstYear) && (w[2] <= lastYear)) {
          year = w[2];
          if ((monthIndex == 0) ||
              ("en".equalsIgnoreCase(locale.getLanguage()) && 
               "US".equalsIgnoreCase(locale.getCountry()))) {
            // parse month day year in US only
            if ((w[0] >= 1) && (w[0] <= 12)) {
              month = w[0];
              if ((w[1] >= 1) && (w[1] <= 31)) {
                day = w[1];
              }
            }
          } else {
            // parse day month and year
            if ((w[1] >= 1) && (w[1] < 12)) {
              month = w[1];
              if ((w[0] >= 1) && (w[0] <= 31)) {
                day = w[0];
              }
            }
          }
        }
      }
    }
    
    // There must be at least a year. It would be good to catch
    // other cases as well, but this is what is curently being done. 
    if (year == 0) {
      throw new ParseException(s, 0);
    }
  }
  
  /**
   * Construct a publication date from the specified date
   * @param aDate the date
   */
  public PublicationDate(Date aDate) {
    this(aDate, "YMD");
  }

  /**
   * Construct a publication date from the specified date 
   * and mask. The mask length indicates how many elements
   * of the date to include: one element for year, two
   * elements for year and month, and three elements for
   * year month and day.
   * @param aDate the date
   * @param mask a mask string of 1-3 characters
   */
  public PublicationDate(Date aDate, String mask) {
    if ((mask != null) && mask.length() >= 1) {
      Calendar cal = Calendar.getInstance();
      cal.setTime(aDate);
      year = cal.get(Calendar.YEAR);
      if (mask.length() >= 2) {
        month = cal.get(Calendar.MONTH);
        if (mask.length() >= 3) {
          day = cal.get(Calendar.DAY_OF_MONTH);
        }
      }
    }
  }

  /**
   * Static for parsing a publication date string for the
   * default locale.
   * @param pubDateStr the date string
   * @param locale the locale
   * @throws ParseException if unrecognized date string
   */
  static public PublicationDate parse(String pubDateStr) 
      throws ParseException {
    return new PublicationDate(pubDateStr);
  }

  /**
   * Static for parsing a publication date string for the
   * specified locale. 
   * @param pubDateStr the date string
   * @param locale the locale
   * @throws ParseException if unrecognized date string
   */
  static public PublicationDate parse(String pubDateStr, Locale locale) 
      throws ParseException {
    return new PublicationDate(pubDateStr, locale);
  }
  
  /**
   * Return string representation of the publication date in extended
   * ISO-8601 format: YYYY, YYYY-MM, YYYY-MM-DD, YYYY-QQ, YYYY-SS, where
   * YYYY, MM, and DD are numeric, and QQ is like "Q1" for the first quarter 
   * and SS is like "S1" for the first season.
   * 
   * @return the publication date string in extended ISO-8601 format
   */
  public String toString() {
    String s = Integer.toString(year);
    if (quarter > 0) {
      s += "-Q" + quarter;
    } else if (season > 0) {
      s += "-S" + season;
    } else if (month > 0) {
      s += String.format("-%02d", month);
      if (day > 0) {
        s += String.format("-%02d", day);
      }
    }
    return s;
  }
  
  /**
   * Get the year for this date.
   * @return the year
   */
  public int getYear() {
    return year;
  }
  
  /**
   * Get the 1-based month for this date.
   * @return the month or 0 if not specified 
   */
  public int getMonth() {
    return month;
  }
  
  /**
   * Get the 1-based day of month for this date.
   * @return the day of month or 0 if not specified
   */
  public int getDayOfMonth() {
    return day;
  }
  
  /**
   * Get the 1-based quarter for this date.
   * @return the quarter if not specified
   */
  public int getQuarter() {
    return quarter;
  }
  
  /**
   * Get the 1-based season for this date.
   * @return the season if not specified
   */
  public int getSeason() {
    return season;
  }
}
