import java.io.*;
import java.util.*;

public class Packing{

    static int cache_cap=3000;	// size of cache
    static int sack_cap=16000;	// size of knapsack CHANGE THIS
    static int max=10000;	// max value, used to do MAX-LRU_index
    static int wt_max=50;	// [1-wt_max] is the weight of items
    static long seed=0xDEADBEEF; // seed for random number generator
    static long RUN = 50;	 // number of experiments
    static String base="/home/karthik/cache-benchmark/lirs-trc/";


    public static double [][]st;
    public static int []wt;
    public static double []val;
    public static boolean[][] sol;
    public static HashMap<Integer,Integer> ht;
    public static boolean[] take;
    public static ArrayList<Integer> lru_data;

    public static Random gen;
    public static double sum;
    public static double avg;
    public static Value obj;

    public static void main(String[] args) throws IOException{
	String base="/home/karthik/cache-benchmark/lirs-trc/";
	//exp_split_train_trace_run_knapsack_get_hitrate(base);
	exp_run_different_knapsack_capacities_hitrate(base);
    }

    public static void exp_run_different_knapsack_capacities_hitrate(String base) throws IOException{
	ArrayList<Integer> l=new ArrayList<Integer>();
	int tot=64000;
	while(tot<=cache_cap*25){
	    tot+=8000;	    
	    sack_cap=tot;
	    System.out.println(sack_cap);
	    do_exp_run_different_knapsack_capacities_hitrate(base);
	}
    }

    public static void do_exp_run_different_knapsack_capacities_hitrate(String base) throws IOException{
	BufferedReader trace=new BufferedReader(new FileReader(base+"/sprite-train.trc"));
	ht=new HashMap<Integer,Integer>(cache_cap);
	st=new double[cache_cap+1][sack_cap+1];;
	wt=new int[cache_cap+1];
	val=new double[cache_cap+1];
	sol = new boolean[cache_cap+1][sack_cap+1];
	take = new boolean[cache_cap+1];
	lru_data=null;
	int ct=0;
	LRUCache cache=new LRUCache(cache_cap);
	//while(getRefCounts(trace,Integer.MAX_VALUE,cache)){
	int split=Integer.MAX_VALUE;
	while(getRefCounts(trace,split,cache)){
	    System.out.println(++ct*split);
	    BufferedWriter w=new BufferedWriter(new FileWriter(base+"/sprite-lru1.dat"));
	    printCache(cache,w);
	    w.close();
	    setup();
	}
	System.out.println(++ct*split);
	BufferedWriter w=new BufferedWriter(new FileWriter(base+"/sprite-lru1.dat"));
	printCache(cache,w);
	w.close();
	setup();
	trace.close();

    }


    public static void exp_split_train_trace_run_knapsack_get_hitrate(String base) throws IOException{
	BufferedReader trace=new BufferedReader(new FileReader(base+"/sprite-train.trc"));
	ht=new HashMap<Integer,Integer>(cache_cap);
	st=new double[cache_cap+1][sack_cap+1];;
	wt=new int[cache_cap+1];
	val=new double[cache_cap+1];
	sol = new boolean[cache_cap+1][sack_cap+1];
	take = new boolean[cache_cap+1];
	lru_data=null;
	int ct=0;
	LRUCache cache=new LRUCache(cache_cap);
	//while(getRefCounts(trace,Integer.MAX_VALUE,cache)){
	int split=15000;
	while(getRefCounts(trace,split,cache)){
	    System.out.println(++ct*split);
	    BufferedWriter w=new BufferedWriter(new FileWriter(base+"/sprite-lru1.dat"));
	    printCache(cache,w);
	    w.close();
	    setup();
	}
	System.out.println(++ct*split);
	BufferedWriter w=new BufferedWriter(new FileWriter(base+"/sprite-lru1.dat"));
	printCache(cache,w);
	w.close();
	setup();
	trace.close();

    }

    public static void printCache(LRUCache cache,BufferedWriter w)  throws IOException{
	Node t = cache.head;
	while(t!=null){
	    w.write(t.key+"\n");
	    t=t.next;
	}
    }

    public static void setup() throws IOException{
	sum=0;
	avg=0;
	gen=new Random(seed);
	System.out.println("LfuLru");
	obj=new LfuLru();
	do_run();
	
	
	sum=0;
	avg=0;
	gen=new Random(seed);
	System.out.println("inverse LfuLru");
	obj=new  LfuLruInverse();
	do_run();

    }

    public static void do_run() throws IOException{
	for(int run=0;run<RUN;run++){
//	    System.out.println("RUN-> "+run);
	    for(int i=0;i<st.length;i++){
		Arrays.fill(st[i],0);
		Arrays.fill(sol[i],false);
	    }
	    Arrays.fill(wt,0);
	    Arrays.fill(val,0);
	    Arrays.fill(take,false);
	    // open lru cache items
	    BufferedReader lru=new BufferedReader(new FileReader(base+"/sprite-lru1.dat"));
	    // create a new list for cache items
	    lru_data=new ArrayList<Integer>(cache_cap+1);
	    // randomly assign wts for cache items
	    // value is calculated accordingly
	    get_val_wts(lru_data,lru);
	    lru.close();
	    
	    do_knapsack();
	    run_test_trace(lru_data,1);
	}
	System.out.println("FINAL AVG HIT RATE-> "+get_avg());
	System.out.print("MAX HIT RATE POSSIBLE-> ");
	run_test_trace(lru_data,0);
	System.out.println("Knapsack size-> "+((double)sack_cap/(cache_cap*25))*100+"% of cache items size"); // 25 since weights are assigned randomly between 1-50
    }
    

    public static boolean getRefCounts(BufferedReader fp,int count,LRUCache cache) throws IOException{
	int it;
	String line;
	int t=0;
	while(true){
	    line=fp.readLine();
	    if(line!=null){
		line=line.trim();
		it=Integer.parseInt(line);
		cache.set(it,0);
		if(ht.containsKey(it)==false){
		    // key is not present
		    ht.put(it,1); // init ref count
		}
		else{
		    // key is present
		    ht.put(it,ht.get(it)+1); // increment ref count		    
		}
		t++;
		if(t>=count){
		    return true;
		}
	    }
	    else{
		return false;
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
    
    public static void get_val_wts(ArrayList<Integer> lru_data,BufferedReader lru) throws IOException{
	lru_data.add(-99);	// some dummy data for 0th index
	String line;
	int it;
	int count=0;
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
	    // val[count]=(max-count)*ht.get(it);
	    val[count]=obj.get_value(max,count,ht.get(it));
	    lru_data.add(Integer.parseInt(line));
	}
	// if(count!=cache_cap){
	//     System.out.println("cache not equal to cache_cap");
	// }
	
    }

    public static void do_knapsack(){
	int n,w;
	for(n=1;n<=cache_cap;n++){
	    for(w=1;w<=sack_cap;w++){
		double option1 = st[n-1][w];
		double option2=Integer.MIN_VALUE;
		if(wt[n]<=w){
		    option2=val[n]+st[n-1][w-wt[n]];
		}
		st[n][w]=Math.max(option1,option2);
		sol[n][w]=(option2>option1);
	    }
	}

        for ( n = cache_cap, w = sack_cap; n > 0; n--) {
            if (sol[n][w]) { 
		take[n] = true;  w = w - wt[n]; 
	    }
            else{ 
		take[n] = false; 
	    }
        }
	
    }
    

    public static void run_test_trace(ArrayList<Integer> lru_data,int flag)throws IOException{
	BufferedReader test=new BufferedReader(new FileReader(base+"/sprite-test.trc"));
	int count=0;
	int hit_rate=0;
	String line;
	int ind,it;
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
		if(flag==1){
		    if(take[ind]==true){
			hit_rate++;
		    }
		}
		else{
		    hit_rate++;
		}
	    }
	    else{
		if(flag==0)
		    System.out.println("hit rate = "+(100*((double)hit_rate/count)));
		break;
	    }
	    
	}
	test.close();
	//System.out.println("max value is "+st[cache_cap][sack_cap]);
	//System.out.println("###");
	update_sum(100*((double)hit_rate/count));
        // for ( n = 1; n <= cache_cap; n++) {
        //     System.out.println(n + "\t" + val[n] + "\t" + wt[n] + "\t" + take[n]);
        // }

    }
    
    public static void update_sum(double rt){
	sum+=rt;
    }
    
    public static double get_avg(){
	return sum/RUN;
    }
}


class Node{
    int key;
    int value;
    Node pre;
    Node next;
 
    public Node(int key, int value){
        this.key = key;
        this.value = value;
    }
}


class LRUCache {
    int capacity;
    HashMap<Integer, Node> map = new HashMap<Integer, Node>();
    Node head=null;
    Node end=null;
 
    public LRUCache(int capacity) {
        this.capacity = capacity;
    }
 
    public int get(int key) {
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
            old.value = value;
            remove(old);
            setHead(old);
        }else{
            Node created = new Node(key, value);
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
}
