package beast.evolution.taxonomy;


import beast.base.core.Description;


@Description("For identifying a single taxon, modified to be comparable")
public class Taxon extends beast.base.evolution.alignment.Taxon implements Comparable<Taxon> {

    public Taxon(String id) throws Exception {
        super(id);
    }

    public String toString() {
        return getID();
    }

    @Override
    public int compareTo(Taxon taxon) {
        return toString().compareTo(taxon.toString());
    }
}
