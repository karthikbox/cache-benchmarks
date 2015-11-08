package com.project;

import java.math.BigInteger;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by karthik on 11/7/15.
 */
public class ConsistentHashing {
    HashFunction hash;
    TreeMap<BigInteger,LRUCache> circle = new TreeMap<BigInteger, LRUCache>();
    int numReplicas;

    ConsistentHashing(HashFunction h,int reps,LRUCache[] allCaches) throws HashGenerationException {
        this.hash=h;
        this.numReplicas=reps;
        for(LRUCache t:allCaches){
            add(t);
        }
    }

    public void add(LRUCache c) throws HashGenerationException {
        for(int i=0;i<this.numReplicas;i++) {
            circle.put(hash.generateMD5(c.toString()+i),c);
        }
    }

    public void remove(LRUCache c) throws HashGenerationException {
        for(int i=0;i<this.numReplicas;i++) {
            circle.remove(hash.generateMD5(c.toString()+i));
        }
    }

    public LRUCache get(int key) throws HashGenerationException {
        if(circle.size()==0){
            return null;
        }
        else{
            BigInteger keyHash=hash.generateMD5(Integer.toString(key));
            SortedMap<BigInteger,LRUCache> t=circle.tailMap(keyHash);
            return circle.get(t.isEmpty()?circle.firstKey():t.firstKey());
        }
    }
}
