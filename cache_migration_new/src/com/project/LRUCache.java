package com.project;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by karthik on 11/8/15.
 */
public class LRUCache {
    int capacity;
    HashMap<Integer, Node> map = new HashMap<Integer, Node>();
    Node head=null;
    Node end=null;
    HashMap<Integer,Node> cache_order = new HashMap<Integer,Node>();
    HashSet<Integer> hotDataSetFromTakenArray = null;
    String cacheId;

    public void initHotDataMapFromTakenArray(boolean[] taken){
        hotDataSetFromTakenArray= new HashSet<Integer>();
        for(int i=0;i<taken.length;i++){
            if(taken[i]){
                hotDataSetFromTakenArray.add(cache_order.get(i).key); //get(i) is correct
                //since token starts from index 1 to N
            }
        }
    }

    public void initCacheOrder(int max){
        cache_order.clear();
        int count=1;
        Node t=head;
        while(t!=null){
            cache_order.put(count,t);
            t.value=((double)max/(double)(count+1))*(double)t.count;
            count++;
            t=t.next;
        }
    }

    public LRUCache(int capacity, String s) {
        this.capacity = capacity;
        cacheId=s;
    }



    public double get(int key) {
        if(map.containsKey(key)){
            Node n = map.get(key);
            remove(n);
            setHead(n);
            return n.value;
        }

        return -1;
    }

    public void remove(Node n){
        if(n.pre!=null){
            n.pre.next = n.next;
        }else{
            head = n.next;
        }

        if(n.next!=null){
            n.next.pre = n.pre;
        }else{
            end = n.pre;
        }

    }

    public void setHead(Node n){
        n.next = head;
        n.pre = null;

        if(head!=null)
            head.pre = n;

        head = n;

        if(end ==null)
            end = head;
    }

    public void set(int key, int value) {
        if(map.containsKey(key)){
            Node old = map.get(key);
            old.count+=1;
            old.value = value;
            remove(old);
            setHead(old);
        }else{
            Node created = new Node(key, value,1);
            if(map.size()>=capacity){
                map.remove(end.key);
                remove(end);
                setHead(created);

            }else{
                setHead(created);
            }

            map.put(key, created);
        }
    }

    @Override
    public String toString() {
        return this.cacheId;
    }
}
