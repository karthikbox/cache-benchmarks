package com.project;
/**
 * Created by karthik on 11/8/15.
 */

class Node{
    int key;
    double value;
    int count;
    Node pre;
    Node next;

    public Node(int key, int value,int count){
        this.key = key;
        this.value = value;
        this.count=count;
    }
}