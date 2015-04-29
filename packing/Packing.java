import java.io.*;
import java.util.*;

public class Packing{

    static int cache_cap=3000;	// size of cache
    static int sack_cap=600;	// size of knapsack
    static int max=10000;	// max value, used to do MAX-LRU_index
    static int wt_max=50;	// [1-wt_max] is the weight of items
    static long seed=0xDEADBEEF; // seed for random number generator

    public static int [][]st=new int[cache_cap+1][sack_cap+1];;
    public static int []wt=new int[cache_cap+1];
    public static int []val=new int[cache_cap+1];
    public static boolean[][] sol = new boolean[cache_cap+1][sack_cap+1];
    public static HashMap<Integer,Integer> ht=new HashMap<Integer,Integer>(cache_cap);


    public static void main(String[] args) throws IOException{
	String base="/home/karthik/cache-benchmark/lirs-trc/";
	BufferedReader trace=new BufferedReader(new FileReader(base+"/sprite-train.trc"));
	BufferedReader lru=new BufferedReader(new FileReader(base+"/sprite-lru.dat"));
	int count=0;
	Random gen=new Random(seed);
	
	getRefCounts(trace);
	// printHT();
	ArrayList<Integer> lru_data=new ArrayList<Integer>(cache_cap+1);
	lru_data.add(-99);	// some dummy data for 0th index
	String line;
	int it;
	while(true){
	    
	    line=lru.readLine();
	    if(line != null){
		line=line.trim();
		it=Integer.parseInt(line);
	    }
	    else{
		break;
	    }
	    count++;
	    wt[count]=gen.nextInt(wt_max)+1;
	    if(ht.get(it)==null)
		System.out.println("hello "+it+" "+ht.get(it));
	    val[count]=(max-count)*ht.get(it);
	    lru_data.add(Integer.parseInt(line));
	}
	if(count!=cache_cap){
	    System.out.println("cache not equal to cache_cap");
	}
	int n,w;
	for(n=1;n<=cache_cap;n++){
	    for(w=1;w<=sack_cap;w++){
		int option1 = st[n-1][w];
		int option2=Integer.MIN_VALUE;
		if(wt[n]<=w){
		    option2=val[n]+st[n-1][w-wt[n]];
		}
		st[n][w]=Math.max(option1,option2);
		sol[n][w]=(option2>option1);
	    }
	}
	boolean[] take = new boolean[cache_cap+1];
        for ( n = cache_cap, w = sack_cap; n > 0; n--) {
            if (sol[n][w]) { 
		take[n] = true;  w = w - wt[n]; 
	    }
            else{ 
		take[n] = false; 
	    }
        }
	
        // print results
	int size=lru_data.size();
	// for(int i=1;i<size;i++)
	//     System.out.println(lru_data.get(i));
	BufferedReader test=new BufferedReader(new FileReader(base+"/sprite-test.trc"));
	count=0;
	int hit_rate=0;

	int ind;
	while(true){
	    line=test.readLine();
	    if(line != null){
		line=line.trim();
		it=Integer.parseInt(line);
		count++;
		// check if it exists in lru items
		ind=lru_data.indexOf(it);
		if(ind==-1)
		    continue;
		if(take[ind]==true){
		    hit_rate++;
		}
	    }
	    else{
		System.out.println("hit rate = "+(100*((double)hit_rate/count)));
		break;
	    }
	    
	}
	test.close();
	System.out.println();
	System.out.println();
	System.out.println("max value is "+st[cache_cap][sack_cap]);
        System.out.println("item" + "\t" + "value" + "\t" + "weight" + "\t" + "take");
        for ( n = 1; n <= cache_cap; n++) {
            System.out.println(n + "\t" + val[n] + "\t" + wt[n] + "\t" + take[n]);
        }
	trace.close();
	lru.close();

    }
    

    public static void getRefCounts(BufferedReader fp) throws IOException{
	int it;
	String line;
	while(true){
	    line=fp.readLine();
	    if(line!=null){
		line=line.trim();
		it=Integer.parseInt(line);
		if(ht.containsKey(it)==false){
		    // key is not present
		    ht.put(it,1); // init ref count
		}
		else{
		    // key is present
		    ht.put(it,ht.get(it)+1); // increment ref count		    
		}
	    }
	    else{
		return;
	    }
	}
    }
    
    public static void printHT(){
	Iterator itr = ht.entrySet().iterator();
	for(;itr.hasNext();){
	    Map.Entry pair=(Map.Entry)itr.next();
	    System.out.println(pair.getKey()+" = "+ pair.getValue());
	    itr.remove();
	}

    }
}
