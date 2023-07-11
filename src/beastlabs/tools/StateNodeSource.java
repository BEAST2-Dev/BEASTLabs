package beastlabs.tools;

// typically contains all information from a file to inform a statenode, like a trace or tree log
public interface StateNodeSource {
	
	// initialise state nodes in the entry with the i-th trace value
	public void initStateNodes(int i);		
}
