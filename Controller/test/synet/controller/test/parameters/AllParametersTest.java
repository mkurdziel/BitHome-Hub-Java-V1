package synet.controller.test.parameters;

import synet.controller.actions.ActionParameter;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllParametersTest
{

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AllParametersTest.class.getName());
        //$JUnit-BEGIN$
        suite.addTestSuite(ActionParameterTest.class);
        suite.addTestSuite(NodeParameterTest.class);
        suite.addTestSuite(ParameterBaseTest.class);
        //$JUnit-END$
        return suite;
    }

}
