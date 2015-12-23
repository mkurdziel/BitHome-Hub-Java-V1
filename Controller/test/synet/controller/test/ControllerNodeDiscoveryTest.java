/**
 * 
 */
package synet.controller.test;

import junit.framework.TestCase;

import nu.xom.Element;

import org.junit.Test;

import synet.controller.Controller;
import synet.controller.NodeManager;
import synet.controller.Protocol.EsnDataTypes;
import synet.controller.actions.INodeAction;
import synet.controller.actions.INodeParameter;
import synet.controller.actions.NodeParameter;
import synet.controller.messaging.messages.MsgCatalogResponse;
import synet.controller.messaging.messages.MsgConstants;
import synet.controller.messaging.messages.MsgDeviceStatusResponse;
import synet.controller.messaging.messages.MsgConstants.EsnAPIDeviceStatusValue;
import synet.controller.messaging.messages.MsgDeviceStatusRequest.EsnRequestType;
import synet.controller.messaging.messages.MsgParameterResponse;
import synet.controller.nodes.NodeBase.EsnInvestigationStatusEnum;
import synet.controller.nodes.NodeBase.EsnStatusEnum;
import synet.controller.utils.XmlUtils;


/**
 * @author kur57360
 *
 */
public class ControllerNodeDiscoveryTest extends TestCase
{
    private static final int C_MESSAGE_TIMEOUT = 5000;
    private static final String C_STR_TESTFILESDIR = "test/testFiles/";
    private static final String C_STR_TESTFILE1 = C_STR_TESTFILESDIR + "ControllerNodeDiscoveryTestNode1.xml";
    private static final String C_STR_TESTFILE2 = C_STR_TESTFILESDIR + "ControllerNodeDiscoveryTestNode2.xml";

    /**
     * Test a normal investigation operating properly 
     * 
     * @throws InterruptedException 
     */
    @Test
    public void testSuccessfulInvestigation() throws InterruptedException
    {
        Controller c;
        TestMsgAdapter testAdapter;
        c = new Controller(true, "", "9000");
        testAdapter = new TestMsgAdapter();
        testAdapter.ignoreMsgType(MsgConstants.SN_API_SYSTEM_NODELIST_TRANSMIT);
        testAdapter.ignoreMsgType(MsgConstants.SN_API_SYSTEM_CATALOG_TRANSMIT);
        c.addMsgAdapter(testAdapter);
        c.setNetAdapterEnable(false);
        c.setXbeeAdapterEnable(false);

        // Import the element from the test xml
        Element nodeElement = XmlUtils.readXML(C_STR_TESTFILE1).getRootElement();
        TestNode refNode = new TestNode(nodeElement);

        TestNode node = new TestNode(refNode.getNodeId());

        NodeManager nm = NodeManager.getInstance();

        nm.addNewNode(node.getNodeId(), node);

        nm.setIsPeriodicCheck(false);

        Thread cThread = new ControllerThread(c);
        cThread.start();

        // Wait for the initial query
        assertTrue(testAdapter.waitForMsgDeviceStatusRequest(C_MESSAGE_TIMEOUT, EsnRequestType.INFORMATION));

        // Verify the node state
        assertEquals( EsnInvestigationStatusEnum.UNKNOWN, node.getInvestigationStatus());
        assertEquals( EsnStatusEnum.UNKNOWN, node.getNodeStatus());

        // Send back the initial status information
        MsgDeviceStatusResponse msgStatusResponse = 
            new MsgDeviceStatusResponse(node, null, 
                    EsnAPIDeviceStatusValue.INFO, 
                    refNode.getSynetId(), refNode.getManufacturerId(), 
                    refNode.getProfile(), refNode.getRevision());
        testAdapter.receiveMessage(msgStatusResponse);

        assertTrue(testAdapter.waitForCatalogRequest(C_MESSAGE_TIMEOUT, 0));

        // Verify the node state
        assertEquals( EsnInvestigationStatusEnum.INFO, node.getInvestigationStatus());
        assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());

        int numFunctions = refNode.getNumTotalFunctions();

        // Send back the initial catalog response
        MsgCatalogResponse msgCatalogResponse = 
            // total entries, entry number, num params, function name, param type
            new MsgCatalogResponse(node, null, numFunctions,
                    0,0,EsnDataTypes.VOID,null,null); 
        testAdapter.receiveMessage(msgCatalogResponse);

        // Check to see if all the function requests are received
        for (int i=1; i<=numFunctions; ++i)
        {
            assertTrue(testAdapter.waitForCatalogRequest(C_MESSAGE_TIMEOUT, i));

            // Verify the node state
            assertEquals( EsnInvestigationStatusEnum.FUNCTION, node.getInvestigationStatus());
            assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());
            INodeAction action = refNode.getNodeAction(i);

            sendCatalogResponse(testAdapter, node, action, numFunctions);
        }

        // Check to see if all the parameter requests are received
        for (int i=1; i<=numFunctions; ++i)
        {
            INodeAction action = refNode.getNodeAction(i);
            for (int p=1; p<=action.getNumParameters(); ++p)
            {
                INodeParameter param = action.getParameter(p);
                assertTrue(testAdapter.waitForParameterRequest(C_MESSAGE_TIMEOUT, i, p));

                // Verify the node state
                assertEquals( EsnInvestigationStatusEnum.PARAMETER, node.getInvestigationStatus());
                assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());

                sendParameterResponse(testAdapter, node, param);
            }
        }

        // Give a chance for the last message to get picked up by the thread
        Thread.sleep(100);
        
        assertTrue(refNode.isEqualTo(node, false));

        // Verify the node state
        assertEquals( EsnInvestigationStatusEnum.COMPLETED, node.getInvestigationStatus());
        assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());

        cleanUp(c, cThread);
    }

    /**
     * Test an investigation where the node sends repeat responses
     * 
     * @throws InterruptedException 
     */
    @Test
    public void testSuccessfulInvestigationWithRepeats() throws InterruptedException
    {
        final Controller c = new Controller(true, "", "9000");
        TestMsgAdapter testAdapter = new TestMsgAdapter();
        testAdapter.ignoreMsgType(MsgConstants.SN_API_SYSTEM_NODELIST_TRANSMIT);
        testAdapter.ignoreMsgType(MsgConstants.SN_API_SYSTEM_CATALOG_TRANSMIT);
        c.addMsgAdapter(testAdapter);
        c.setNetAdapterEnable(false);
        c.setXbeeAdapterEnable(false);

        // Import the element from the test xml
        Element nodeElement = XmlUtils.readXML(C_STR_TESTFILE1).getRootElement();
        TestNode refNode = new TestNode(nodeElement);

        TestNode node = new TestNode(refNode.getNodeId());

        NodeManager nm = NodeManager.getInstance();

        nm.addNewNode(node.getNodeId(), node);

        nm.setIsPeriodicCheck(false);

        Thread cThread = new ControllerThread(c);
        cThread.start();

        // Wait for the initial query
        assertTrue(testAdapter.waitForMsgDeviceStatusRequest(C_MESSAGE_TIMEOUT, EsnRequestType.INFORMATION));

        // Verify the node state
        assertEquals( EsnInvestigationStatusEnum.UNKNOWN, node.getInvestigationStatus());
        assertEquals( EsnStatusEnum.UNKNOWN, node.getNodeStatus());

        // Send back the initial status information
        MsgDeviceStatusResponse msgStatusResponse = 
            new MsgDeviceStatusResponse(node, null, 
                    EsnAPIDeviceStatusValue.INFO, 
                    refNode.getSynetId(), refNode.getManufacturerId(), 
                    refNode.getProfile(), refNode.getRevision());
        testAdapter.receiveMessage(msgStatusResponse);

        assertTrue(testAdapter.waitForCatalogRequest(C_MESSAGE_TIMEOUT, 0));

        // Verify the node state
        assertEquals( EsnInvestigationStatusEnum.INFO, node.getInvestigationStatus());
        assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());

        int numFunctions = refNode.getNumTotalFunctions();

        // Send back the initial catalog response
        MsgCatalogResponse msgCatalogResponse = 
            // total entries, entry number, num params, function name, param type
            new MsgCatalogResponse(node, null, numFunctions,
                    0,0,EsnDataTypes.VOID,null,null); 
        testAdapter.receiveMessage(msgCatalogResponse);

        // Check to see if all the function requests are received
        for (int i=1; i<=numFunctions; ++i)
        {
            int requestInt = i;

            for (int numRetries = 0; numRetries < 3; ++numRetries)
            {
                assertTrue(testAdapter.waitForCatalogRequest(C_MESSAGE_TIMEOUT, requestInt));

                // Verify the node state
                assertEquals( EsnInvestigationStatusEnum.FUNCTION, node.getInvestigationStatus());
                assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());
                INodeAction action = refNode.getNodeAction(i);

                sendCatalogResponse(testAdapter, node, action, numFunctions);

                // After the first attempt, the request will be for the next
                if (requestInt == i) requestInt++;

                if (i == numFunctions) break;
            }
        }

        // Check to see if all the parameter requests are received
        for (int i=1; i<=numFunctions; ++i)
        {
            INodeAction action = refNode.getNodeAction(i);


            for (int p=1; p<=action.getNumParameters(); ++p)
            {
                int requestInt = p;

                for (int numRetries = 0; numRetries < 3; ++numRetries)
                {
                    INodeParameter param = action.getParameter(p);
                    assertTrue(testAdapter.waitForParameterRequest(C_MESSAGE_TIMEOUT, i, requestInt));

                    // Verify the node state
                    assertEquals( EsnInvestigationStatusEnum.PARAMETER, node.getInvestigationStatus());
                    assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());

                    sendParameterResponse(testAdapter, node, param);
                    
                    // After the first attempt, the request will be for the next
                    if (requestInt == p) requestInt++;

                    if (p == action.getNumParameters()) break;
                }
            }
        }

        // Give a chance for the last message to get picked up by the thread
        Thread.sleep(200);
        
        assertTrue(refNode.isEqualTo(node, false));

        // Verify the node state
        assertEquals( EsnInvestigationStatusEnum.COMPLETED, node.getInvestigationStatus());
        assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());

        cleanUp(c, cThread);
    }
    
    /**
     * Test that when investigating, the node manager will retransmit attempts
     * and then give up after a set number of retries
     * 
     * @throws InterruptedException
     */
    @Test
    public void testInvestigationRetransmits() throws InterruptedException
    {
        Controller c;
        TestMsgAdapter testAdapter;
        c = new Controller(true, "", "9000");
        testAdapter = new TestMsgAdapter();
        testAdapter.ignoreMsgType(MsgConstants.SN_API_SYSTEM_NODELIST_TRANSMIT);
        testAdapter.ignoreMsgType(MsgConstants.SN_API_SYSTEM_CATALOG_TRANSMIT);
        c.addMsgAdapter(testAdapter);
        c.setNetAdapterEnable(false);
        c.setXbeeAdapterEnable(false);

        // Import the element from the test xml
        Element nodeElement = XmlUtils.readXML(C_STR_TESTFILE1).getRootElement();
        TestNode refNode = new TestNode(nodeElement);

        TestNode node = new TestNode(refNode.getNodeId());

        NodeManager nm = NodeManager.getInstance();

        nm.addNewNode(node.getNodeId(), node);

        Thread cThread = new ControllerThread(c);
        cThread.start();

        // Wait for the initial query
        assertTrue(testAdapter.waitForMsgDeviceStatusRequest(C_MESSAGE_TIMEOUT, EsnRequestType.INFORMATION));
        
        assertEquals( NodeManager.C_QUERY_INTERVAL_MS, nm.getQueryInterval() );

        // Verify the node state
        assertEquals( EsnInvestigationStatusEnum.UNKNOWN, node.getInvestigationStatus());
        assertEquals( EsnStatusEnum.UNKNOWN, node.getNodeStatus());

        // Send back the initial status information
        MsgDeviceStatusResponse msgStatusResponse = 
            new MsgDeviceStatusResponse(node, null, 
                    EsnAPIDeviceStatusValue.INFO, 
                    refNode.getSynetId(), refNode.getManufacturerId(), 
                    refNode.getProfile(), refNode.getRevision());
        testAdapter.receiveMessage(msgStatusResponse);

        assertTrue(testAdapter.waitForCatalogRequest(C_MESSAGE_TIMEOUT, 0));
            
        for (int i=1; i<NodeManager.C_INVESTIGATE_RETRIES; ++i)
        {
            assertEquals( NodeManager.C_INVESTIGATE_INTERVAL_MS, nm.getQueryInterval() );
            
            // Verify the node state
            assertEquals( EsnInvestigationStatusEnum.INFO, node.getInvestigationStatus());
            assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());
            
            assertTrue(testAdapter.waitForCatalogRequest(
                    NodeManager.C_INVESTIGATE_TIMEOUT + NodeManager.C_INVESTIGATE_INTERVAL_MS, 0));
        }
        
        // Make sure we gave up after the set number of retries
        Thread.sleep( NodeManager.C_INVESTIGATE_TIMEOUT + NodeManager.C_INVESTIGATE_INTERVAL_MS);
        
        assertEquals(0, testAdapter.getReceivedMessageNum());
        
        // Make sure we've gone back to the ping interval
        assertEquals( NodeManager.C_QUERY_INTERVAL_MS, nm.getQueryInterval() );

        // Verify the node state
        assertEquals( EsnInvestigationStatusEnum.TIMEOUT, node.getInvestigationStatus());
        assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());
        assertEquals( 0, nm.getNumNodesInvestigating());
        
        // Send another status message to invoke another investigation
        msgStatusResponse = 
            new MsgDeviceStatusResponse(node, null, 
                    EsnAPIDeviceStatusValue.INFO, 
                    refNode.getSynetId(), refNode.getManufacturerId(), 
                    refNode.getProfile(), refNode.getRevision());
        testAdapter.receiveMessage(msgStatusResponse);
        
        // Verify that the investigation has started again
        assertTrue(testAdapter.waitForCatalogRequest(C_MESSAGE_TIMEOUT, 0));
        
        assertEquals( NodeManager.C_INVESTIGATE_INTERVAL_MS, nm.getQueryInterval() );
        
        // Verify the node state
        assertEquals( EsnInvestigationStatusEnum.INFO, node.getInvestigationStatus());
        assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());
        assertEquals( 1, nm.getNumNodesInvestigating());

        cleanUp(c, cThread);
    }
    
    /**
     * Test a normal status query interval
     * 
     * @throws InterruptedException 
     */
//    @Test
//    public void testStatusPingInterval() throws InterruptedException
//    {
//        Controller c;
//        TestMsgAdapter testAdapter;
//        c = new Controller(true, "", "9000");
//        testAdapter = new TestMsgAdapter();
//        testAdapter.ignoreMsgType(MsgConstants.SN_API_SYSTEM_NODELIST_TRANSMIT);
//        testAdapter.ignoreMsgType(MsgConstants.SN_API_SYSTEM_CATALOG_TRANSMIT);
//        c.addMsgAdapter(testAdapter);
//        c.setNetAdapterEnable(false);
//        c.setXbeeAdapterEnable(false);
//
//        Thread cThread = new ControllerThread(c);
//        cThread.start();
//        
//        // Wait for the initial query
//        assertTrue(testAdapter.waitForMsgDeviceStatusRequest(C_MESSAGE_TIMEOUT, EsnRequestType.INFORMATION));
//        
//        // Wait for the timeout
//        Thread.sleep(NodeManager.C_QUERY_INTERVAL_MS+100);
//        
//        // Wait for the status query
//        assertEquals(1, testAdapter.getReceivedMessageNum());
//        assertTrue(testAdapter.waitForMsgDeviceStatusRequest(1, EsnRequestType.STATUS));
//        
//        cleanUp(c, cThread);
//    }
    
    /**
     * Test a normal investigation operating properly 
     * 
     * @throws InterruptedException 
     */
//    public void testNewRevisionInvestigation() throws InterruptedException
//    {
//        Controller c;
//        TestMsgAdapter testAdapter;
//        c = new Controller(false, "", "9000");
//        testAdapter = new TestMsgAdapter();
//        testAdapter.ignoreMsgType(MsgConstants.SN_API_SYSTEM_NODELIST_TRANSMIT);
//        testAdapter.ignoreMsgType(MsgConstants.SN_API_SYSTEM_CATALOG_TRANSMIT);
//        c.addMsgAdapter(testAdapter);
//        c.setNetAdapterEnable(false);
//        c.setXbeeAdapterEnable(false);
//
//        // Import the element from the test xml
//        Element nodeElement = XmlUtils.readXML(C_STR_TESTFILE1).getRootElement();
//        TestNode refNode = new TestNode(nodeElement);
//
//        TestNode node = new TestNode(refNode.getNodeId());
//
//        NodeManager nm = NodeManager.getInstance();
//
//        nm.addNewNode(node.getNodeId(), node);
//
//        nm.setIsPeriodicCheck(false);
//
//        Thread cThread = new ControllerThread(c);
//        cThread.start();
//
//        // Wait for the initial query
//        assertTrue(testAdapter.waitForMsgDeviceStatusRequest(C_MESSAGE_TIMEOUT, EsnRequestType.INFORMATION));
//
//        // Verify the node state
//        assertEquals( EsnInvestigationStatusEnum.UNKNOWN, node.getInvestigationStatus());
//        assertEquals( EsnStatusEnum.UNKNOWN, node.getNodeStatus());
//
//        // Send back the initial status information
//        MsgDeviceStatusResponse msgStatusResponse = 
//            new MsgDeviceStatusResponse(node, null, 
//                    EsnAPIDeviceStatusValue.INFO, 
//                    refNode.getSynetId(), refNode.getManufacturerId(), 
//                    refNode.getProfile(), refNode.getRevision());
//        testAdapter.receiveMessage(msgStatusResponse);
//
//        assertTrue(testAdapter.waitForCatalogRequest(C_MESSAGE_TIMEOUT, 0));
//
//        // Verify the node state
//        assertEquals( EsnInvestigationStatusEnum.INFO, node.getInvestigationStatus());
//        assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());
//
//        int numFunctions = refNode.getNumTotalFunctions();
//
//        // Send back the initial catalog response
//        MsgCatalogResponse msgCatalogResponse = 
//            // total entries, entry number, num params, function name, param type
//            new MsgCatalogResponse(node, null, numFunctions,
//                    0,0,EsnDataTypes.VOID,null,null); 
//        testAdapter.receiveMessage(msgCatalogResponse);
//
//        // Check to see if all the function requests are received
//        for (int i=1; i<=numFunctions; ++i)
//        {
//            assertTrue(testAdapter.waitForCatalogRequest(C_MESSAGE_TIMEOUT, i));
//
//            // Verify the node state
//            assertEquals( EsnInvestigationStatusEnum.FUNCTION, node.getInvestigationStatus());
//            assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());
//            INodeAction action = refNode.getNodeAction(i);
//
//            sendCatalogResponse(testAdapter, node, action, numFunctions);
//        }
//
//        // Check to see if all the parameter requests are received
//        for (int i=1; i<=numFunctions; ++i)
//        {
//            INodeAction action = refNode.getNodeAction(i);
//            for (int p=1; p<=action.getNumParameters(); ++p)
//            {
//                INodeParameter param = action.getParameter(p);
//                assertTrue(testAdapter.waitForParameterRequest(C_MESSAGE_TIMEOUT, i, p));
//
//                // Verify the node state
//                assertEquals( EsnInvestigationStatusEnum.PARAMETER, node.getInvestigationStatus());
//                assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());
//
//                sendParameterResponse(testAdapter, node, param);
//            }
//        }
//
//        // Give a chance for the last message to get picked up by the thread
//        Thread.sleep(100);
//        
//        assertTrue(refNode.isEqualTo(node, false));
//
//        // Verify the node state
//        assertEquals( EsnInvestigationStatusEnum.COMPLETED, node.getInvestigationStatus());
//        assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());
//        
//        // Import the element from the test xml
//        nodeElement = XmlUtils.readXML(C_STR_TESTFILE2).getRootElement();
//        refNode = new TestNode(nodeElement);
//        
//        // Send back the status information with the new revision
//        msgStatusResponse = 
//            new MsgDeviceStatusResponse(node, null, 
//                    EsnAPIDeviceStatusValue.INFO, 
//                    refNode.getSynetId(), refNode.getManufacturerId(), 
//                    refNode.getProfile(), refNode.getRevision());
//        testAdapter.receiveMessage(msgStatusResponse);
//        
//        // Now wait for the node manager to attempt a new investigation
//        assertTrue(testAdapter.waitForCatalogRequest(C_MESSAGE_TIMEOUT, 0));        
//
//        // Verify the node state
//        assertEquals( EsnInvestigationStatusEnum.INFO, node.getInvestigationStatus());
//        assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());
//
//        numFunctions = refNode.getNumTotalFunctions();
//
//        // Send back the initial catalog response
//        msgCatalogResponse = 
//            // total entries, entry number, num params, function name, param type
//            new MsgCatalogResponse(node, null, numFunctions,
//                    0,0,EsnDataTypes.VOID,null,null); 
//        testAdapter.receiveMessage(msgCatalogResponse);
//
//        // Check to see if all the function requests are received
//        for (int i=1; i<=numFunctions; ++i)
//        {
//            assertTrue(testAdapter.waitForCatalogRequest(C_MESSAGE_TIMEOUT, i));
//
//            // Verify the node state
//            assertEquals( EsnInvestigationStatusEnum.FUNCTION, node.getInvestigationStatus());
//            assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());
//            INodeAction action = refNode.getNodeAction(i);
//
//            sendCatalogResponse(testAdapter, node, action, numFunctions);
//        }
//
//        // Check to see if all the parameter requests are received
//        for (int i=1; i<=numFunctions; ++i)
//        {
//            INodeAction action = refNode.getNodeAction(i);
//            for (int p=1; p<=action.getNumParameters(); ++p)
//            {
//                INodeParameter param = action.getParameter(p);
//                assertTrue(testAdapter.waitForParameterRequest(C_MESSAGE_TIMEOUT, i, p));
//
//                // Verify the node state
//                assertEquals( EsnInvestigationStatusEnum.PARAMETER, node.getInvestigationStatus());
//                assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());
//
//                sendParameterResponse(testAdapter, node, param);
//            }
//        }
//
//        // Give a chance for the last message to get picked up by the thread
//        Thread.sleep(100);
//        
//        assertTrue(refNode.isEqualTo(node, false));
//
//        // Verify the node state
//        assertEquals( EsnInvestigationStatusEnum.COMPLETED, node.getInvestigationStatus());
//        assertEquals( EsnStatusEnum.ACTIVE, node.getNodeStatus());
//
//        cleanUp(c, cThread);
//    }


    /**
     * Clean up from the test
     * 
     * @param c
     * @param cThread
     * @throws InterruptedException
     */
    private void cleanUp(final Controller c, Thread cThread)
            throws InterruptedException
    {
        c.shutDown();
        cThread.join();
    }

    /**
     * @param p_testAdapter
     * @param p_node
     * @param p_action
     * @param p_numFunctions 
     */
    private void sendCatalogResponse(TestMsgAdapter p_testAdapter,
            TestNode p_node, INodeAction p_action, int p_numFunctions)
    {
        // Send the requested function
        MsgCatalogResponse msgCatalogResponse = 
            // total entries, entry number, num params, function name, param type
            new MsgCatalogResponse(p_node, null, p_numFunctions,
                    p_action.getFunctionId(),p_action.getNumParameters(),
                    p_action.getReturnType(), p_action.getName(),null); 
        p_testAdapter.receiveMessage(msgCatalogResponse);

    }

    /**
     * @param p_testAdapter
     * @param p_node
     * @param p_param
     */
    private void sendParameterResponse(TestMsgAdapter p_testAdapter,
            TestNode p_node, INodeParameter p_param)
    {
        MsgParameterResponse msgParamResponse = new 
        MsgParameterResponse(p_node, null, 
                p_param.getFunctionIndex(),
                p_param.getParameterIndex(),
                p_param.getName(),
                p_param.getDataType(),
                p_param.getValidationType(),
                p_param.getMaxStringLength(),
                p_param.getMinimumValue(),
                p_param.getMaximumValue(),
                p_param.getEnumValueMap());
        p_testAdapter.receiveMessage(msgParamResponse);
    }
    

    /**
     * Thread for running the controller in
     */
    private class ControllerThread extends Thread
    {
        private Controller m_controller;

        public ControllerThread(Controller p_controller)
        {
            m_controller = p_controller;
        }

        @Override
        public void run()
        {
            m_controller.run();
        }
    }

}
