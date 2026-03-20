package beastlabs.evolution.operators;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: joseph
 * Date: 17/09/15
 */
public interface DistanceProvider {
    // Return a mapping from taxon name to its associated data for all taxa
    Map<String,Data> init(Set<String> taxa);

    // return a new 'empty' entry
    Data empty();

    // clear an existing data entry
    void clear(Data d);

    // combine 'info' and 'with' and store the result back to 'info'
    void update(Data info, Data with);

    // distance between two summaries
    double dist(Data info1, Data info2);

    // Data associated with tip or node
    interface Data {}

    DistanceProvider uniform = new DistanceProvider() {
        @Override
        public Map<String, Data> init(Set<String> taxa) {
            HashMap<String, Data> m = new HashMap<String, Data>();
            for( String s : taxa ) {
                m.put(s, empty());
            }
            return m;
        }

        class Data1 implements Data {};
        @Override
        public Data empty() {
            return new Data1();
        }

        @Override
        public void clear(Data d) {}

        @Override
        public void update(Data info, Data with) {}

        @Override
        public double dist(Data info1, Data info2) {
            return 1;
        }
    };
}
