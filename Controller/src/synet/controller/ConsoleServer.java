package synet.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import nu.xom.Attribute;
import nu.xom.Element;

import org.apache.commons.lang3.StringUtils;

import synet.controller.Protocol.EsnDataTypes;
import synet.controller.actions.ActionBase;
import synet.controller.actions.ActionManager;
import synet.controller.actions.ActionParameter;
import synet.controller.actions.ActionRequest;
import synet.controller.actions.IAction;
import synet.controller.actions.INodeAction;
import synet.controller.actions.INodeParameter;
import synet.controller.actions.IParameter;
import synet.controller.actions.NodeAction;
import synet.controller.actions.NodeParameter;
import synet.controller.actions.ParameterBase;
import synet.controller.actions.SequenceAction;
import synet.controller.actions.ActionParameter.EsnActionParameterType;
import synet.controller.configuration.Configuration;
import synet.controller.messaging.MsgDispatcher;
import synet.controller.messaging.messages.MsgBootloadTransmit;
import synet.controller.messaging.messages.MsgConstants.EsnAPIBootloadTransmit;
import synet.controller.nodes.MetaNode;
import synet.controller.nodes.NodeBase;
import synet.controller.utils.Logger;
import synet.controller.utils.SysUtils;
import synet.controller.utils.XmlUtils;

public class ConsoleServer
{
    public static final int PORT = 9000;
    public static final String TAG = "ConsoleServer";

    private ServerSocket m_serverSocket = null;
    private boolean m_isRunning = false;

    private ConsoleServerThread m_serverThread;

    private ArrayList<ConsoleClientServerThread> m_clientThreads = 
        new ArrayList<ConsoleClientServerThread>();

    private NodeManager m_nodeManager;
    private ActionManager m_actionManager;
    private Configuration m_config;
    private BufferedReader m_reader = null;
    private InputStreamReader m_inputStreamReader = null;

    Collection<NodeBase> m_currentNodes;

    /**
     * Default constructor
     * @param p_consoleServerPort 
     * @param p_isdaemon 
     */
    public ConsoleServer(String p_consoleServerPort)
    {
        if (p_consoleServerPort != null)
        {
            try
            {
                Logger.v(TAG, "using console server port argument " + p_consoleServerPort);
            }
            catch (NumberFormatException e)
            {
                Logger.w(TAG, "error parsing console server port argument " + p_consoleServerPort);
            }
        }
    }

    /**
     * Set the node manager
     * 
     * @param p_nodeManager
     */
    public void setNodeManager(NodeManager p_nodeManager)
    {
        m_nodeManager = p_nodeManager;
    }

    /**
     * Set the action manager
     * 
     * @param p_actionManager
     */
    public void setActionManager(ActionManager p_actionManager)
    {
        m_actionManager = p_actionManager;
    }

    /**
     * Set the configuration
     * 
     * @param p_config
     */
    public void setConfiguration(Configuration p_config)
    {
        m_config = p_config;
    }

    /**
     * Start the ConsoleServer
     * 
     * @return true if the server was started on the socket
     */
    public boolean start()
    {
        m_isRunning = true;

        try
        {
            m_serverSocket = new ServerSocket(PORT);

            m_serverThread = new ConsoleServerThread();
            m_serverThread.start();
        }
        catch (IOException e)
        {
            Logger.e(TAG, "Could not start socket on port " + PORT);
            m_isRunning = false;
            return false;
        }

        Logger.i(TAG, "started successfully on port " + PORT);

        return true;
    }

    /**
     * Stop the console server
     */
    public void stop()
    {
        try
        {
            m_isRunning = false;
            bootOthers(null);
            if (m_reader != null)
            {
                Logger.i(TAG, "closing local console");
                m_inputStreamReader.close();
                m_reader.close();
            }
            if (m_serverThread != null)
            {
                m_serverThread.interrupt();
            }
            if (m_serverSocket != null)
            {
                m_serverSocket.close();
            }
        }
        catch (IOException e)
        {
            Logger.e(TAG, "Error closing", e);
        }
        Logger.i(TAG, "stopped");
    }

    /**
     * Print the intro line
     * 
     * @param p_stream
     */
    private void printIntro(PrintStream p_stream)
    {
        p_stream.println("Enter a command (type 'q' to exit): ");
    }

    /**
     * Thread to accept console client connections
     */
    public class ConsoleServerThread extends Thread
    {
        private final static String TAG = "ConsoleServerThread";

        /**
         * Default constructor
         */
        public ConsoleServerThread()
        {
            this.setName(TAG); 
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run()
        {
            Logger.v(TAG, "server thread started");

            while(m_isRunning)
            {
                try {
                    Socket clientSocket = m_serverSocket.accept();
                    Logger.i(TAG, "Socket accepted from " + clientSocket.getInetAddress());

                    ConsoleClientServerThread clientThread = new ConsoleClientServerThread(clientSocket);

                    // Add him to our list
                    m_clientThreads.add(clientThread);

                    // Run the client thread
                    clientThread.start();
                }
                catch (SocketException e)
                {
                    if (m_isRunning)
                    {
                        Logger.w(TAG, "Socket interrupted", e);
                    }
                } 
                catch (IOException e) {
                    Logger.e(TAG, "IOException thrown: ", e);
                }

            }
        }
    }


    /**
     * Boot all other clients
     */
    private void bootOthers(ConsoleClientServerThread p_myThread)
    {
        Logger.v(TAG, "Booting other clients from server");

        while(!m_clientThreads.isEmpty())
        {
            ConsoleClientServerThread thread = m_clientThreads.remove(0);
            if (thread != p_myThread)
            {
                thread.shutDown();
            }
        }

        if(p_myThread != null)
        {
            m_clientThreads.add(p_myThread);
        }
    }

    /**
     * Thread that runs when a client connection is accepted
     */
    public class ConsoleClientServerThread extends Thread
    {
        private static final String TAG = "ConsoleClientServerThread";
        private Socket m_socket;
        private PrintWriter m_out;
        private BufferedReader m_in;

        /**
         * Default constructor
         * 
         * @param p_clientSocket
         */
        public ConsoleClientServerThread(Socket p_clientSocket) 
        {
            m_socket = p_clientSocket;

            this.setName(TAG + " - " + p_clientSocket.getInetAddress());

            try
            {
                m_out = new PrintWriter(m_socket.getOutputStream());
                m_in = new BufferedReader(
                        new InputStreamReader(m_socket.getInputStream()));
            } catch (IOException e)
            {
                Logger.e(TAG, "IO Exception with client socket", e);
            }
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            boolean run = true;
            // First we need to check if there is already a client connected. 
            // If so we present the option to boot him. 
            synchronized (m_clientThreads) 
            {
                if (m_clientThreads.size() > 1)
                {
                    boolean doBoot = warnAboutBooting();
                    if (doBoot)
                    {
                        bootOthers(this);
                    }
                    else
                    {
                        run = false;
                    }
                }
            }

            if (run)
            {
                Logger.i(TAG, "Console client starting on " + m_socket.getInetAddress());

                try
                {
                    PrintStream pStream = new PrintStream(m_socket.getOutputStream());
                    Logger.setOutputStreams(pStream, pStream);

                    // Print the intro to the client
                    printIntro(pStream);

                    String strIn, result;
                    while ((strIn = m_in.readLine()) != null) {
                        result = processInput(strIn);
                        if (result != null)
                        {
                            pStream.print(result);
                        }
                    }
                } catch (IOException e)
                {
                    System.err.println("Stream killed");
                    Logger.e(TAG, "Error setting up client print streams", e);
                }

                // Reset the streams
                Logger.setOutputStreams(null, null);

                m_clientThreads.remove(this);
            }
        }


        /**
         * Kill the connection
         */
        private void shutDown()
        {
            m_out.println("Connection being terminated");
            try
            {
                m_socket.close();
            } catch (IOException e)
            {
                Logger.e(TAG, "Error closing client socket", e);
            }
        }

        /**
         * Warn the client that there are other clients connected and ask if 
         * they should be booted out of OTV. Hack the planet.
         * 
         * @return
         */
        private boolean warnAboutBooting()
        {
            if (m_socket.isConnected())
            {
                m_out.println("Another user is already connected. Boot other users and take control? [Y]");
                try
                {
                    String userInput = m_in.readLine();
                    // If yes or blank, boot 'em
                    if (userInput.equalsIgnoreCase("y") || userInput.equals(""))
                    {
                        return true;
                    }
                } catch (IOException e)
                {
                    Logger.e(TAG, "Error reading input from client", e);
                }
            }
            return false;
        }
    }

    /**
     * Run the local console
     */
    public void localConsole()
    {
        m_inputStreamReader = new InputStreamReader(System.in);
        m_reader = new BufferedReader(m_inputStreamReader);
        String strIn, result;
        try
        {
            while (m_isRunning)
            {
                if (m_reader.ready())
                {
                    if ((strIn = m_reader.readLine()) != null)
                    {
                        result = processInput(strIn); 
                        if (result == null)
                        {
                            break;
                        }
                        System.out.println(result);
                    }
                }
                else
                {

                    Thread.sleep(500);
                }
            }
        } 
        catch (IOException e)
        {
            e.printStackTrace();
        } 
        catch (InterruptedException e)
        {
            Logger.w(TAG, "localConsole thread interrupted");
        }
    }


    private String processInput(String curLine)
    {
        StringBuilder sb = new StringBuilder();

        if(curLine.equals("q"))
        {
            return null;
        }
        if(curLine.equals("?"))
        {
            printOutput(sb);
        }
        else if(curLine.equals("n"))
        {
            printNodeList(sb);
        }
        else if(curLine.equals("C"))
        {
            printConfig(sb);
        }
        else if(curLine.substring(0,1).equals("A"))
        {
            String[] tokens = curLine.split(" ");
            if (tokens.length >= 2)
            {
                short actionId = (short)Long.parseLong(tokens[1].replaceFirst("0x", ""), 16);

                IAction userAction = m_actionManager.getAction(actionId);

                if (userAction != null)
                {
                    if (userAction.getNumParameters() == (tokens.length-2))
                    {
                        // Add the parameter values
                        for(int paramLoop = 3; paramLoop<tokens.length; ++paramLoop)
                        {
                            IParameter p = userAction.getParameter(paramLoop - 2);
                            if (p != null)
                            {
                                if(!p.setValue(tokens[paramLoop]))
                                {
                                    sb.append("Could not set parameter value to " + tokens[paramLoop] + "\n");
                                }
                            }
                            else
                            {
                                sb.append("Could not set value of parameter " + (paramLoop-3) + "\n");
                            }
                        }

                        m_actionManager.executeAction(userAction.getActionId());
                    }
                    else
                    {
                        sb.append("Invalid number of parameter values");
                    }
                }
                else
                {
                    sb.append("could not find action for id");
                }

            }
            else
            {
                sb.append("invalid number of tokens");
            }
        }
        else if(curLine.substring(0,1).equals("U"))
        {
            // Handle any sub options
            if (curLine.length() > 1)
            {
                // Add new user action
                if(curLine.subSequence(1, 2).equals("a"))
                {
                    if(curLine.length() > 2)
                    {
                        if(curLine.subSequence(2, 3).equals("s"))
                        {
                            SequenceAction action = m_actionManager.addSequenceAction();
                            if(curLine.length() > 4)
                            {
                                action.setName(curLine.substring(4));
                            }
                        }
                    }
                }

                // Handle Sequence Action specifics
                else if(curLine.subSequence(1, 2).equals("S"))
                {
                    // Add to a sequence action
                    if(curLine.length() > 2)
                    {
                        if(curLine.subSequence(2, 3).equals("a"))
                        {
                            String[] tokens = curLine.split(" ");
                            if (tokens.length >= 3)
                            {
                                try
                                {
                                    short userActionId = (short)Long.parseLong(tokens[1].replaceFirst("0x", ""), 16);
                                    short actionAddId = (short)Long.parseLong(tokens[2].replaceFirst("0x", ""), 16);

                                    SequenceAction userAction = (SequenceAction) m_actionManager.getAction(userActionId);
                                    IAction addAction = m_actionManager.getAction(actionAddId);

                                    if (userAction != null && addAction != null)
                                    {
                                        // Add the parameter values
                                        for(int paramLoop = 3; paramLoop<tokens.length; ++paramLoop)
                                        {
                                            IParameter p = addAction.getParameter(paramLoop - 2);
                                            if (p != null)
                                            {
                                                if(!p.setValue(tokens[paramLoop]))
                                                {
                                                    sb.append("Could not set parameter value to " + tokens[paramLoop] + "\n");
                                                }
                                            }
                                            else
                                            {
                                                sb.append("Could not set value of parameter " + (paramLoop-3) + "\n");
                                            }
                                        }

                                        userAction.addAction(addAction);
                                        m_actionManager.sendUserActionListUpdate();
                                    }
                                    else
                                    {
                                        sb.append("Could not find actions from ID\n");
                                    }
                                }
                                catch (NumberFormatException e)
                                {
                                    sb.append("Incorrect number formatting\n");
                                    sb.append(e.toString());
                                    sb.append("\n");
                                }
                            }
                            else
                            {
                                sb.append("Incorrect number of options\n");
                            }
                        }
                    }
                }
                else
                {
                    sb.append("Unhandled User Action option\n");
                }
            }
            else
            {
                printUserActions(sb);
            }
        }
        // Handle Parameter specifics
        else if(curLine.subSequence(0, 1).equals("P"))
        {
            if(curLine.length() > 2)
            {
                // Set Parameter
                if(curLine.subSequence(1, 2).equals("s"))
                { 
                    String[] tokens = curLine.split(" ");
                    if (tokens.length == 3)
                    {
                        short paramId = (short)Long.parseLong(tokens[1].replaceFirst("0x", ""), 16);
                        ParameterBase param = m_actionManager.getParameter(paramId);
                        if (param != null)
                        {
                            sb.append("Setting parameter value to " + tokens[2] + "\n");

                            if(param.setValue(tokens[2]))
                            {
                                sb.append("Success\n");
                                // TODO: remove these peppered everywhere
                                m_actionManager.sendUserActionListUpdate();
                            }
                            else
                            {
                                sb.append("Invalid value\n");
                            }

                        }
                        else
                        {
                            sb.append("Could not find parameter");
                        }
                    }
                    else
                    {
                        sb.append("Invalid number of tokens");
                    }
                }
                // Set Parameter type
                else if(curLine.subSequence(1, 2).equals("t"))
                { 
                    String[] tokens = curLine.split(" ");
                    if (tokens.length == 3)
                    {
                        short paramId = (short)Long.parseLong(tokens[1].replaceFirst("0x", ""), 16);
                        ActionParameter param = (ActionParameter) m_actionManager.getParameter(paramId);
                        if (param != null)
                        {
                            sb.append("Setting parameter type to " + tokens[2] + "\n");

                            EsnActionParameterType paramType = EsnActionParameterType.valueOf(tokens[2]);
                            if(paramType != null)
                            {
                                param.setParameterType(paramType);
                                sb.append("Success\n");
                                m_actionManager.sendUserActionListUpdate();
                            }
                            else
                            {
                                sb.append("Invalid value\n");
                            }

                        }
                        else
                        {
                            sb.append("Could not find parameter");
                        }
                    }
                    else
                    {
                        sb.append("Invalid number of tokens");
                    }
                }
            }
        }
        else if(curLine.length() > 3 && curLine.substring(0, 2).equals("rn"))
        {
            try
            {
                Integer i = Integer.parseInt(curLine.substring(3));
                reinvestigateNode(sb, i);
            } catch (NumberFormatException e)
            {
                sb.append("Incorrect node number");
            }   
        }
        else if(curLine.length() > 2 && curLine.substring(0, 1).equals("r"))
        {
            try
            {
                Integer i = Integer.parseInt(curLine.substring(2));
                rebootNode(sb, i);
            } catch (NumberFormatException e)
            {
                sb.append("Incorrect node number");
            }   
        }
        else if(curLine.length() > 2 && curLine.substring(0, 1).equals("l"))
        {
            try
            {
                Integer i = Integer.parseInt(curLine.substring(2));
                Logger.setMinLevel(i);
            } catch (NumberFormatException e)
            {
                sb.append("Incorrect level number");
            }   
        }
        else if(curLine.length() > 2 && curLine.substring(0, 1).equals("L"))
        {
            String tagString = curLine.substring(2);

            Logger.setTagFilter(tagString);

        }
        else if(curLine.equals("L"))
        {
            Logger.setTagFilter("");
        }
        else if(curLine.length() > 2 && curLine.substring(0, 1).equals("i"))
        {
            try
            {
                Integer i = Integer.parseInt(curLine.substring(2));
                printNodeInfo(sb, i-1);
            } catch (NumberFormatException e)
            {
                sb.append("Incorrect node number");
            }
        }
        else if(curLine.length() > 2 && curLine.substring(0, 1).equals("c"))
        {
            try
            {
                Integer i = Integer.parseInt(curLine.substring(2));
                printNodeActions(sb, i-1);
            } catch (NumberFormatException e)
            {
                sb.append("Incorrect node number");
            }
        }
        else if(curLine.length() > 2 && curLine.substring(0, 1).equals("d"))
        {
            try
            {
                Integer i = Integer.parseInt(curLine.substring(2));
                deleteNode(sb, i-1);
            } catch (NumberFormatException e)
            {
                sb.append("Incorrect node number");
            }
        }
        else if(curLine.length() >= 3 && curLine.substring(0, 3).equals("Nca"))
        {
            String[] tokens = curLine.split(" ");
            if (tokens.length == 3)
            {
                long nodeId = parseLong(tokens[1].replaceFirst("0x", ""));
                MetaNode node = (MetaNode) m_nodeManager.getNode(nodeId);

                short actionId = (short)Long.parseLong(tokens[2].replaceFirst("0x", ""), 16);

                ActionBase action = (ActionBase) m_actionManager.getAction(actionId);
                if (node != null && action != null)
                {
                    node.addAction(action);
                    rebootNode(sb, nodeId);
                }
                else
                {
                    sb.append("Could not get node or action");
                }

            }
            else
            {
                sb.append("Incorrect number of parameters");
            }
        }
        else if(curLine.length() >= 2 && curLine.substring(0, 2).equals("Na"))
        {
            String[] tokens = curLine.split(" ");
            // Create a new node with a unique ID
            MetaNode node = new MetaNode(m_nodeManager.getUniqueNodeId());
            if (tokens.length > 1)
            {
                node.setName(tokens[1]);
            }

            m_nodeManager.addNewNode(node.getNodeId(), node);

            sb.append("Added new meta node: " + node.getDescString());
        }
        else if(curLine.length() > 4 && curLine.substring(0, 1).equals("N"))
        {
            try
            {
                Integer i = Integer.parseInt(curLine.substring(2, 3));
                updateNodeName(sb, i-1, curLine.substring(4, curLine.length()));
            } catch (NumberFormatException e)
            {
                sb.append("Incorrect node number " + curLine.substring(2));
            }
        }
        else if(curLine.length() > 4 && curLine.substring(0, 1).equals("u"))
        {
            try
            {
                Integer i = Integer.parseInt(curLine.substring(2, 3));
                updateNode(sb, i-1, curLine.substring(4, curLine.length()));
            } catch (NumberFormatException e)
            {
                sb.append("Incorrect node number " + curLine.substring(2));
            }
        }
        else if(curLine.length() > 2 && curLine.substring(0, 1).equals("a"))
        {
            try
            {
                String[] tokens = curLine.split(" ");
                if (tokens.length > 2)
                {
                    sendAction(sb, tokens);
                }
                else
                {
                    sb.append("Incorrect format to send function");
                }

            } catch (NumberFormatException e)
            {
                sb.append("Incorrect node number");
            }
        }
        return sb.toString();
    }

    /**
     * Update the node with the given file
     * @param p_sb
     * @param p_i
     * @param p_substring
     */
    private void updateNode(StringBuilder p_sb, int p_nodeNum, String p_fileName)
    {
        NodeBase node = (NodeBase)m_currentNodes.toArray()[p_nodeNum];

        m_nodeManager.updateNode(node, p_fileName);
    }


    /**
     * Update the node with the given name
     * @param p_sb
     * @param p_i
     * @param p_substring
     */
    private void updateNodeName(StringBuilder p_sb, int p_nodeNum, String p_name)
    {
        NodeBase node = (NodeBase)m_currentNodes.toArray()[p_nodeNum];
        p_sb.append( "Setting node " + node.getDescString() + " name to " + p_name);
        node.setName(p_name);
    }

    /**
     * Print all the configuration values
     * @param p_sb 
     */
    private void printConfig(StringBuilder p_sb) {
        Map<String, String> config = m_config.getAll();

        p_sb.append("Configuration Values\n");
        p_sb.append("========================\n");

        for(String key : config.keySet())
        {
            p_sb.append(String.format("%s = %s", key, config.get(key)));
            p_sb.append("\n");
        }
    }

    private void reinvestigateNode(StringBuilder p_sb, int p_node)
    {
        if (p_node > 0 && p_node <= m_currentNodes.size())
        {
            p_sb.append("Reinvestigating node " + p_node);
            p_sb.append("\n");
            NodeBase node = (NodeBase)m_currentNodes.toArray()[p_node-1];

            m_nodeManager.reinvestigateNode(node);
        }
        else
        {
        }
    }

    private void rebootNode(StringBuilder p_sb, int p_node)
    {
        if (p_node > 0 && p_node <= m_currentNodes.size())
        {
            p_sb.append("Rebooting node " + p_node);
            p_sb.append("\n");
            NodeBase node = (NodeBase)m_currentNodes.toArray()[p_node-1];


            m_nodeManager.rebootNode(node);
        }
        else
        {
            p_sb.append("Incorrect node number\n");
        }
    }
    
    private void rebootNode(StringBuilder p_sb, long p_nodeId)
    {
            p_sb.append("Rebooting node " + p_nodeId);
            p_sb.append("\n");
            NodeBase node = m_nodeManager.getNode(p_nodeId);


            if (node != null)
            {
            m_nodeManager.rebootNode(node);
            } 
            else
            {
                p_sb.append("Node not found");
            }
    }

    private void sendAction(StringBuilder p_sb, String[] tokens)
    {
        try
        {
            int nodeNum = Integer.parseInt(tokens[1]);
            int functionNum = Integer.parseInt(tokens[2]);

            if (nodeNum > 0 && nodeNum <= m_currentNodes.size())
            {
                NodeBase node = (NodeBase)m_currentNodes.toArray()[nodeNum-1];
                INodeAction function = node.getNodeAction(functionNum);

                if (function != null)
                {
                    if (function.getNumParameters() == tokens.length - 3)
                    {
                        INodeParameter param;
                        boolean allParams = true;
                        for (int i=1; i<= function.getNumParameters(); ++i)
                        {
                            param = function.getParameter(i);

                            if (!param.setValue(tokens[2+i]))
                            {
                                p_sb.append(i + " - Error parsing parameter " + tokens[2+i]);
                                p_sb.append("\n");
                                allParams = false;
                            }
                        }

                        if (allParams)
                        {
                            ActionRequest request = m_actionManager.executeAction(function.getActionId(), 5000);
                            if (request.waitForExecution()){
                                p_sb.append(">Action Execution returned " + request.getStatus());
                                p_sb.append("\n");
                                if (function.getReturnType() != EsnDataTypes.VOID)
                                {
                                    p_sb.append(">Action return value: " + function.getStringReturnValue());
                                    p_sb.append("\n");
                                }
                            }
                            else
                            {
                                p_sb.append(">Action failed: " + request.getErrorMessage());
                                p_sb.append("\n");
                            }
                        }
                    }
                    else
                    {
                        p_sb.append("Incorrect number of parameters\n");
                    }
                }
                else
                {
                    p_sb.append("Incorrect function number\n");
                }
            }
            else
            {
                p_sb.append("Incorrect node number\n");
            }
        }
        catch (NumberFormatException e)
        {
            p_sb.append("Error parsing values\n");
        }
    }

    private void printNodeInfo(StringBuilder p_sb, Integer i) {
        if (m_currentNodes != null && m_currentNodes.toArray().length > i)
        {
            NodeBase node = (NodeBase) m_currentNodes.toArray()[i];
            p_sb.append("Info for node " + (i+1));
            p_sb.append("\n");
            p_sb.append("========================");
            p_sb.append("\n");
            p_sb.append(String.format("Name: %s", node.getName()));
            p_sb.append("\n");
            p_sb.append(String.format("Desc: %s", node.getDescString()));
            p_sb.append("\n");
            p_sb.append(String.format("Status: %s", node.getNodeStatus()));
            p_sb.append("\n");
            p_sb.append(String.format("Investigated: %s", node.getInvestigationStatus()));
            p_sb.append("\n");
            p_sb.append(String.format("Known: %s", node.getIsUnknown()));
            p_sb.append("\n");
            p_sb.append(String.format("Last Seen: %s", node.getLastSeen()));
            p_sb.append("\n");
            p_sb.append(String.format("Manufacturer: %x", node.getManufacturerId()));
            p_sb.append("\n");
            p_sb.append(String.format("Node ID: %x", node.getNodeId()));
            p_sb.append("\n");
            p_sb.append(String.format("Profile: %x", node.getProfile()));
            p_sb.append("\n");
            p_sb.append(String.format("Revision: %x", node.getRevision()));
            p_sb.append("\n");
            p_sb.append(String.format("SyNet ID: %x", node.getSynetId()));
            p_sb.append("\n");
            p_sb.append(String.format("Node type: %s", node.getNodeTypeIdentifierString()));
            p_sb.append("\n");
        }
        else
        {
            p_sb.append("Unable to get requested node " + i);
            p_sb.append("\n");
        }
    }

    private void deleteNode(StringBuilder p_sb, Integer i) {
        if (m_currentNodes != null && m_currentNodes.toArray().length > i)
        {
            NodeBase node = (NodeBase) m_currentNodes.toArray()[i];
            m_nodeManager.deleteNode(node.getNodeId());
        }
        else
        {
            p_sb.append("Unable to get requested node " + i);
            p_sb.append("\n");
        }
    }

    private void printNodeActions(StringBuilder p_sb, Integer i) {
        if (m_currentNodes != null && m_currentNodes.toArray().length > i)
        {
            NodeBase node = (NodeBase) m_currentNodes.toArray()[i];
            INodeAction[] nodeActions = node.getNodeActions();
            p_sb.append("Actions("+nodeActions.length+") for node " + (i+1));
            p_sb.append("\n");
            p_sb.append("========================");
            p_sb.append("\n");
            for (INodeAction a : nodeActions)
            {
                printNodeAction(p_sb, a);
            }
        }
        else
        {
            p_sb.append("Unable to get requested node " + i);
            p_sb.append("\n");
        }
    }

    private void printNodeAction(StringBuilder p_sb, INodeAction action)
    {
        p_sb.append(String.format("%d, Name: %s", action.getFunctionId(), action.getName()));
        p_sb.append("\n");
        p_sb.append(String.format("ActionId: %s", action.getActionIdString()));
        p_sb.append("\n");
        p_sb.append(String.format("Description: %s", action.getDescription()));
        p_sb.append("\n");
        p_sb.append(String.format("Return Type: %s", action.getReturnType()));
        p_sb.append("\n");
        p_sb.append(String.format("Num Parameters: %d", action.getNumParameters()));
        p_sb.append("\n");

        for (int i=1; i<= action.getNumParameters(); ++i)
        {
            INodeParameter p = action.getParameter(i);
            if (p != null)
            {
                p_sb.append("Parameter " + p.getDescription());
                p_sb.append("\n");
            }
            else
            {
                p_sb.append("Null parameter at index " + i);
                p_sb.append("\n");
            }
        }
    }


    private void printNodeList(StringBuilder p_sb)
    {
        m_currentNodes = NodeManager.getInstance().getNodeList();

        int i=1;
        for(NodeBase node : m_currentNodes)
        {
            p_sb.append(String.format("%d: %s - %X %s", i++, node.getName(), node.getNodeId(), node.getNodeStatusString()));
            p_sb.append("\n");
        }
    }

    private void printUserActions(StringBuilder p_sb)
    {
        IAction[] userActions = ActionManager.getInstance().getUserActions();

        p_sb.append("User Actions("+userActions.length +")");
        p_sb.append("\n");
        p_sb.append("========================");
        p_sb.append("\n");
        int i=1;
        for(IAction action : userActions)
        {
            p_sb.append(String.format("%d: %s\n", i++, action.toString()));
        }
    }

    private void printOutput(StringBuilder p_sb)
    {
        p_sb.append("Options\n");
        p_sb.append("========================\n");
        p_sb.append("C   print configuration options\n");
        p_sb.append("n   print node list\n");
        p_sb.append("N <node #> <name> set node name\n");
        p_sb.append("Na <name> add a meta node\n");
        p_sb.append("Nca <nodeId> <actionId> add action to meta node\n");
        p_sb.append("i <node #>  print node info\n");
        p_sb.append("d <node #>  delete node\n");
        p_sb.append("c <node #>  print node catalog\n");
        p_sb.append("a <node #>  <action #> [param 1] [param 2] send node action\n");
        p_sb.append("r <node #>  reboot node\n");
        p_sb.append("rn <node #> reinvestigate node\n");
        p_sb.append("l <level #> set the min log level\n");
        p_sb.append("L <tag>     set the log filter string, comma separated ie. Node,NodeManager\n");
        p_sb.append("u <node #> <filename> update the node with the given file\n");
        p_sb.append("A <actionId> execute user action\n");
        p_sb.append("U list the user actions\n");
        p_sb.append("Uas <name> add a new sequence action\n");
        p_sb.append("USa <userActionId> <addedActionId> <params> add to a sequence action\n");
        p_sb.append("Ps <paramId> <value>  set the value of the parameter\n");
        p_sb.append("Pt <paramId> <type>  INPUT, CONSTANT, DEPENDENT\n");
    }

    // TODO: Combine this with XMLUtils
    private long parseLong(String p_long)
    {
        if (p_long != null)
        {
            String strLong = StringUtils.leftPad(p_long, 16, "0");
            Long msb = Long.parseLong(strLong.substring(0, 8), SysUtils.C_INT_BASE) << 32;
            Long lsb = Long.parseLong(strLong.substring(8, 16), SysUtils.C_INT_BASE);
            return msb | lsb;
        }
        return 0;
    }
}
