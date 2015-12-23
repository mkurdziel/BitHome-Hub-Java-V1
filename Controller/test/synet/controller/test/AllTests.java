package synet.controller.test;

import synet.controller.test.parameters.AllParametersTest;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests
{

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AllTests.class.getName());
        //$JUnit-BEGIN$
        suite.addTestSuite(ControllerNodeDiscoveryTest.class);
        suite.addTest(AllParametersTest.suite());
        //$JUnit-END$
        return suite;
    }

}
