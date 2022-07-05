package beastlabs.evolution.operators;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.Operator;


@Description("Operator with a flexible kernel distribution")
public class KernelOperator extends Operator {
    public final Input<KernelDistribution> kernelDistributionInput = new Input<>("kernelDistribution", "provides sample distribution for proposals", 
    		KernelDistribution.newDefaultKernelDistribution());

    protected KernelDistribution kernelDistribution;

	@Override
	public void initAndValidate() {
    	kernelDistribution = kernelDistributionInput.get();
	}

	@Override
	public double proposal() {
		// TODO Auto-generated method stub
		return 0;
	}

}
