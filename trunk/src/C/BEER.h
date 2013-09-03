#include <stdio.h>
class BEER {
	bool m_bUseScaling;

protected:
	/** various counts **/
	int m_nStates;
    int m_nNodes;
    int m_nPatterns;
    int m_nPartialsSize;
    int m_nMatrixSize;
    int m_nMatrixCount;

    /** flag to indicate whether to integrate over site categories (as defined by the SiteModel) */
    bool m_bIntegrateCategories;

    double*** m_fPartials; // 2 x #nodes x (#patterns*#states*#matrices)

    int** m_iStates; // #nodes x #patterns

    double*** m_fMatrices; // 2 x #nodes x matrix size

    int* m_iCurrentMatrices; // # nodes
    int* m_iStoredMatrices;  // # nodes
    int* m_iCurrentPartials; // # nodes
    int* m_iStoredPartials;  // # nodes

    // used to store/restore state
	int * m_iCurrentStates;
	int * m_iStoredStates;

	/** one number to scale them all */
	double SCALE;
//    protected double*** m_fScalingFactors; // 2 x #nodes x #patterns



    
    int** m_nNrOfID; // 2 x #nodes
    int*** m_nID; // 2 x #nodes x #patterns
    /** contains [0,1,2,...#pattenrs-1], used as default m_nID content **/
    int * DEFAULT_ID; 
    /** memory for building up cache index **/
    int * DEFAULT_MAP; 


	// stack related variables
	#define OPERATION_SS  0
	#define OPERATION_SP  1
	#define OPERATION_PP  2
	int m_nTopOfStack;
	int * m_nOperation; // #nodes
	int * m_nNode1;     // #nodes
	int * m_nNode2;     // #nodes
	int * m_nNode3;     // #nodes
	int *** m_nStates1; // 2 x #nodes x #patterns
	int *** m_nStates2; // 2 x #nodes x #patterns
	int ** m_nStackStates1; // #nodes x * pointer to m_nStates, or DEFAULT_ID
	int ** m_nStackStates2; // #nodes x * pointer to m_nStates, or DEFAULT_ID
protected:
	BEER() {};
public:
	BEER(int nStateCount) {
		fprintf(stderr,"Creating BEER here with state count=%d\n", nStateCount);
		m_nStates = nStateCount;
		m_bUseScaling = false;
    	SCALE = 1.05;
    	m_nTopOfStack = 0;
    } // c'tor



	/** reserve memory for partials, indices and other 
	 * data structures required by the core **/
	bool initialize(int nNodeCount, int nPatternCount, int nMatrixCount, bool bIntegrateCategories);
	
	/** clean up after last likelihood calculation, if at all required **/
	void finalize();

	/** reserve memory for partials for node with number iNode **/
	void createNodePartials(int iNode);
	
	
	/** indicate that the partials for node 
	 * iNode is about the be changed, that is, that the stored
	 * state for node iNode cannot be reused **/
	void setNodePartialsForUpdate(int iNode);
	/** assign values of partials for node with number iNode **/
	// do we need these???
	//void setNodePartials(int iNode, double* fPartials);
    //void setCurrentNodePartials(int iNode, double* fPartials);

    /** reserve memory for states for node with number iNode **/
	void createNodeStates(int iNode);

	/** assign values of states for node with number iNode **/
	void setNodeStates(int iNode, int* iStates);
	
	/** indicate that the probability transition matrix for node 
	 * iNode is about the be changed, that is, that the stored
	 * state for node iNode cannot be reused **/
	void setNodeMatrixForUpdate(int iNode);
	
    /** assign values of states for probability transition matrix for node with number iNode **/
	void setNodeMatrix(int iNode, int iMatrixIndex, double* fMatrix);
    void setPaddedMatrices(int iNode, double * fMatrix);

    
    /** indicate that the topology of the tree chanced so the cache 
	 * data structures cannot be reused **/
    void setNodeStatesForUpdate(int iNode);
    

    
	/** flag to indicate whether scaling should be used in the
	 * likelihood calculation. Scaling can help in dealing with
	 * numeric issues (underflow).
	 */
	void setUseScaling(double fScale);
	bool getUseScaling() {return m_bUseScaling;}
	/** return the cumulative scaling effect. Should be zero if no scaling is used **/
    double getLogScalingFactor(int iPattern);

    /** Calculate partials for node iNode3, with children iNode1 and iNode2. 
     * NB Depending on whether the child nodes contain states or partials, the
     * calculation differs-**/
    void calculatePartials(int iNode1, int iNode2, int iNode3);
    //void calculatePartials(int iNode1, int iNode2, int iNode3, int* iMatrixMap);
    /** integrate partials over categories (if any). **/
    void integratePartials(int iNode, double* fProportions, double* fOutPartials);

    /** calculate log likelihoods at the root of the tree,
     * using fFrequencies as root node distribution.
     * fOutLogLikelihoods contains the resulting probabilities for each of 
     * the patterns **/
	//void calculateLogLikelihoods(double* fPartials, double* fFrequencies, double* fOutLogLikelihoods);
	
    
    
    /** store current state **/
    void store();
    /** reset current state to stored state, only used when switching from non-scaled to scaled or vice versa **/
    void unstore();
    /** restore state **/
    void restore();


private:
	void calcAllMatrixSSP(int nNrOfID, int * pStates1, int * pStates2, double * pfMatrices1, double * pfMatrices2, double * pfPartials3);
	void calcAllMatrixSPP(int nNrOfID, int * pStates1, int * pStates2, double * pfMatrices1, double * pfMatrices2, double * pfPartials2, double * pfPartials3);
	void calcAllMatrixSPP2(int * pStates1, double * pfMatrices1, double * pfMatrices2, double * pfPartials2, double * pfPartials3);
	void calcAllMatrixPPP(int nNrOfID, int * pStates1, int * pStates2, double * pfMatrices1, double * pfPartials1, double * pfMatrices2, double * pfPartials2, double * pfPartials3);
	void calcAllMatrixPPP2(double * pfMatrices1, double * pfPartials1, double * pfMatrices2, double * pfPartials2, double * pfPartials3);

	void calcSSP(int state1, int state2, double * pfMatrices1, double * pfMatrices2, double * pfPartials3, int w, int v);
	void calcSPP(int state1, double * pfMatrices1, double * pfMatrices2, double * pfPartials2, double * pfPartials3, int w, int v, int u);
	void calcPPP(double * pfMatrices1, double * pfPartials1, double * pfMatrices2, double * pfPartials2, double * pfPartials3, int w, int v1, int v2, int u);
	void calcPPP2(double * pfMatrices1, double * pfPartials1, double * pfMatrices2, double * pfPartials2, double * pfPartials3, int w, int v, int u);

	int * initIDMap(int iNode, int nMaxID1, int nMaxID2);
	void calculateStatesStatesPruning(int iNode1, int iNode2, int iNode3);
	void calculateStatesPartialsPruning(int iNode1, int iNode2, int iNode3);
	void calculatePartialsPartialsPruning(int iNode1, int iNode2, int iNode3);
	int calcPPPInner(int * nID1, int * nID2, int * nID3, int nBase, int * nIDMap, int * pStates1, int * pStates2, int iNode3);
	void processNodeFromStack(int iJob);

    void scalePartials(int iNode);
private:
    inline void arraycopy(double * src, int nSrcOffset, double * dest, int nDestOffset, int nLength);
    inline void arraycopy(int * src, int nSrcOffset, int * dest, int nDestOffset, int nLength);
    inline void arraycopy(double * src, double * dest, int nLength);
    inline void arraycopy(int * src, int * dest, int nLength);
    int * newint(int nSize);
    double * newdouble(int nSize);
}; // class BEER



class BEER4 : public BEER {
public:
	BEER4() : BEER(4) {fprintf(stderr,"NUC'EM! ");}
	void calcSSP(int state1, int state2, double * pfMatrices1, double * pfMatrices2, double * pfPartials3, int w, int v);
	void calcSPP(int state1, double * pfMatrices1, double * pfMatrices2, double * pfPartials2, double * pfPartials3, int w, int v, int u);
	void calcPPP(double * pfMatrices1, double * pfPartials1, double * pfMatrices2, double * pfPartials2, double * pfPartials3, int w, int v1, int v2, int u);
	void calcPPP2(double * pfMatrices1, double * pfPartials1, double * pfMatrices2, double * pfPartials2, double * pfPartials3, int w, int v, int u);
};
