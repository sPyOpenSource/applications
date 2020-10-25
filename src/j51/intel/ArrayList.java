/*
 * $Id: ArrayList.java 70 2010-07-01 09:57:00Z mviara $
 *
 */
package j51.intel;

/**
 * Fast implementation without any syncronization and check of
 * of java.util.Arraylist. MCS51 make intensive usage of this class
 * and the performance are critical.
 *
 * @author Mario Viara
 * @version 1.00
 * 
 * @deprecated
 */
public class ArrayList
{
	static final int GROW_SIZE = 10;
	Object objects[];
	int currentSize;

	public ArrayList()
	{
		objects = new Object[0];
		currentSize = 0;
	}

	public int size()
	{
		return currentSize;
	}

	private void checkSize()
	{
		if (currentSize >= objects.length)
		{
			Object newObjects[] = new Object[objects.length+GROW_SIZE];
			System.arraycopy(objects,0,newObjects,0,objects.length);
			objects = newObjects;
		}

	}

	public void clear()
	{
		currentSize = 0;
	}

	public void add(Object o)
	{
		checkSize();
		objects[currentSize++] = o;
	}

	void add(int i,Object o)
	{
		checkSize();
		System.arraycopy(objects,i,objects,i+1,currentSize - i);
		objects[i] = o;
		currentSize++;
	}

	int indexOf(Object o)
	{
		for (int i = 0 ; i < currentSize ; i++)
			if (objects[i] == o)
				return i;

		return -1;
	}

	void remove(int i)
	{
		if (i < currentSize && i >= 0)
		{
			objects[i] = null;

			int num = currentSize - i - 1;
			if (num > 0)
				System.arraycopy(objects,i+1,objects,i,num);
			currentSize--;
		}

	}

	public void set(int i,Object o)
	{
		objects[i] = o;
	}
	

	void remove(Object o)
	{
		int i = indexOf(o);
		if (i != -1)
			remove(i);
	}

	public Object get(int i)
	{
		return objects[i];
	}

	boolean contains(Object o)
	{
		return indexOf(o) != -1;
	}
}

