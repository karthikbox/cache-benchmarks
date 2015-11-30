package com.project;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class Main {



    //current state: runHotDataBackupModel, with updates to backup, min-freq

    public static int CACHE_CAPACITY = 300;//DEFAULT SIZE IS 3000
    private static double knapsackWeight = 0.2; // as percentage, DEFAULT SIZE IS 0.02


    public static final int CACHE_NUM = 10;
    private static ArrayList<Integer> unique_al;
    static String base = "/Users/karthik/cache-benchmarks/lirs-trc";
    private static HashMap<Integer, Integer> unique_ht_count;
    private static final long SEED = 0xDEADBEEF;
    private static final int REPLFACTOR = 8;
    private static int wt_max = 50;
    private static int MAX = 10000;
    public static HashMap<Integer, Integer> unique_ht_weight;


    public static void main(String[] args) throws HashGenerationException, IOException {
//        if(args.length < 2){
//            System.out.println("java Main KnapsackWeight CacheSize");
//            System.out.println("java Main 0.02 3000");
//            return;
//        }
//        knapsackWeight=Double.parseDouble(args[0]);
//        CACHE_CAPACITY=Integer.parseInt(args[1]);
        //init caches
        System.out.println("cache size= " + CACHE_CAPACITY + "\n" + "knapsackWeight= " + knapsackWeight + "\n");
        LRUCache[] allCaches = new LRUCache[CACHE_NUM];
        for (int i = 0; i < allCaches.length; i++) {
            allCaches[i] = new LRUCache(CACHE_CAPACITY, "cacheId" + i);
        }
        //create hashRing and insert cache into it
        ConsistentHashing lookupRing = new ConsistentHashing(new HashFunction(), REPLFACTOR, allCaches);
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
        trainCaches("sprite-train.trc", lookupRing);
        //runNaiveModel(lookupRing, allCaches);
        //runNoModel(lookupRing,allCaches);
//        for(LRUCache c:allCaches) {
//            System.out.println(c.cacheId+" "+c.map.size());
//        }
        //runHotDatDistributionModel(lookupRing,allCaches);
        //runHotDataBackupModel(lookupRing, allCaches);
        runHotDataBackupModelWithGlobalKnapsack(lookupRing,allCaches);
    }

    private static void runHotDataBackupModelWithGlobalKnapsack(ConsistentHashing lookupRing, LRUCache[] allCaches) throws IOException, HashGenerationException {
        //back up hot data from every server
        //apply knapsack at every server and store it in the back up server
        //knapsack size as usual
        //also creates a backupServerCache to hold the knapsack items from every cache

        //capacity of knapsack at every cache
        int w = (int) Math.ceil(knapsackWeight * unique_al.size() * 25);
        int n = allCaches[0].map.size() + 200;//provisioning for worst case

        // explicitly provision a back up LRUCache with capacity=CACHE_CAPACITY
        LRUCache masterCache = new LRUCache(CACHE_CAPACITY, "backupServerCache");


        double[][] table = new double[(int) Math.ceil(w) + 10][(int) Math.ceil(n) + 10];
        boolean[][] sol = new boolean[(int) Math.ceil(w) + 10][(int) Math.ceil(n) + 10];
        boolean[] taken = new boolean[(int) Math.ceil(n) + 10];
        for (int i = 0; i < allCaches.length; i++) {
            for (double[] t : table) {
                Arrays.fill(t, 0);
            }
            for (boolean[] t : sol) {
                Arrays.fill(t, false);
            }
            Arrays.fill(taken, false);
            doKnapsack(allCaches[i], w, allCaches[i].map.size(), table, sol, taken);
            //convert taken array indexes to element key hashset. element key is trace numbers.
            allCaches[i].initHotDataMapFromTakenArray(taken);
        }
        mergeAllCachesAndInitCountOrder(masterCache,allCaches);
        //get avh weight of all the caches and use that weight to be the knapsack weight
        //run knapsack on master
        for(int i=0;i<allCaches.length;i++){
            System.out.println(allCaches[i].toString()+" wtSum= "+allCaches[i].getWeightSum());
        }
        w=CACHE_CAPACITY*25;
        n=masterCache.cache_order.size();

        table = new double[(int) Math.ceil(w) + 10][(int) Math.ceil(n) + 10];
        sol = new boolean[(int) Math.ceil(w) + 10][(int) Math.ceil(n) + 10];
        taken = new boolean[(int) Math.ceil(n) + 10];
        for (double[] t : table) {
            Arrays.fill(t, 0);
        }
        for (boolean[] t : sol) {
            Arrays.fill(t, false);
        }
        Arrays.fill(taken, false);
        doKnapsackOnMaster(masterCache,w, n, table, sol, taken);
        masterCache.initHotDataMapFromTakenArray(taken);
        System.out.println("backup server cache size items= "+CACHE_CAPACITY+"; weight capacity in wt= "+w);
        System.out.println("items in backupserver after global knapsack"+masterCache.hotDataSetFromTakenArray.size());
        for (int i : masterCache.hotDataSetFromTakenArray) {
            masterCache.set(i, 0);
        }

        // "random" return a random node to kill
        // "max-freq" returns a cache with highest sum of frequencies
        // "min-freq" returns a cache with lowest sum of frequencies
        String s="random";
        int node_kill_index=getKillNodeIndex(s,allCaches);
        System.out.println(s+" node to be killed = " + node_kill_index);
        //run trace for this model
        testCaches("sprite-test.trc", lookupRing, allCaches[node_kill_index], masterCache);
    }

    private static void mergeAllCachesAndInitCountOrder(LRUCache master, LRUCache[] allCaches){
        //iterate over every cache
        //from each cache get the countOrder map
        int count=1;
        for(int i = 0; i< allCaches.length; i++){
            for(int s:allCaches[i].hotDataSetFromTakenArray){
                master.cache_order.put(count, allCaches[i].map.get(s));
                count++;
            }
        }
    }

    private static void runHotDataBackupModel(ConsistentHashing lookupRing, LRUCache[] allCaches) throws IOException, HashGenerationException {
        //back up hot data from every server
        //apply knapsack at every server and store it in the back up server
        //knapsack size as usual
        //also creates a backupServerCache to hold the knapsack items from every cache

        //capacity of knapsack at every cache
        int w = (int) Math.ceil(knapsackWeight * unique_al.size() * 25);
        int n = allCaches[0].map.size() + 200;//provisioning for worst case

        // explicitly provision a back up LRUCache with capacity=10*w
        int backupServerCapacity = (int) Math.ceil(allCaches.length * w);
        LRUCache backupServerCache = new LRUCache(backupServerCapacity, "backupServerCache");
        System.out.println("backup server cache size= "+backupServerCapacity);
        double[][] table = new double[(int) Math.ceil(w) + 10][(int) Math.ceil(n) + 10];
        boolean[][] sol = new boolean[(int) Math.ceil(w) + 10][(int) Math.ceil(n) + 10];
        boolean[] taken = new boolean[(int) Math.ceil(n) + 10];
        for (int i = 0; i < allCaches.length; i++) {
            for (double[] t : table) {
                Arrays.fill(t, 0);
            }
            for (boolean[] t : sol) {
                Arrays.fill(t, false);
            }
            Arrays.fill(taken, false);
            doKnapsack(allCaches[i], w, allCaches[i].map.size(), table, sol, taken);
            //convert taken array indexes to element key hashset. element key is trace numbers.
            allCaches[i].initHotDataMapFromTakenArray(taken);
            //add the knaspsacked data into the backupServerCache
            for (int s : allCaches[i].hotDataSetFromTakenArray) {
                backupServerCache.set(s, 0);
            }
        }
        // "random" return a random node to kill
        // "max-freq" returns a cache with highest sum of frequencies
        // "min-freq" returns a cache with lowest sum of frequencies
        String s="random";
        int node_kill_index=getKillNodeIndex(s,allCaches);
        System.out.println(s+" node to be killed = " + node_kill_index);
        //run trace for this model
        testCaches("sprite-test.trc", lookupRing, allCaches[node_kill_index], backupServerCache);
    }

    private static void testCaches(String file, ConsistentHashing lookupRing, LRUCache node_killed, LRUCache backupServerCache) throws IOException, HashGenerationException {
        BufferedReader r = new BufferedReader(new FileReader(base + "/" + file));
        String line;
        HashMap<LRUCache, Integer> hitmap = new HashMap<LRUCache, Integer>();
        HashMap<LRUCache, Integer> missmap = new HashMap<LRUCache, Integer>();
        int total_hits = 0;
        int total_miss = 0;
        int backup_hits = 0;
        int backup_miss = 0;
        while ((line = r.readLine()) != null) {
            int a = Integer.parseInt(line);
//            int cacheIndex=a%caches.length;
            LRUCache c = lookupRing.get(a);
            if (c.cacheId.compareTo(node_killed.cacheId) == 0) {
                //cache is killed cache
                //check if item is in backup server
                //if so-hit rate ++, update the backupServerCache
                //if miss, then miss++, update the backupServerCache by adding this item

                if (backupServerCache.map.containsKey(a)) {
                    total_hits++;
                    backup_hits++;

                } else {
                    total_miss++;
                    backup_miss++;

                }
                //remove this to run trace without updating the backupServerCache
                backupServerCache.set(a, 0);
            } else {
                //cache is not killed cache, then track hits and misses and update LRU of each cache
                if (c.map.containsKey(a)) {
                    if (hitmap.containsKey(c)) {
                        hitmap.put(c, hitmap.get(c) + 1);
                    } else {
                        hitmap.put(c, 1);
                    }
                    total_hits++;
                    //since this cache has item which mapped to this cache,
                    // incr hit count
                    // update the LRU of this cache
                    c.set(a, 0);
                } else {
                    if (missmap.containsKey(c)) {
                        missmap.put(c, hitmap.get(c) + 1);
                    } else {
                        missmap.put(c, 1);
                    }
                    total_miss++;
                    //since this cache didnt have an item it should have but doesnt, increment miss count
                    // and add this item to cache
                    c.set(a, 0);
                }
            }
        }
        String str = "";
//        for(int i=0;i<caches.length;i++){
//            //System.out.println("cache #"+i+" hits="+hits[i]+" miss="+miss[i]+" hitrate="+((float)hits[i]/(hits[i]+miss[i]))+" cached items="+caches[i].map.size());
//            str+=String.format("%d,%d,%d,%f,%d\n",i,hits[i],miss[i],((float)hits[i]/(hits[i]+miss[i])),caches[i].map.size());
//        }
        //System.out.println("total hits="+total_hits+" total miss="+total_miss+" total hitrate="+((float)total_hits/(total_hits+total_miss)));
        str += String.format("%d,%d,%f\n", total_hits, total_miss, ((float) total_hits / (total_hits + total_miss)));
        System.out.println();
        System.out.println(str);
        System.out.println("backup-server hit ratio= " + ((float) backup_hits / (backup_hits + backup_miss)));
        System.out.println("backup-server hits= " + backup_hits + " backup-server hits= " + backup_miss);
        r.close();

    }

    private static void runNoModel(ConsistentHashing lookupRing, LRUCache[] allCaches) throws IOException, HashGenerationException {
        //run test trace on this lookup ring
        testCaches("sprite-test.trc", lookupRing);
    }


    public static void assignRandomWeightsTo(ArrayList<Integer> al) {
        unique_ht_weight = new HashMap<Integer, Integer>();
        Random gen = new Random(SEED);
        for (int a : al) {
            unique_ht_weight.put(a, gen.nextInt(wt_max) + 1);
        }
    }


    private static void runHotDatDistributionModel(ConsistentHashing lookupRing, LRUCache[] allCaches) throws HashGenerationException, IOException {

        // "random" return a random node to kill
        // "max-freq" returns a cache with highest sum of frequencies
        // "min-freq" returns a cache with lowest sum of frequencies
        String s="min-freq";
        int node_kill_index=getKillNodeIndex(s,allCaches);
        System.out.println(s+" node to be killed = " + node_kill_index);

        //remove node from lookup ring
        lookupRing.remove(allCaches[node_kill_index]);

        //apply knapsack at the removed node to get the hot data
        doKnapsackOnCache(allCaches[node_kill_index]);
//        System.out.println(allCaches[node_kill_index].hotDataSetFromTakenArray.size()+"\n"+allCaches[node_kill_index].cache_order.size());

//        for(LRUCache c:allCaches){
//            System.out.println(c.cacheId+" "+c.map.size());
//        }


        //for each item in hot data redistribute to other nodes, which we get through the lookupring since
        for (int a : allCaches[node_kill_index].hotDataSetFromTakenArray) {
            //get next node for this item, note we already removed the node from the lookupring
            LRUCache c = lookupRing.get(a);
            //add this new item to the new node. Let the node update itself
            c.set(a, 0);
        }

//        System.out.println("hot data from "+allCaches[node_kill_index].cacheId+" redistributed");
//        for(LRUCache c:allCaches){
//            System.out.println(c.cacheId+" "+c.map.size());
//        }

        //run test trace on this new lookup ring by allowing caches updates
        testCaches("sprite-test.trc", lookupRing);
    }

    private static void doKnapsackOnCache(LRUCache killedCache) {
        //allocate memory


        int w = (int) Math.ceil(knapsackWeight * unique_al.size() * 25);
        int n = killedCache.map.size();
        double[][] table = new double[(int) Math.ceil(w) + 10][(int) Math.ceil(n) + 10];
        boolean[][] sol = new boolean[(int) Math.ceil(w) + 10][(int) Math.ceil(n) + 10];
        boolean[] taken = new boolean[(int) Math.ceil(n) + 10];
        System.out.println("killed cache id " + killedCache.cacheId + " w=" + w + " n=" + n);
        for (double[] t : table) {
            Arrays.fill(t, 0);
        }
        for (boolean[] t : sol) {
            Arrays.fill(t, false);
        }
        Arrays.fill(taken, false);
        doKnapsack(killedCache, w, n, table, sol, taken);
        //convert taken array indexes to element key hashset. element key is trace numbers.
        killedCache.initHotDataMapFromTakenArray(taken);

    }

    public static void doKnapsackOnMaster(LRUCache c, int W, int N, double[][] table, boolean[][] sol, boolean[] taken) {
        //no need for init order on master because mergeAllCachesAndInitCountOrder does that

        for (int i = 0; i <= N; i++) {
            table[0][i] = 0;
        }
        for (int i = 0; i <= W; i++) {
            table[i][0] = 0;
        }
        //System.out.println("1");
        for (int i = 1; i <= W; i++) {
            for (int j = 1; j <= N; j++) {
                double a1 = table[i][j - 1];// without j
                double a2 = Integer.MIN_VALUE;
                if (i >= weight(c, j)) {
                    a2 = table[i - weight(c, j)][j - 1] + value(c, j);//with j
                }
                table[i][j] = Math.max(a1, a2);
                sol[i][j] = a1 < a2;
            }
        }
        //System.out.println("2");
        int w = W;
        for (int j = N; j > 0; j--) {
            if (sol[w][j] == true) {
                taken[j] = true;
                w = w - weight(c, j);
                //System.out.println("w= "+w);
            } else {
                taken[j] = false;
            }
        }

    }

    public static void doKnapsack(LRUCache c, int W, int N, double[][] table, boolean[][] sol, boolean[] taken) {
        //resume here
        c.initCacheOrder(MAX);
        for (int i = 0; i <= N; i++) {
            table[0][i] = 0;
        }
        for (int i = 0; i <= W; i++) {
            table[i][0] = 0;
        }
        //System.out.println("1");
        for (int i = 1; i <= W; i++) {
            for (int j = 1; j <= N; j++) {
                double a1 = table[i][j - 1];// without j
                double a2 = Integer.MIN_VALUE;
                if (i >= weight(c, j)) {
                    a2 = table[i - weight(c, j)][j - 1] + value(c, j);//with j
                }
                table[i][j] = Math.max(a1, a2);
                sol[i][j] = a1 < a2;
            }
        }
        //System.out.println("2");
        int w = W;
        for (int j = N; j > 0; j--) {
            if (sol[w][j] == true) {
                taken[j] = true;
                w = w - weight(c, j);
                //System.out.println("w= "+w);
            } else {
                taken[j] = false;
            }
        }

    }

    public static int weight(LRUCache c, int a) {

        if (c.cache_order.containsKey(a)) {
            int key = c.cache_order.get(a).key;
            if (unique_ht_weight.containsKey(key)) {
                return unique_ht_weight.get(key);
            } else {
                System.out.println("invalid weight for item key " + key);
                return 0;
            }
        } else {
            System.out.println("invalid weight for item key " + a);
            return 0;
        }
    }

    public static double value(LRUCache c, int a) {
        if (c.cache_order.containsKey(a)) {
            return c.cache_order.get(a).value;
        } else {
            System.out.println("invalid value for item key " + a);
            return 0;
        }
    }


    private static void runNaiveModel(ConsistentHashing lookupRing, LRUCache[] allCaches) throws IOException, HashGenerationException {
        // "random" return a random node to kill
        // "max-freq" returns a cache with highest sum of frequencies
        // "min-freq" returns a cache with lowest sum of frequencies
        String s="min-freq";
        int node_kill_index=getKillNodeIndex(s,allCaches);
        System.out.println(s+" node to be killed = " + node_kill_index);
        System.out.println("random node to be killed = " + node_kill_index);
        //remove node from lookup ring
        lookupRing.remove(allCaches[node_kill_index]);
        //run test trace on this lookup ring
        testCaches("sprite-test.trc", lookupRing);
    }

    private static void testCaches(String file, ConsistentHashing lookupRing) throws IOException, HashGenerationException {
        BufferedReader r = new BufferedReader(new FileReader(base + "/" + file));
        String line;
        HashMap<LRUCache, Integer> hitmap = new HashMap<LRUCache, Integer>();
        HashMap<LRUCache, Integer> missmap = new HashMap<LRUCache, Integer>();
        int total_hits = 0;
        int total_miss = 0;
        while ((line = r.readLine()) != null) {
            int a = Integer.parseInt(line);
//            int cacheIndex=a%caches.length;
            LRUCache c = lookupRing.get(a);
            if (c.map.containsKey(a)) {
                if (hitmap.containsKey(c)) {
                    hitmap.put(c, hitmap.get(c) + 1);
                } else {
                    hitmap.put(c, 1);
                }
                total_hits++;
                //since this cache has item which mapped to this cache,
                // incr hit count
                // update the LRU of this cache
                c.set(a, 0);
            } else {
                if (missmap.containsKey(c)) {
                    missmap.put(c, hitmap.get(c) + 1);
                } else {
                    missmap.put(c, 1);
                }
                total_miss++;
                //since this cache didnt have an item it should have but doesnt, increment miss count
                // and add this item to cache
                c.set(a, 0);
            }
        }
        String str = "";
//        for(int i=0;i<caches.length;i++){
//            //System.out.println("cache #"+i+" hits="+hits[i]+" miss="+miss[i]+" hitrate="+((float)hits[i]/(hits[i]+miss[i]))+" cached items="+caches[i].map.size());
//            str+=String.format("%d,%d,%d,%f,%d\n",i,hits[i],miss[i],((float)hits[i]/(hits[i]+miss[i])),caches[i].map.size());
//        }
        //System.out.println("total hits="+total_hits+" total miss="+total_miss+" total hitrate="+((float)total_hits/(total_hits+total_miss)));
        str += String.format("%d,%d,%f\n", total_hits, total_miss, ((float) total_hits / (total_hits + total_miss)));
        System.out.println();
        System.out.println(str);
        r.close();

    }

    public static void trainCaches(String file, ConsistentHashing lookupRing) throws IOException, HashGenerationException {
        BufferedReader r = new BufferedReader(new FileReader(base + "/" + file));
        String line;
        while ((line = r.readLine()) != null) {
            int a = Integer.parseInt(line);
            LRUCache c = lookupRing.get(a);
//            System.out.println("cacheID= "+c.cacheId+" item hash= "+HashFunction.generateMD5(Integer.toString(a)));
            c.set(a, 0);
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
        unique_al = al;
        unique_ht_count = ht;
        r.close();

    }

    public static int getKillNodeIndex(String s, LRUCache[] allCaches) {
//        for(int i=0;i<allCaches.length;i++){
//            System.out.println(allCaches[i].toString()+" freqSum= "+allCaches[i].getFreqSum());
//        }
        int killNodeIndex=Integer.MAX_VALUE;
        int sum=-1;
        switch (s){
            case "random":
                Random rand = new Random(SEED);
                int node_kill_index = rand.nextInt(CACHE_NUM);
                killNodeIndex=node_kill_index;
                break;
            case "max-freq":
                int max=Integer.MIN_VALUE;
                // get cache index which maximum sum of frequencies
                for(int i=0;i<allCaches.length;i++){
                    if((sum=allCaches[i].getFreqSum())>max){
                        max=sum;
                        killNodeIndex=i;
                    }
                }
                break;
            case "min-freq":
                int min=Integer.MAX_VALUE;
                for(int i=0;i<allCaches.length;i++){
                    if((sum=allCaches[i].getFreqSum())< min){
                        min=sum;
                        killNodeIndex=i;
                    }
                }
                break;
            default:
                // default case is "random"
                killNodeIndex=getKillNodeIndex("random",allCaches);
                break;
        }
        return killNodeIndex;
    }
}
