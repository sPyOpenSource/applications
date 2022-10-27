package org.jnode.fs.jfat;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import jx.devices.bio.BlockIO;

public class CacheMap extends LinkedHashMap<CacheKey, CacheElement> 
{
    private static final long serialVersionUID = 1L;
    private final int cacheSize;
    private final CacheKey key = new CacheKey();
    private final Stack<CacheElement> free = new Stack<>();

    public CacheMap(int cacheSize) {
        super(/*(int) Math.ceil(cacheSize / org.jnode.fs.jfat.FatCache.loadFactor) + 1*/11/*, org.jnode.fs.jfat.FatCache.loadFactor, true*/);
        this.cacheSize = cacheSize;

        for (int i = 0; i < cacheSize + 1; i++)
            free.push(new CacheElement());
    }

    public CacheMap(int cacheSize, BlockIO api) {
       super(/*(int) Math.ceil(cacheSize / org.jnode.fs.jfat.FatCache.loadFactor) + 1*/11/*, org.jnode.fs.jfat.FatCache.loadFactor, true*/);
            this.cacheSize = cacheSize;

            for (int i = 0; i < cacheSize + 1; i++)
                free.push(new CacheElement());
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public int usedEntries() {
        return size();
    }

    public int freeEntries() {
        return (free.size() - 1);
    }

    public CacheElement peek() {
        return free.peek();
    }

    private CacheElement push(CacheElement c) {
        return free.push(c);
    }

    public CacheElement pop() {
        return free.pop();
    }

    public CacheElement get(int address) {
        key.set(address);
        return get(key);
    }

    public CacheElement put(CacheElement c) {
        return put(c.getAddress(), c);
    }

    /**
     * discard the eldest element when the cache is full
     * @return 
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<CacheKey, CacheElement> eldest) {
        boolean remove = (size() > cacheSize);

        /**
         * before going to discard the eldest push it back on the stacked
         * object pool
         */
        if (remove)
            push(eldest.getValue());

        return remove;
    }

    /*public String flushOrder() {
        StrWriter out = new StrWriter();

        for (CacheElement c : values()) {
            if (c.isDirty())
                out.print("<" + c.getAddress().get() + ">");
        }

        return out.toString();
    }

    public String toString() {
        StrWriter out = new StrWriter();

        for (CacheElement c : values())
            out.println(c);

        return out.toString();
    }*/
}