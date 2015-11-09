import java.lang.Double;
import java.lang.InterruptedException;
import java.lang.System;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Integer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;


class Simulation{


    static LRUCache[] caches;
    static LRUCache masterCache;
    static String base="/Users/karthik/cache-benchmarks/lirs-trc/";
    static HashMap<Integer, Integer> unique_ht_count; // do not use this for knapsack value
    static ArrayList<Integer> unique_al;
    static HashMap<Integer,Integer> unique_ht_weight;

    static long seed=0xDEADBEEF; // seed for random number generator
    static int wt_max=50;
    static int MAX=10000;
    static double knapsackWeight=0.1; // as percentage
    static double masterKnapsackWeight=knapsackWeight*10;

    public static void main(String[] args) throws IOException{
        initCaches(10, 3000);
        readUniqueItemsFromFile("sprite-train.trc");
        assignRandomWeightsTo(unique_al);
        trainCaches("sprite-train.trc");
        runTestTraceAgainstCachesUsing("sprite-test.trc");
        ArrayList<Boolean[]> hotDataOfAllCaches=doKnapSackOnCaches();
        runTestTraceAgainstKnapsacksUsing("sprite-test.trc");
        //create a master knapsack
        masterCache=mergeAllCachesAndInitCountOrder();
        //run knapsack on this mastercace with size 20%
        int w=(int)Math.ceil(masterKnapsackWeight*unique_al.size()*25);
        int n=masterCache.cache_order.size();
        double[][] table=new double[23000][5000];
        boolean[][] sol=new boolean[23000][5000];
        boolean[] taken=new boolean[23000];
        for(double[] t:table) {
            Arrays.fill(t,0);
        }
        for(boolean[] t:sol){
            Arrays.fill(t,false);
        }
        Arrays.fill(taken,false);
        System.out.println("MASTER CACHE"+" w="+w+" n="+n);
        runKnapsackOnMaster(masterCache, w, n, table, sol, taken);
        masterCache.initHotDataMapFromTakenArray(taken);
        //runTestTrace against master knapsack
        runTestTraceAgainstMasterKnapsackUsing("sprite-test.trc");
    }

    public static void runTestTraceAgainstMasterKnapsackUsing(String file)  throws IOException{
        BufferedReader r = new BufferedReader(new FileReader(base + "/" + file));
        String line;
        int hits=0,miss=0;
        int total_hits=0;
        int total_miss=0;
        while((line=r.readLine())!=null){
            int a=Integer.parseInt(line);
            if(masterCache.hotDataSetFromTakenArray.contains(a)){
                hits++;
                total_hits++;
            }
            else{
                miss++;
                total_miss++;
            }
        }
        String str="";
        str+=String.format("%d,%d,%f,%d\n",hits,miss,((float)hits/(hits+miss)),masterCache.hotDataSetFromTakenArray.size());
        //System.out.println("total hits="+total_hits+" total miss="+total_miss+" total hitrate="+((float)total_hits/(total_hits+total_miss)));
        System.out.println("master cache stats");
        System.out.println(str);
        r.close();
    }

    static void runKnapsackOnMaster(LRUCache c,int W,int N,double[][] table,boolean[][] sol,boolean[] taken){
        for(int i=0;i<=N;i++){
            table[0][i]=0;
        }
        for(int i=0;i<=W;i++){
            table[i][0]=0;
        }
        //System.out.println("1");
        for(int i=1;i<=W;i++) {
            //System.out.println("master cache w="+i+" <= "+W);
            for (int j = 1; j <= N; j++) {
                //System.out.println("master cache n="+j+" <= "+N);
                double a1 = table[i][j - 1];// without j
                double a2 = Integer.MIN_VALUE;
                //System.out.println(" j="+j+" wight "+c.cache_order.get(j));
                if (i >= weight(c,j))
                {


                    a2 = table[i - weight(c,j)][j - 1] + value(c,j);//with j
                }
                table[i][j] = Math.max(a1, a2);
                sol[i][j] = a1 < a2;
            }
        }
        //System.out.println("2");
        int w=W;
        for(int j=N;j>0;j--){
            if(sol[w][j]==true){
                taken[j]=true;
                w=w-weight(c,j);
                //System.out.println("w= "+w);
            }
            else{
                taken[j]=false;
            }
        }
    }

    static LRUCache mergeAllCachesAndInitCountOrder(){
        //iterate over every cache
        //from each cache get the countOrder map
        LRUCache master=new LRUCache(caches[0].capacity*10);//enough space for 10 caches...but we wont use this anyway
        int count=1;

        for(int i =0;i<caches.length;i++){
            int start=1;
            int end=caches[i].cache_order.size();
            for(int t=start;t<=end;t++) {
                master.cache_order.put(count,caches[i].cache_order.get(t));
                count++;
            }
        }
        return master;
    }

    public static ArrayList<Boolean[]> doKnapSackOnCaches(){

        double[][] table=new double[5000][5000];
        boolean[][] sol=new boolean[5000][5000];
        boolean[] taken=new boolean[5000];
        ArrayList<Boolean[]> hotDataOfAllCaches = new ArrayList<Boolean[]>();
        for(int i=0;i<caches.length; i++) {
            int w=(int)Math.ceil(knapsackWeight*unique_al.size()*25);
            int n=caches[i].map.size();
            System.out.println("cache #"+i+" w="+w+" n="+n);
            for(double[] t:table) {
                Arrays.fill(t,0);
            }
            for(boolean[] t:sol){
                Arrays.fill(t,false);
            }
            Arrays.fill(taken,false);
            doKnapsack(caches[i], w, n, table,sol,taken);
            //convert taken array indexes to element key hashset. element key is trace numbers.
            caches[i].initHotDataMapFromTakenArray(taken);
        }
        return hotDataOfAllCaches;
    }

    public static int weight(LRUCache c,int a){

        if(c.cache_order.containsKey(a)){
            int key= c.cache_order.get(a).key;
            if(unique_ht_weight.containsKey(key)){
                return unique_ht_weight.get(key);
            }
            else{
                System.out.println("invalid weight for item key "+key);
                return 0;
            }
        }
        else{
            System.out.println("invalid weight for item key "+a);
            return 0;
        }
    }

    public static double value(LRUCache c,int a){
        if(c.cache_order.containsKey(a)){
            return c.cache_order.get(a).value;
        }
        else{
            System.out.println("invalid value for item key "+a);
            return 0;
        }
    }



    public static void doKnapsack(LRUCache c,int W,int N,double[][] table,boolean[][] sol,boolean[] taken){
        c.initCacheOrder(MAX);
        for(int i=0;i<=N;i++){
            table[0][i]=0;
        }
        for(int i=0;i<=W;i++){
            table[i][0]=0;
        }
        //System.out.println("1");
        for(int i=1;i<=W;i++) {
            for (int j = 1; j <= N; j++) {
                double a1 = table[i][j - 1];// without j
                double a2 = Integer.MIN_VALUE;
                if (i >= weight(c,j)) {
                    a2 = table[i - weight(c,j)][j - 1] + value(c,j);//with j
                }
                table[i][j] = Math.max(a1, a2);
                sol[i][j] = a1 < a2;
            }
        }
        //System.out.println("2");
        int w=W;
        for(int j=N;j>0;j--){
            if(sol[w][j]==true){
                taken[j]=true;
                w=w-weight(c,j);
                //System.out.println("w= "+w);
            }
            else{
                taken[j]=false;
            }
        }

    }

    //this is for post-knapsack hotdata
    public static void runTestTraceAgainstKnapsacksUsing(String file)  throws IOException{
        BufferedReader r = new BufferedReader(new FileReader(base + "/" + file));
        String line;
        int[] hits=new int[caches.length];
        int[] miss=new int[caches.length];
        Arrays.fill(hits,0);
        Arrays.fill(miss,0);
        int total_hits=0;
        int total_miss=0;
        while((line=r.readLine())!=null){
            int a=Integer.parseInt(line);
            int cacheIndex=a%caches.length;
            if(caches[cacheIndex].hotDataSetFromTakenArray.contains(a)){
                hits[cacheIndex]++;
                total_hits++;
            }
            else{
                miss[cacheIndex]++;
                total_miss++;
            }
        }
        String str="";
        for(int i=0;i<caches.length;i++){
            System.out.println("hot-cache #"+i+" hits="+hits[i]+" miss="+miss[i]+" hitrate="+((float)hits[i]/(hits[i]+miss[i]))+" cached items="+caches[i].hotDataSetFromTakenArray.size());
            str+=String.format("%d,%d,%d,%f,%d\n",i,hits[i],miss[i],((float)hits[i]/(hits[i]+miss[i])),caches[i].hotDataSetFromTakenArray.size());
        }
        System.out.println("total hits="+total_hits+" total miss="+total_miss+" total hitrate="+((float)total_hits/(total_hits+total_miss)));
        str+=String.format("%d,%d,%d,%f,%d\n",caches.length,total_hits,total_miss,((float)total_hits/(total_hits+total_miss)),10);
        System.out.println();
        System.out.println(str);
        r.close();
    }

    //this is for pre-knapsack cached data
    public static void runTestTraceAgainstCachesUsing(String file)  throws IOException{
        BufferedReader r = new BufferedReader(new FileReader(base + "/" + file));
        String line;
        int[] hits=new int[caches.length];
        int[] miss=new int[caches.length];
        Arrays.fill(hits,0);
        Arrays.fill(miss,0);
        int total_hits=0;
        int total_miss=0;
        while((line=r.readLine())!=null){
            int a=Integer.parseInt(line);
            int cacheIndex=a%caches.length;
            if(caches[cacheIndex].map.containsKey(a)){
                hits[cacheIndex]++;
                total_hits++;
            }
            else{
                miss[cacheIndex]++;
                total_miss++;
            }
        }
        String str="";
        for(int i=0;i<caches.length;i++){
            //System.out.println("cache #"+i+" hits="+hits[i]+" miss="+miss[i]+" hitrate="+((float)hits[i]/(hits[i]+miss[i]))+" cached items="+caches[i].map.size());
            str+=String.format("%d,%d,%d,%f,%d\n",i,hits[i],miss[i],((float)hits[i]/(hits[i]+miss[i])),caches[i].map.size());
        }
        //System.out.println("total hits="+total_hits+" total miss="+total_miss+" total hitrate="+((float)total_hits/(total_hits+total_miss)));
        str+=String.format("%d,%d,%d,%f,%d\n",caches.length,total_hits,total_miss,((float)total_hits/(total_hits+total_miss)),caches[0].map.size()*caches.length);
        System.out.println();
        System.out.println(str);
        r.close();
    }


    public static void trainCaches(String file) throws IOException{
        BufferedReader r = new BufferedReader(new FileReader(base + "/" + file));
        String line;
        while((line=r.readLine())!=null){
            int a=Integer.parseInt(line);
            int cacheIndex=a%caches.length;
            caches[cacheIndex].set(a,0);
        }
        r.close();
    }

    public static void assignRandomWeightsTo(ArrayList<Integer> al){
        unique_ht_weight=new HashMap<Integer,Integer>();
        Random gen=new Random(seed);
        for(int a:al){
            unique_ht_weight.put(a,gen.nextInt(wt_max)+1);
        }
    }

    public static void readUniqueItemsFromFile(String file) throws IOException{
        BufferedReader r = new BufferedReader(new FileReader(base + "/" + file));
        String line;
        HashMap<Integer, Integer> ht = new HashMap<Integer, Integer>();
        ArrayList<Integer> al = new ArrayList<Integer>();
        while ((line = r.readLine()) != null) {
            int item = Integer.parseInt(line);
            if (ht.containsKey(item)) {
                ht.put(item, ht.get(item) + 1);

            } else {
                ht.put(item, 1);
                al.add(item);
            }
        }
        unique_al=al;
        unique_ht_count=ht;
        r.close();

    }

    public static void initCaches(int n,int cap){
        caches=new LRUCache[n];
        for(int i=0;i<n;i++){
            caches[i]=new LRUCache(cap);
        }
    }
}


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


class LRUCache {
    int capacity;
    HashMap<Integer, Node> map = new HashMap<Integer, Node>();
    Node head=null;
    Node end=null;
    HashMap<Integer,Node> cache_order = new HashMap<Integer,Node>();
    HashSet<Integer> hotDataSetFromTakenArray = null;

    public void initHotDataMapFromTakenArray(boolean[] taken){
        hotDataSetFromTakenArray= new HashSet<Integer>();
        for(int i=0;i<taken.length;i++){
            if(taken[i]){
                hotDataSetFromTakenArray.add(cache_order.get(i).key);
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

    public LRUCache(int capacity) {
        this.capacity = capacity;
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
}
