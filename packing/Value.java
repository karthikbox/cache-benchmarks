public interface Value{
    public double get_value(long max, int count, int ref);
}

class LfuLru implements Value{
    public double get_value(long max,int count,int ref){
	return (max-count)*ref;
    }
}

class LfuLruInverse implements Value{
    public double get_value(long max,int count,int ref){
	return ((double)max/count)*ref;
    }
}

class LruIndex implements Value{
    public double get_value(long max,int count,int ref){
	return (max-count);
    }
}

class LruIndexInverse implements Value{
    public double get_value(long max,int count,int ref){
	return ((double)max/count);
    }
}
