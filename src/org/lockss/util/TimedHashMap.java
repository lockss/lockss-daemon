package org.lockss.util;

import java.util.*;
import org.apache.commons.collections.*;

/**
 * <p>Title: TimedHashMap</p>
 * <p>Description: </p>
 * @author Tyrone Nicholas
 * @version 1.0
 */

public class TimedHashMap implements Map
{

	/** Amount of time each entry has before it will be deleted */
	int interval;

	/** Stores times of all the keys */
	Map keytimes;

    /** Delegates actual entries */
    SequencedHashMap entries;

	public TimedHashMap(int interval)
	{
		this.interval = interval;
		this.keytimes = new HashMap();
        this.entries = new SequencedHashMap();
	}

	protected void updateEntries()
	{
		while (!entries.isEmpty())
		{
			Object o = entries.getFirstKey();
            Deadline entry = (Deadline)keytimes.get(o);
			if (entry.expired())
			{
				keytimes.remove(o);
				entries.remove(o);
			}
			else
				return;
		}
	}

  public void clear()
  {
      entries.clear();
      keytimes.clear();
  }

	public boolean containsKey(Object key)
	{
		updateEntries();
		return entries.containsKey(key);
	}

	public boolean containsValue(Object value)
	{
		updateEntries();
		return entries.containsKey(value);
	}

	public Set entrySet()
	{
		updateEntries();
		return entries.entrySet();
	}

	public boolean equals(Object obj)
    {

		try {
			updateEntries();
			TimedHashMap other = (TimedHashMap) obj;
			other.updateEntries();
			return (interval == other.interval &&
					entries.equals(other.entries) &&
					keytimes.equals(other.keytimes));
		}
		catch (ClassCastException e)
        {
			return false;
		}
	}

	public Object get(Object key)
    {
		updateEntries();
		return entries.get(key);
	}

	public int hashCode()
    {
      int a = entries.hashCode();
      int b = interval;
      int c = keytimes.hashCode();
		return entries.hashCode() + interval + keytimes.hashCode();
	}

	public boolean isEmpty()
    {
		updateEntries();
		return entries.isEmpty();
	}

	public Set keySet()
    {
		updateEntries();
		return entries.keySet();
	}

	public Object put(Object key, Object value)
    {
		updateEntries();
		Deadline deadline = Deadline.in(interval);
		keytimes.put(key,deadline);
		return entries.put(key, value);
	}

	public void putAll(Map t)
    {
		updateEntries();
		Iterator it = t.entrySet().iterator();
		while (it.hasNext())
        {
            Map.Entry entry = (Map.Entry)it.next();
            put(entry.getKey(),entry.getValue());
        }
	}

	public Object remove(Object key)
    {
		updateEntries();
		keytimes.remove(key);
		return entries.remove(key);
	}

	public int size()
    {
		updateEntries();
		return entries.size();
	}

	public Collection values()
    {
		updateEntries();
		return entries.values();
	}

}