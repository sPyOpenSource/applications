/**
 * $Id: FastArray.java 71 2010-07-02 06:55:21Z mviara $
 */
package j51.util;

/**
 * A class like  standard java.util.ArrayList but without
 * implementation of list and synchronization for maximum
 * performance.
 * 
 * @author Mario Viara
 * @version 1.00
 *
 * @since 1.04
 */
public class FastArray<E>
{
	private int growSize;
	private Object objects[];
	private int currentSize;

	public FastArray(int initialCapacity,int growSize)
	{
		objects = new Object[initialCapacity];
		currentSize = 0;
		this.growSize = growSize;
	}

	public FastArray(int n)
	{
		this(n,10);
	}

	public  FastArray()
	{
		this(0);
	}

	public final void setGrowSize(int n)
	{
		growSize = n;
	}

	public final int getGrowSize()
	{
		return growSize;
	}

	public final int size()
	{
		return currentSize;
	}

	private final void checkSize()
	{
		if (currentSize >= objects.length)
		{
			Object newObjects[] = new Object[objects.length+growSize];
			System.arraycopy(objects,0,newObjects,0,objects.length);
			objects = newObjects;
		}

	}

	public final void clear()
	{
		currentSize = 0;
	}

	public final void add(E o)
	{
		checkSize();
		objects[currentSize++] = o;
	}

	public final void add(int i,Object o)
	{
		checkSize();
		System.arraycopy(objects,i,objects,i+1,currentSize - i);
		objects[i] = o;
		currentSize++;
	}

	public final int indexOf(Object o)
	{
		for (int i = currentSize ; --i >= 0 ;)
			if (objects[i] == o)
				return i;

		return -1;
	}


	public final void remove(int i)
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


	public final void remove(Object o)
	{
		int i = indexOf(o);
		if (i != -1)
			remove(i);
	}

	public final void set(int i,E o)
	{
		objects[i] = o;
	}
	
	public final E get(int i)
	{
		return (E)objects[i];
	}

	public final boolean contains(Object o)
	{
		return indexOf(o) != -1;
	}

}
