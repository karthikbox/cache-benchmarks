package com.project;


import java.math.BigInteger;
import java.util.Map;

public class Main {

    public static final int CACHE_NUM=10;
    public static final int CACHE_CAPACITY=3000;

    public static void main(String[] args) throws HashGenerationException {
        //init caches
        LRUCache[] allCaches=new LRUCache[CACHE_NUM];
        for(int i=0;i<allCaches.length;i++){
            allCaches[i]=new LRUCache(CACHE_CAPACITY,"cacheId"+i);
        }
        //create hashRing and insert cache into it
        ConsistentHashing lookupRing=new ConsistentHashing(new HashFunction(),8,allCaches);
        //print nodes in the lookupring along with their md5's
        for(Map.Entry<BigInteger,LRUCache> c:lookupRing.circle.entrySet()){
            System.out.println(c.getValue().cacheId+" "+c.getKey().toString(10));
        }
    }
}
