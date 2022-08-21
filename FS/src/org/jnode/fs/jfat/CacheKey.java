package org.jnode.fs.jfat;

/**
     * Here we need to "wrap" a long because Java Long wrapper is an "immutable"
     * type
     */
    public class CacheKey {
        private static final int FREE = -1;

        private int key;

        public CacheKey(int key) {
            this.key = key;
        }

        public CacheKey() {
            free();
        }

        public void free() {
            key = FREE;
        }

        public boolean isFree() {
            return (key == FREE);
        }

        public int get() {
            return key;
        }

        public void set(int value) {
            key = value;
        }

        @Override
        public int hashCode() {
            return (int) (key ^ (key >>> 16));
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CacheKey && key == ((CacheKey) obj).get();
        }

        @Override
        public String toString() {
            return String.valueOf(key);
        }
    }