package org.lockss.exporter.kbart;

import java.util.Comparator;
import java.util.Vector;

import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.oro.text.regex.Util;
import org.lockss.config.TdbAu;
import org.lockss.util.Logger;
import org.lockss.util.RegexpUtil;
import org.lockss.util.StringUtil;

/**
 * Sort a set of <code>TdbAu</code> objects into a pseudo-chronological order based on names.
 * There is no guaranteed way to order chronologically due to the incompleteness of date 
 * metadata included in AUs at the time of writing; however the naming convention of AUs
 * provides a best-guess method.
 * <p>
 * First an attempt is made to order by any available date information. By convention of TDB AU 
 * naming, ascending alphanumerical order should also be chronological order, so this is tried second.
 * Note that in general the AU names are identical to the title name plus a volume or year identifier.
 * However if a title's title/name changes during its run, multiple titles can appear 
 * in the AU records. This is probably wrong as the new title should get a different ISSN and
 * have a separate title record.
 * <p>
 * The alphanumeric ordering tokenizes each string into number and non-number sections, and then
 * performs a sequential pair-wise comparison on each token; alphabetically for text, and by magnitude  
 * for numbers, until a difference is found. As a last resort, natural string ordering is used. 
 * <p>
 * As a side effect, where multiple title names appear within a single journal title,
 * they should end up appropriately grouped so they can be split into different title ranges.
 * <p>
 * Perhaps this comparator should throw an exception if the conventions appear to be 
 * contravened, rather than trying to order with arbitrary names. 
 *	
 * @author neil
 */
public class TdbAuAlphanumericComparator implements Comparator<TdbAu>  {

  private static Logger log = Logger.getLogger("TdbAuAlphanumericComparator");

  private static final String numberRegex = "\\d+";
  private static final String nonNumberRegex = "[^\\d]+";
  private static final Pattern number = RegexpUtil.uncheckedCompile(numberRegex);
  private static final Pattern nonNumber = RegexpUtil.uncheckedCompile(nonNumberRegex);

  private static final Perl5Matcher matcher = RegexpUtil.getMatcher();
   
  public int compare(TdbAu au1, TdbAu au2) {
    // First try year comparison
    String yr1 = KbartTdbAuUtil.getFirstYear(KbartTdbAuUtil.findYear(au1));
    String yr2 = KbartTdbAuUtil.getFirstYear(KbartTdbAuUtil.findYear(au2));
    try {
      if (!"".equals(yr1) && !"".equals(yr2)) {
	return KbartTdbAuUtil.compareStringYears(yr1, yr2);
      }
    } catch (NumberFormatException e) {
      log.warning("Could not compare years ("+yr1+", "+yr2+")");
      // fall through to an alphanumeric comparison
    }
    return compareAlphanumerically(au1, au2);
  }
  
  /**
   * Compare the names of the AUs by tokenising them into number and non-number sections,
   * and comparing as strings or numbers as appropriate. If something goes wrong, the names
   * are compared alphabetically.
   * 
   * @param au1 first AU
   * @param au2 second AU
   * @return the usual compareTo values
   */
  private int compareAlphanumerically(TdbAu au1, TdbAu au2) {
    String au1name = au1.getName();
    String au2name = au2.getName();

    if (StringUtil.isNullString(au1name) || StringUtil.isNullString(au2name)) {
      log.warning("One or more empty AU names: '"+au1name+"' and '"+au2name+"'");
      // Use natural ordering      
      return au1name.compareTo(au2name);
    }
    
    // Split each string into tokens
    Vector<String> au1TextTokens = new Vector<String>();
    Vector<String> au1NumTokens = new Vector<String>();
    Vector<String> au2TextTokens = new Vector<String>();
    Vector<String> au2NumTokens = new Vector<String>();
    Util.split(au1TextTokens, matcher, number, au1name);
    Util.split(au1NumTokens, matcher, nonNumber, au1name);
    Util.split(au2TextTokens, matcher, number, au2name);
    Util.split(au2NumTokens, matcher, nonNumber, au2name);
    // List sizes    
    int au1tl = au1TextTokens.size();
    int au1nl = au1NumTokens.size();
    int au2tl = au2TextTokens.size();
    int au2nl = au2NumTokens.size();
    
    // Check for empty arrays
    if (au1tl<=0 || au2tl<=0 || au1nl<=0 || au2nl<=0) {
      log.warning("Could not tokenise AU names '"+au1name+"' and '"+au2name+"'");
      // Use natural ordering      
      return au1name.compareTo(au2name);
    }
    
    // Establish which of the tokens arrays comes first in the string
    boolean numsFirst = false;
    if (au1tl > au1nl) {
      // interleave the tokens starting with text
      numsFirst = false;      
    } else if (au1tl < au1nl) {
      // interleave the tokens starting with number
      numsFirst = true;      
    } else {
      // interleave the tokens based on inspection of the first char
      numsFirst = au1name.startsWith(au1NumTokens.get(0));
    }
    
    // Check number of tokens for each string is the same, to simplify iteration.
    // A different number of tokens in each name could indicate that
    // the formats are different; or it could be the difference
    // between "1" and "1a" in a volume.
    if (au1tl < au2tl) au1TextTokens.add(""); 
    if (au1tl > au2tl) au2TextTokens.add(""); 
    if (au1nl < au2nl) au1NumTokens.add(""); 
    if (au1nl > au2nl) au2NumTokens.add(""); 
        
    // For each pair of number or text tokens, do a pairwise comparison, casting to number as appropriate
    try {
      // Total number of pairs
      int maxPairs = au1NumTokens.size() + au1TextTokens.size();
      // Whether to compare numerically
      boolean isNumerical = numsFirst;
      // Get a pair of text or num tokens for each string and compare them
      for (int i=0; i<maxPairs; i++) {
	// Is this a number comparison? (even index and numsFirst, or odd index and !numsFirst)
	//boolean isNumerical = i%2==0 ? numsFirst : !numsFirst;
	
	// Retrieve tokens for each string
	if (isNumerical) {
	  String s1 = au1NumTokens.get(i/2);
	  String s2 = au2NumTokens.get(i/2);
	  int r;
	  try {
	    // Parse number tokens as integers
	    Integer i1 = new Integer(s1);
	    Integer i2 = new Integer(s2);
	    r = i1.compareTo(i2);
	  } catch (NumberFormatException e) {
	    // Just compare as text
	    r = s1.compareTo(s2);	    
	  }
	  if (r!=0) return r;
	} else {
	  // Parse text tokens as text
	  String s1 = au1TextTokens.get(i/2);
	  String s2 = au2TextTokens.get(i/2);
	  int r = s1.compareTo(s2);
	  if (r!=0) return r;
	}
	// Toggle numerical processing
	isNumerical = !isNumerical;
      }
    } catch (Exception e) {
      // There was some problem parsing the strings; just return a natural ordering.
      log.warning("Could not compare strings ("+au1name+", "+au2name+").", e);
    }
    // Alphabetical (natural string) ordering by default
    return au1name.compareTo(au2name);
  }

}

  
  


