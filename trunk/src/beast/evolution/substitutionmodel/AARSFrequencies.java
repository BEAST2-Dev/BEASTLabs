package beast.evolution.substitutionmodel;

import beast.core.Description;

@Description("Helper for AARSSubstitutionModel to handle frequent frequency changes")
public class AARSFrequencies extends Frequencies {
    public void setFreqs(double [] fFreqs) {
    	m_fFreqs = fFreqs;
    	m_bNeedsUpdate = false;
    }

}
