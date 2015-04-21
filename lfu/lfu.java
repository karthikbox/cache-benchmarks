import java.io.*;
import java.util.*;
class Lfu {
    
    public static void main(String[] args) throws IOException {
	// TODO Auto-generated method stub
	String base="/home/karthik/cache-benchmark/lirs-trc/";
	String file="sprite";
	BufferedReader par_file = new BufferedReader(new FileReader(base+file+".par"));
	BufferedWriter out_file = new BufferedWriter(new FileWriter(base+file+"_LFU.dat"));
	int cap;
	String line;
	String a;
	int it=0;
	int item_num=0;
	double hit_count=0,total=0;
	while(true){
	    BufferedReader trc_file=new BufferedReader(new FileReader(base+file+".trc"));
	    line=par_file.readLine();
	    if(line != null){
		line=line.trim();
	    }
	    else{
		break;
	    }
	    cap=Integer.parseInt(line);
	    System.out.println(cap);
	    // create a priority queue of capacity id
	    // default, head is least eleement
	    PriorityQueue<Item> heap=new PriorityQueue<Item>(cap);
	    // create a new hash table
	    HashMap<Integer,Integer> ht= new HashMap<Integer,Integer>(cap);
	    // read trc file
	    item_num=0;
	    hit_count=0;
	    total=0;
	    while(true){
		a=trc_file.readLine();
		if(a!=null){
		    it=Integer.parseInt(a.trim());
		    total++;
		}
		else{
		    // dump stats
		    System.out.println("cache size-> "+cap+" hit rate-> "+(hit_count/total)*100);
		    out_file.write(String.format("%07d  %2.1f\n",cap,(hit_count/total)*100));
		    break;
		}
		// it is item id
		if(ht.containsKey(it)){
		    // item in ht
		    // increment ref count of item
		    ht.put(it,ht.get(it)+1);
		    // min-heapify by removing and adding same item
		    // remove from Q <it,ht.get(it)-1>.i.e old value
		    if(heap.remove(new Item(it,ht.get(it)-1))!=true){
			System.out.println("item not present in heap after being inserted...fatal");
		    }
		    // add <it,new count of it> to Q
		    heap.add(new Item(it,ht.get(it)));
		    // dont increment item_count
		    // increment hit count
		    hit_count++;
		}
		else{
		    // item not present in cache
		    // increase count of cache items
		    item_num++;
		    if(item_num<=cap){
			// cache has space for more items
			// add new element to ht
			ht.put(it,1);
			// insert new element into pr Q
			heap.add(new Item(it,1));
		    }
		    else{
			// cache has no more space
			// decrement count
			item_num--;
			// evict smallest element
			Item to_remove=heap.poll();
			// remove this element from ht
			if(ht.remove(to_remove.it)==null){
			    System.out.println("item not present in HT after being inserted...fatal");
			}
			// ad new element to ht
			ht.put(it,1);
			//add new element to Q
			heap.add(new Item(it,1));
		    }
		}
	    }		    		    
	    trc_file.close();
	}
	out_file.flush();
	out_file.close();
	par_file.close();
	
    }

}
    
class Item implements Comparable<Item>{
    int it;
    int ref_cnt;
    
    Item(int a,int b){
	it=a;
	ref_cnt=b;
    }
    
    // write comparator
    public int compareTo(Item o1){
	return this.ref_cnt - o1.ref_cnt;
    }
    
    public boolean equals(Object o1){
	Item c=(Item)o1;
	return (this.it==c.it) && (this.ref_cnt==c.ref_cnt);
    }

}
