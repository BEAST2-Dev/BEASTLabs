package beast.beast1;

import beast.core.Citation;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Plugin;


@Description("")
@Citation("Created by Walter Xie")
public class Expectation extends Plugin {

    public Input<String> m_Name = new Input<String>("name", "The name of a loggable plugin", Validate.REQUIRED);

    public Input<Double> m_ExpValue =
            new Input<Double>("expectedValue", "The expected value of the referred loggable plugin", Validate.REQUIRED);

    public Input<Double> m_StandErrorOfMean =
            new Input<Double>("error", "The expected standard error of mean. If not given, it will estimate error from log",
                    0.0, Validate.OPTIONAL);

}
