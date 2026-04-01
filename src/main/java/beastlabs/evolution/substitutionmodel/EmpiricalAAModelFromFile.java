package beastlabs.evolution.substitutionmodel;


import java.io.File;
import java.io.IOException;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.datatype.Aminoacid;
import beast.base.evolution.datatype.DataType;
import beast.base.evolution.substitutionmodel.EmpiricalSubstitutionModel;
import beastfx.app.inputeditor.BeautiDoc;

@Description("A reversible amino acid substitution model with rates and frequencies stored in file")
public class EmpiricalAAModelFromFile extends EmpiricalSubstitutionModel {

	final public Input<File> matrixDirInput = new Input<>("matrixDir", "director containing files with "
			+ "empirical rate matrix and empirical frequencies.");
	final public Input<String> rateFileInput = new Input<>("rateFile", "name of comma separated file containing "
			+ "empirical rate matrix -- can contain new lines, but not comments.", "rates.csv");
	final public Input<String> freqFileInput = new Input<>("freqFile", "name of comma separated file containing "
			+ "empirical frequencies", "freqs.csv");
	final public Input<String> encodinInput = new Input<>("encoding", "order im which the amino acids appear in file", "ARNDCQEGHILKMFPSTWYV");
	

    private double[][] ratesFromFile;	
    private double[] f; 

    @Override
    public void initAndValidate() {
    	// sanity check
    	if (encodinInput.get().length() != 20) {
			throw new IllegalArgumentException("encoding should contain 20 characters, but got " + encodinInput.get().length());
    	}
    	
    	// read in information from file 
    	try {
    		ratesFromFile = new double[20][20];
    		f = new double[20];
    		
    		// parse rate matrix
    		String str = BeautiDoc.load(matrixDirInput.get().getPath() + "/" + rateFileInput.get());
    		String [] strs = str.split(",");
			int k = 0;
    		if (strs.length == 400 || strs.length == 380) {
    			for (int i = 0; i < 20; i++) {
    				for (int j = 0; j < 20; j++) {
    					if (strs.length == 380 && i != j) {
    						ratesFromFile[i][j] = Double.parseDouble(strs[k].trim());
    						k++;
    					}
    				}
    			}
    			
    			// sanity check
    			// matrix should be symmetric or contain only zeros in lower triangle
    			for (int i = 0; i < 20; i++) {
    				for (int j = i+1; j < 20; j++) {
    					if (Math.abs(ratesFromFile[i][j] - ratesFromFile[j][i]) > 1e-6 && ratesFromFile[j][i] != 0) {
    						char from = encodinInput.get().charAt(i);
    						char to = encodinInput.get().charAt(j);
    		    			throw new IllegalArgumentException("The matrix is asymetric:\n"
    		    					+ "rates[" + from +"][" + to + "]=" + ratesFromFile[i][j] + "\n"    						
    		    				    + "rates[" + to +"][" + from + "]=" + ratesFromFile[j][i] + "\n");    						
    					}
    				}
    			}
    			
    		} else if (strs.length == 190) {
    			for (int i = 0; i < 20; i++) {
    				for (int j = i+1; j < 20; j++) {
    					ratesFromFile[i][j] = Double.parseDouble(strs[k].trim());
    					k++;
    				}
    			}
    		} else {
    			throw new IllegalArgumentException("expected 190, 380 or 400 entries in rates file, but found " + strs.length);
    		}
    		
    		// parse frequencies
    		str = BeautiDoc.load(matrixDirInput.get().getPath() + "/" + freqFileInput.get());
    		strs = str.split(",");
    		if (strs.length == 20) {
    			for (int i = 0; i < 20; i++) {
    				f[i] = Double.parseDouble(strs[i].trim());
    			}
    		} else {
    			throw new IllegalArgumentException("expected 20 entries in frequencies file, but found " + strs.length);
    		}
    		
    	} catch (IOException e) {
			throw new IllegalArgumentException("Problem reading rate or frequency file: " + e.getMessage());
		}
    	
    	super.initAndValidate();
    }
    
    
	
    @Override
	public
    double[][] getEmpiricalRates() {
    	return ratesFromFile;
    }

    @Override
    public double[] getEmpiricalFrequencies() {
        return f;
    }

    @Override
    public int[] getEncodingOrder() {
        Aminoacid dataType = new Aminoacid();
        String codeMap = dataType.getCodeMap();
        int[] codeMapNrs = new int[dataType.getStateCount()];
        String encoding = encodinInput.get();
        for (int i = 0; i < dataType.getStateCount(); i++) {
            codeMapNrs[i] = encoding.indexOf(codeMap.charAt(i));
        }
        return codeMapNrs;
    }

    @Override
    public boolean canHandleDataType(DataType dataType) {
        return dataType instanceof Aminoacid;
    }
    
} // class EmpiricalFromFile
