package segeraroot.performancecounter;

public interface PCSink {
    void dump(String name, Aggregator aggregator);
}
