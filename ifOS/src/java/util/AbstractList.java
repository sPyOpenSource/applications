package java.util;

public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> 
{
    
    @Override
    public void sort(Comparator<? super E> c) {
        Object[] a = this.toArray();
        Arrays.sort(a, (Comparator) c);
        ListIterator<E> i = this.listIterator();
        for (Object e : a) {
            i.next();
            i.set((E) e);
        }
    }
    
    @Override
    public boolean add(E e) {
        add(size(), e);
        return true;
    }
    
    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Iterator<E> iterator() {
	throw new Error("ITERATOR");
    }
    
    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    abstract public E get(int index);
    
    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }
    
    @Override
    public ListIterator<E> listIterator(final int index) {
        /*rangeCheckForAdd(index);

        return new ListItr(index);*/
        throw new Error("Object method not implemented");
    }
    
    @Override
    public int indexOf(Object o) {
        ListIterator<E> it = listIterator();
        if (o==null) {
            while (it.hasNext())
                if (it.next()==null)
                    return it.previousIndex();
        } else {
            while (it.hasNext())
                if (o.equals(it.next()))
                    return it.previousIndex();
        }
        return -1;
    }
    
    @Override
    public int lastIndexOf(Object o) {
        ListIterator<E> it = listIterator(size());
        if (o == null) {
            while (it.hasPrevious())
                if (it.previous() == null)
                    return it.nextIndex();
        } else {
            while (it.hasPrevious())
                if (o.equals(it.previous()))
                    return it.nextIndex();
        }
        return -1;
    }
    
    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        /*return (this instanceof RandomAccess ?
                new RandomAccessSubList<>(this, fromIndex, toIndex) :
                new SubList<>(this, fromIndex, toIndex));*/
        throw new Error("Object method not implemented");
    }
    
}
