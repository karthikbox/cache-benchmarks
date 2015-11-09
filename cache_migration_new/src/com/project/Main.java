package com.project;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class Main {


    public static int CACHE_CAPACITY=300;//DEFAULT SIZE IS 3000
    private static double knapsackWeight=0.2; // as percentage, DEFAULT SIZE IS 0.02



    public static final int CACHE_NUM=10;
    private static ArrayList<Integer> unique_al;
    static String base="/Users/karthik/cache-benchmarks/lirs-trc";
    private static HashMap<Integer, Integer> unique_ht_count;
    private static final long SEED=0xDEADBEEF;
    private static final int REPLFACTOR=8;
    private static int wt_max=50;
    private static int MAX=10000;
    private static HashMap<Integer, Integer> unique_ht_weight;


    public static void main(String[] args) throws HashGenerationException, IOException {
//        if(args.length < 2){
//            System.out.println("java Main KnapsackWeight CacheSize");
//            System.out.println("java Main 0.02 3000");
//            return;
//        }
//        knapsackWeight=Double.parseDouble(args[0]);
//        CACHE_CAPACITY=Integer.parseInt(args[1]);
        //init caches
        System.out.println("cache size= "+CACHE_CAPACITY+"\n"+"knapsackWeight= "+knapsackWeight+"\n");
        LRUCache[] allCaches=new LRUCache[CACHE_NUM];
        for(int i=0;i<allCaches.length;i++){
            allCaches[i]=new LRUCache(CACHE_CAPACITY,"cacheId"+i);
        }
        //create hashRing and insert cache into it
        ConsistentHashing lookupRing=new ConsistentHashing(new HashFunction(),REPLFACTOR,allCaches);
        //print nodes in the lookupring along with their md5's
//        for(Map.Entry<BigInteger,LRUCache> c:lookupRing.circle.entrySet()){
//            System.out.println(c.getValue().cacheId+" "+c.getKey().toString(10));
//        }
        //use it decide the sizse of knpsack, no other use of this method
        readUniqueItemsFromFile("sprite-train.trc");
//        System.out.println("size of unique items= "+unique_al.size());
        //assign random weight to items
        assignRandomWeightsTo(unique_al);
        //train the caches using train trace
        trainCaches("sprite-train.trc",lookupRing);
        runNaiveModel(lookupRing, allCaches);
//        for(LRUCache c:allCaches) {
//            System.out.println(c.cacheId+" "+c.map.size());
//        }
        //runHotDatDistributionModel(lookupRing,allCaches);

    }

    public static void assignRandomWeightsTo(ArrayList<Integer> al){
        unique_ht_weight=new HashMap<Integer,Integer>();
        Random gen=new Random(SEED);
        for(int a:al){
            unique_ht_weight.put(a,gen.nextInt(wt_max)+1);
        }
    }


    private static void runHotDatDistributionModel(ConsistentHashing lookupRing, LRUCache[] allCaches) throws HashGenerationException, IOException {
        //pick a random node to kill. Random node is an integer between 0-9
        Random rand=new Random(SEED);
        int node_kill_index=rand.nextInt(CACHE_NUM);
        System.out.println("random node to be killed = "+node_kill_index);

        //remove node from lookup ring
        lookupRing.remove(allCaches[node_kill_index]);

        //apply knapsack at the removed node to get the hot data
        doKnapsackOnCache(allCaches[node_kill_index]);
//        System.out.println(allCaches[node_kill_index].hotDataSetFromTakenArray.size()+"\n"+allCaches[node_kill_index].cache_order.size());

//        for(LRUCache c:allCaches){
//            System.out.println(c.cacheId+" "+c.map.size());
//        }


        //for each item in hot data redistribute to other nodes, which we get through the lookupring since
        for(int a:allCaches[node_kill_index].hotDataSetFromTakenArray){
            //get next node for this item, note we already removed the node from the lookupring
            LRUCache c=lookupRing.get(a);
            //add this new item to the new node. Let the node update itself
            c.set(a,0);
        }

//        System.out.println("hot data from "+allCaches[node_kill_index].cacheId+" redistributed");
//        for(LRUCache c:allCaches){
//            System.out.println(c.cacheId+" "+c.map.size());
//        }

        //run test trace on this new lookup ring by allowing caches updates
        testCaches("sprite-test.trc",lookupRing);
    }

    private static void doKnapsackOnCache(LRUCache killedCache) {
        //allocate memory


        int w=(int)Math.ceil(knapsackWeight*unique_al.size()*25);
        int n=killedCache.map.size();
        double[][] table=new double[(int)Math.ceil(w)+10][(int)Math.ceil(n)+10];
        boolean[][] sol=new boolean[(int)Math.ceil(w)+10][(int)Math.ceil(n)+10];
        boolean[] taken=new boolean[(int)Math.ceil(n)+10];
        System.out.println("killed cache id "+killedCache.cacheId+" w="+w+" n="+n);
        for(double[] t:table) {
            Arrays.fill(t,0);
        }
        for(boolean[] t:sol){
            Arrays.fill(t,false);
        }
        Arrays.fill(taken,false);
        doKnapsack(killedCache, w, n, table,sol,taken);
        //convert taken array indexes to element key hashset. element key is trace numbers.
        killedCache.initHotDataMapFromTakenArray(taken);

    }


    public static void doKnapsack(LRUCache c,int W,int N,double[][] table,boolean[][] sol,boolean[] taken){
        //resume here
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


    private static void runNaiveModel(ConsistentHashing lookupRing, LRUCache[] allCaches) throws IOException, HashGenerationException {
        //pick a random node to kill. Random node is an integer between 0-9
        Random rand=new Random(SEED);
        int node_kill_index=rand.nextInt(CACHE_NUM);
        System.out.println("random node to be killed = "+node_kill_index);
        //remove node from lookup ring
        lookupRing.remove(allCaches[node_kill_index]);
        //run test trace on this lookup ring
        testCaches("sprite-test.trc",lookupRing);
    }

    private static void testCaches(String file, ConsistentHashing lookupRing) throws IOException, HashGenerationException {
        BufferedReader r = new BufferedReader(new FileReader(base + "/" + file));
        String line;
        HashMap<LRUCache,Integer> hitmap=new HashMap<LRUCache,Integer>();
        HashMap<LRUCache,Integer> missmap=new HashMap<LRUCache,Integer>();
        int total_hits=0;
        int total_miss=0;
        while((line=r.readLine())!=null){
            int a=Integer.parseInt(line);
//            int cacheIndex=a%caches.length;
            LRUCache c =lookupRing.get(a);
            if(c.map.containsKey(a)){
                if(hitmap.containsKey(c)){
                    hitmap.put(c,hitmap.get(c)+1);
                }
                else{
                    hitmap.put(c,1);
                }
                total_hits++;
                //since this cache has item which mapped to this cache,
                // incr hit count
                // update the LRU of this cache
                c.set(a,0);
            }
            else{
                if(missmap.containsKey(c)){
                    missmap.put(c,hitmap.get(c)+1);
                }
                else{
                    missmap.put(c,1);
                }
                total_miss++;
                //since this cache didnt have an item it should have but doesnt, increment miss count
                // and add this item to cache
                c.set(a,0);
            }
        }
        String str="";
//        for(int i=0;i<caches.length;i++){
//            //System.out.println("cache #"+i+" hits="+hits[i]+" miss="+miss[i]+" hitrate="+((float)hits[i]/(hits[i]+miss[i]))+" cached items="+caches[i].map.size());
//            str+=String.format("%d,%d,%d,%f,%d\n",i,hits[i],miss[i],((float)hits[i]/(hits[i]+miss[i])),caches[i].map.size());
//        }
        //System.out.println("total hits="+total_hits+" total miss="+total_miss+" total hitrate="+((float)total_hits/(total_hits+total_miss)));
        str+=String.format("%d,%d,%f\n",total_hits,total_miss,((float)total_hits/(total_hits+total_miss)));
        System.out.println();
        System.out.println(str);
        r.close();

    }

    public static void trainCaches(String file, ConsistentHashing lookupRing) throws IOException, HashGenerationException {
        BufferedReader r = new BufferedReader(new FileReader(base + "/" + file));
        String line;
        while((line=r.readLine())!=null){
            int a=Integer.parseInt(line);
            LRUCache c =lookupRing.get(a);
//            System.out.println("cacheID= "+c.cacheId+" item hash= "+HashFunction.generateMD5(Integer.toString(a)));
            c.set(a,0);
        }
        r.close();
    }

    public static void readUniqueItemsFromFile(String file) throws IOException {
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

}