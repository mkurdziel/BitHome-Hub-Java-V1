/**
 * 
 */
package synet.controller.test;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import synet.controller.messaging.MsgAdapterBase;
import synet.controller.messaging.messages.Msg;
import synet.controller.messaging.messages.MsgCatalogRequest;
import synet.controller.messaging.messages.MsgDeviceStatusRequest;
import synet.controller.messaging.messages.MsgParameterRequest;
import synet.controller.messaging.messages.MsgSystemNodelistReceive;
import synet.controller.messaging.messages.MsgTx;
import synet.controller.messaging.messages.MsgDeviceStatusRequest.EsnRequestType;
import synet.controller.nodes.NodeBase;

/**
 * @author mkurdziel
 *
 */
public class TestMsgAdapter extends MsgAdapterBase
{
    LinkedBlockingQueue<Msg> m_msgList = new LinkedBlockingQueue<Msg>();
    Object m_msgNotify = new Object();
    HashSet<Byte> m_ignoreMsgTypes = new HashSet<Byte>();

    /**
     * 
     */
    public TestMsgAdapter()
    {
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#startAdapater()
     */
    @Override
    public boolean startAdapater()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#stopAdapter()
     */
    @Override
    public void stopAdapter()
    {

    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#processSystemNodeListReceive(synet.controller.messaging.messages.MsgSystemNodelistReceive)
     */
    @Override
    public void processSystemNodeListReceive(MsgSystemNodelistReceive p_msg)
    {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#getNodeClass()
     */
    @Override
    public Class<?> getNodeClass()
    {
        return TestNode.class;
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#getNodeTypeIdentifierString()
     */
    @Override
    public String getNodeTypeIdentifierString()
    {
        return "TEST";
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#sendMessage(synet.controller.messaging.messages.MsgTx)
     */
    @Override
    public void sendMessage(MsgTx p_msg)
    {

        if (!m_ignoreMsgTypes.contains(p_msg.getAPI()))
        {
            System.out.println("Controller: " + p_msg.getDescription());
            try
            {
                m_msgList.put(p_msg);
            } catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#broadcastMessage(synet.controller.messaging.messages.MsgTx)
     */
    @Override
    public void broadcastMessage(MsgTx p_msg)
    {
        sendMessage(p_msg);
    }

    /**
     * @param p_msg
     */
    public void receiveMessage(Msg p_msg)
    {
        System.out.println("Node: " + p_msg.getDescription());
        fireMsgReceivedEvent(p_msg);
    }

    /**
     * @return
     */
    public int getReceivedMessageNum()
    {
        return m_msgList.size();
    }

    /**
     * @param p_requestType
     * @return
     * @throws InterruptedException 
     */
    public boolean waitForMsgDeviceStatusRequest(long p_timeout, EsnRequestType p_requestType) throws InterruptedException
    {
        if (p_timeout <= 0)
        {
            return false;
        }

        long bailTime = System.currentTimeMillis() + p_timeout;

        synchronized(m_msgList)
        {
            Msg msg = m_msgList.poll(p_timeout, TimeUnit.MILLISECONDS);
            if(msg instanceof MsgDeviceStatusRequest)
            {
                if (((MsgDeviceStatusRequest)msg).getRequestType() == p_requestType)
                {
                    return true;
                }
            }
        }
        return waitForMsgDeviceStatusRequest(bailTime - System.currentTimeMillis(), p_requestType);
    }

    /**
     * @param p_requestType
     * @return
     * @throws InterruptedException 
     */
    public boolean waitForCatalogRequest(long p_timeout, int p_actionIndex ) throws InterruptedException
    {
        if (p_timeout <= 0)
        {
            return false;
        }

        long bailTime = System.currentTimeMillis() + p_timeout;


        synchronized(m_msgList)
        {
            Msg msg = m_msgList.poll(p_timeout, TimeUnit.MILLISECONDS);
            if(msg instanceof MsgCatalogRequest)
            {
                if (((MsgCatalogRequest)msg).getActionIndex() == p_actionIndex)
                {
                    return true;
                }
                else
                {
                    System.err.println("Catalog request for " + ((MsgCatalogRequest)msg).getActionIndex() + " instead of " + p_actionIndex);
                }
            }
            else
            {
                if (msg != null)
                {
               System.err.println("Not a catalog request:" + msg.getMsgType());
                }
                else
                {
                    System.err.println("Msg is null");
                }
            }
        }
        return waitForCatalogRequest(bailTime - System.currentTimeMillis(), p_actionIndex);
    }

    /**
     * @param p_requestType
     * @return
     * @throws InterruptedException 
     */
    public boolean waitForParameterRequest(long p_timeout, int p_actionIndex, int p_parameterIndex ) throws InterruptedException
    {
        if (p_timeout <= 0)
        {
            return false;
        }

        long bailTime = System.currentTimeMillis() + p_timeout;

        synchronized(m_msgList)
        {
            Msg msg = m_msgList.poll(p_timeout, TimeUnit.MILLISECONDS);
            if(msg instanceof MsgParameterRequest)
            {
                if (((MsgParameterRequest)msg).getActionIndex() == p_actionIndex &&
                        ((MsgParameterRequest)msg).getParameterIndex() == p_parameterIndex )
                {
                    return true;
                }
            }
        }
        return waitForParameterRequest(bailTime - System.currentTimeMillis(), p_actionIndex, p_parameterIndex);
    }

    /**
     * @param p_msgTpe
     */
    public void ignoreMsgType(Byte p_msgType) 
    {
        m_ignoreMsgTypes.add(p_msgType);
    }
}
