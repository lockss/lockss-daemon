package org.lockss.util;

/**
 * <p>Title: TimedIteratorExpiredException</p>
 * <p>Description: Thrown by an iterator to a <code>TimedMap</code> class if
 * an entry in the timed map expires while the iterator is in use.  This is
 * in accordance with the general contract for iterators, whose behavior
 * is undefined if the underlying collection changes.</p>
 * @author Tyrone Nicholas
 * @version 1.0
 */

public class TimedIteratorExpiredException extends RuntimeException {
}