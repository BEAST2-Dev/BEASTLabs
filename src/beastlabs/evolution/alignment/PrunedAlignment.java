package beastlabs.evolution.alignment;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.alignment.Sequence;
import beast.base.evolution.alignment.TaxonSet;
import beast.base.evolution.datatype.DataType;

import java.util.ArrayList;
import java.util.List;

@Description("Sub-alignment generated by pruning some taxa and/or some sites. Can automatically detect taxa " +
        "with non-informative data (all sites are ambiguous).")
public class PrunedAlignment extends Alignment {
    public Input<Alignment> m_srcAlignment = new Input<>("source", "alignment to prune from", Validate.REQUIRED);
    public Input<TaxonSet> taxonSetInput = new Input<>("prunedTaxa",
           "taxa to prune (defaults to all non-informative taxa, i.e. data is all '???')");
    public Input<List<Integer>> m_sites = new Input<>("sites", "sites to retain (default all).", new ArrayList<Integer>());

  public PrunedAlignment() {}

  @Override
  public void initAndValidate() {
      final Alignment source = m_srcAlignment.get();

      final DataType.Base udp = source.userDataTypeInput.get();
      if( udp != null ) {
          userDataTypeInput.setValue(udp, this);
      } else {
          dataTypeInput.setValue(source.dataTypeInput.get(), this);
      }
      stateCountInput.setValue(source.stateCountInput.get(), this);

      List<Integer> sites = m_sites.get();

      List<Sequence> sourceSeqs = source.sequenceInput.get();
      final TaxonSet taxonSet = taxonSetInput.get();

      if( sourceSeqs == null || sourceSeqs.size() == 0 ) {
          // This is truly ugly: alignment object like AlignmentFromTrait don't have sequences, and construct
          // the internals directly. We follow suit here.

          m_dataType = source.getDataType();
          counts = new ArrayList<>();
          stateCounts = new ArrayList<>();

          final List<String> srcTaxa = source.getTaxaNames();
          final List<List<Integer>> srcCounts = source.getCounts();
          if( taxonSet != null ) {
             for(int i = 0; i < source.getTaxaNames().size(); ++i) {

                 if( taxonSet.getTaxonIndex(srcTaxa.get(i)) < 0  ) {
                    counts.add(srcCounts.get(i));
                    stateCounts.add(source.getStateCounts().get(i));
                    taxaNames.add(srcTaxa.get(i));
                }
             }
          } else {
              for(int i = 0; i < source.getStateCounts().size(); ++i) {
                  final List<Integer> c = srcCounts.get(i);
                  for( Integer nc : c ) {
                      //assert( c.size() == 1 );
                      //final Integer nc = c.get(0);
                      if( m_dataType.getStatesForCode(nc).length != m_dataType.getStateCount() ) {
                          counts.add(c);
                          stateCounts.add(source.getStateCounts().get(i));
                          taxaNames.add(srcTaxa.get(i));
                          break;
                      }
                  }
              }
          }

          if( sites.size() > 0 ) {
              for(int i = 0; i < taxaNames.size(); ++i) {
                  List<Integer> newCounts = new ArrayList<>();
                  List<Integer> c = counts.get(i);
                  for( int s : sites ) {
                      newCounts.add( c.get(s) );
                  }
                  counts.set(i, newCounts);
              }
          }

          calcPatterns();
          setupAscertainment();
          return;
      }

      if( sites.size() > 0  ) {
          final int nSites = source.getSiteCount();
          sites = new ArrayList<Integer>(nSites);
          for (int k = 0; k < nSites; ++k) {
              sites.add(k);
          }
      }

      List<Sequence> seqs = new ArrayList<>();

      if( taxonSet != null ) {
          for (Sequence seq :  sourceSeqs ) {
            if( taxonSet.getTaxonIndex(seq.taxonInput.get()) < 0) {
              seqs.add(seq);
            }
         }
      } else {
          for (Sequence seq : sourceSeqs) {
              List<Integer> states = seq.getSequence(source.getDataType());
              final int sn = source.getDataType().getStateCount();
              boolean hasData = false;
              for (int i : sites) {
                  if( states.get(i) >= 0 && states.get(i) < sn ) {
                      hasData = true;
                      break;
                  }
              }
              if( hasData ) {
                  seqs.add(seq);
              }
          }
      }

      if( m_sites.get() != null ) {
          for(int k = 0; k < seqs.size(); ++k ) {
              Sequence seq = seqs.get(k);
              List<Integer> states = seq.getSequence(source.getDataType());
              StringBuilder s = new StringBuilder();
              for (int i : sites) {
                  s.append(states.get(i));
                  s.append(',');
              }
              s.deleteCharAt(s.length() - 1);
              seqs.set(k, new Sequence(seq.taxonInput.get(), s.toString()));
          }
      }
      setInputValue("sequence", seqs);
      super.initAndValidate();
  }
}
