import java.io.*;
import java.util.*;

public class Packing{
    static int cache_cap=3000;
    static int sack_cap=600;
    public static int [][]st=new int[cache_cap+1][sack_cap+1];;
    public static int []wt=new int[cache_cap+1];
    public static int []val=new int[cache_cap+1];
    public static boolean[][] sol = new boolean[cache_cap+1][sack_cap+1];

    public static void main(String[] args) throws IOException{
	String base="/home/karthik/cache-benchmark/lirs-trc/";
	BufferedReader trace=new BufferedReader(new FileReader(base+"/sprite-train.trc"));
	BufferedReader lru=new BufferedReader(new FileReader(base+"/sprite-lru.dat"));
	int count=0;
	int max=10000;
	int wt_max=50;
	long seed=0xDEADBEEF;
	Random gen=new Random(seed);

	String line;
	while(true){
	    
	    line=lru.readLine();
	    if(line != null){
		line=line.trim();
	    }
	    else{
		break;
	    }
	    count++;
	    wt[count]=gen.nextInt(wt_max)+1;
	    val[count]=max-count;
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
	System.out.println("max value is "+st[cache_cap][sack_cap]);
        System.out.println("item" + "\t" + "value" + "\t" + "weight" + "\t" + "take");
        for ( n = 1; n <= cache_cap; n++) {
            System.out.println(n + "\t" + val[n] + "\t" + wt[n] + "\t" + take[n]);
        }
	trace.close();
	lru.close();
    }
}
