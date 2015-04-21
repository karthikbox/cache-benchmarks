import java.io.*;
import java.util.*;

public class Lrfu{

    public static double lamda=0.0;

    public static void main(String[] args) throws IOException{
	String base="/home/karthik/cache-benchmark/lirs-trc/";
	String file="sprite";
	BufferedWriter out_file = new BufferedWriter(new FileWriter(base+file+"_LRFU.dat"));
	for(Lrfu.lamda=0;Lrfu.lamda<=2;Lrfu.lamda+=0.01){
	    System.out.print(String.format("\nLamda->%2.8f\n",Lrfu.lamda));
	    out_file.write(String.format("\nLamda->%2.8f\n",Lrfu.lamda));
	    BufferedReader par_file = new BufferedReader(new FileReader(base+file+".par"));

	    int cap;
	    String line;
	    String a;
	    int it=0;
	    int count=0;
	    long tc=-1;
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
		HashMap<Integer,Item> ht= new HashMap<Integer,Item>(cap);
		// read trc file
		hit_count=0;
		total=0;
		count=0;
		tc=-1;
		while(true){
		    a=trc_file.readLine();
		    if(a!=null){
			it=Integer.parseInt(a.trim());
			total++;
			tc++;
		    }
		    else{
			// dump stats
			System.out.println("cache size-> "+cap+" hit rate-> "+(hit_count/total)*100);
			out_file.write(String.format("%07d  %2.1f\n",cap,(hit_count/total)*100));
			break;
		    }
		    // F(0) = 1
		    // it is item id
		    if(ht.containsKey(it)){
			// get item from ht
			Item t=ht.get(it);
			// remove  Item from heap
			if(heap.remove(t)!=true){
			    System.out.println("item not present in heap after being inserted...fatal");
			}
			// update Item's crf
			t.update(tc);
			// add Item to heap
			heap.add(t);
			// update ht value for it
			ht.put(it,t);
			// increase hit count
			hit_count++;
		    }
		    else{
			// item not present in cache
			if(count<cap){
			    // cache has space for more items
			    // incr count of items in cache
			    count++;
			    // create new Item for it
			    Item t=new Item(tc,1,it);
			    // add new element to ht
			    ht.put(it,t);
			    // insert new element to heap
			    heap.add(t);
			}
			else{
			    // cache has no more space
			    // evict item
			    // get item with lowest crf , ie head of heap
			    Item t= heap.poll();
			    if(t==null){
				// head should have an item
				System.out.println("error...no element on top of heap");
			    }
			    // t is the object to evict
			    // remove t from ht
			    ht.remove(t.it);
			    // create a new Item from it
			    t=new Item(tc,1,it);
			    // add new Item to ht
			    ht.put(it,t);
			    // add new Item to heap
			    heap.add(t);
			    // no change to count
			}
		    }
		}
		
		trc_file.close();
	    }
	    par_file.close();
	}
	out_file.flush();
	out_file.close();
    }

}


class Item implements Comparable<Item>{
    long last;
    double crf;
    int it;

    Item(long time_v,double crf_v,int i){
	last=time_v;
	crf=crf_v;
	it=i;
    }
    // update method
    public void update(long tm){
	// crf_new=1+(t_cur - t_last)crf_old
	crf=1+F(tm-last)*crf;
	last=tm;
    }
    // write comparator
    public int compareTo(Item o1){
	if( (this.crf - o1.crf) > 0){
	    return 1;
	}
	else if( (this.crf - o1.crf) < 0){
	    return -1;
	}
	else{
	    return 0;
	}
    }
    
    // public boolean equals(Object o1){
    // 	Item c=(Item)o1;
    // 	return (this.it==c.it) && (this.crf==c.crf);
    // }

    public static double F(double t){
	if(t<0)
	    System.out.println("time is less than 0");
	return Math.pow(0.5,t*Lrfu.lamda);
    }

}
