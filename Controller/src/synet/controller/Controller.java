package synet.controller;

import java.util.ArrayList;

import javax.swing.plaf.SliderUI;

import synet.controller.actions.ActionManager;
import synet.controller.configuration.XMLConfiguration;
import synet.controller.messaging.MsgAdapterBase;
import synet.controller.messaging.MsgAdapterMeta;
import synet.controller.messaging.MsgAdapterNet;
import synet.controller.messaging.MsgAdapterXbee;
import synet.controller.messaging.MsgDispatcher;
import synet.controller.test.TestMsgAdapter;
import synet.controller.utils.Logger;
import synet.controller.utils.SysUtils;


/**
 * @author mkurdziel
 *
 */
public class Controller {

    private static final String TAG = "Controller";

    private static final String C_STR_LOCATION_MAC = "/opt/synet/net";
    private static final String C_STR_LOCATION_LINUX = "/var/opt/synet/net";
    private static final String C_STR_LOCATION_WIN = "c:\\tmp\\synet\\net";
    private static final String C_STR_LOCATION_LOGS = "logs";
    private static final String C_STR_CONFIGURATION = "config.xml";


    private MsgDispatcher m_msgDispatcher;
    private NodeManager m_nodeManager;
    private ActionManager m_actionManager;

    private ConsoleServer m_consoleServer;

    private String m_baseDir;

    private String m_configurationFile;
    private XMLConfiguration m_config;

    private boolean m_isdaemon;
    private boolean m_isRunning;
    private boolean m_isShutDown = false;

    private boolean m_isNetAdapterEnabled = true;
    private boolean m_isXbeeAdapterEnabled = true;

    private String m_consoleServerPort;
    private String m_commPort;

    private ArrayList<MsgAdapterBase> m_msgAdapters  = new ArrayList<MsgAdapterBase>();

    /**
     * Constructor
     * @param p_string 
     * @param p_string 
     * @param b 
     */
    public Controller(
            boolean p_isdaemon, 
            String p_commPort, 
            String p_consoleServerPort)
    {
        m_isdaemon = p_isdaemon;
        m_commPort = p_commPort;
        m_consoleServerPort = p_consoleServerPort;
        m_isRunning = true;
    }

    /**
     * Run the controller. Blocking.
     */
    public void run()
    {
        m_baseDir = getBaseDirString();
        SysUtils.checkAndCreatePath(m_baseDir);

        m_configurationFile = String.format("%s%s%s", m_baseDir, 
                SysUtils.getSeparatorString(), C_STR_CONFIGURATION);

        // If we're not running as a daemon, instruct the logger
        // to direct the output to the console
        if (false == m_isdaemon)
        {
            Logger.setOutputStreams(System.out, System.err);
        }

        Logger.start(
                String.format("%s%s%s%s", 
                        m_baseDir, 
                        SysUtils.getSeparatorString(), 
                        C_STR_LOCATION_LOGS,
                        SysUtils.getSeparatorString()));


        // Load the configuration from the XML file
        m_config = new XMLConfiguration(m_configurationFile);

        Logger.setConfiguration(m_config.subset("logging"));
        Logger.v(TAG, "Starting on " + System.getProperty("os.name"));

        m_msgDispatcher = MsgDispatcher.getInstance();
        m_msgDispatcher.setConfiguration(m_config.subset("msgDispatcher"));

        // Create the node manager
        m_nodeManager = NodeManager.getInstance();
        m_nodeManager.setConfiguration(m_config.subset("nodeManager"));
        m_nodeManager.setMsgDispatcher(m_msgDispatcher);

        // Create the action manager
        m_actionManager = ActionManager.getInstance();
        m_actionManager.setNodeManager(m_nodeManager);
        m_actionManager.setMsgDispatcher(m_msgDispatcher);

        // Create the console server
        m_consoleServer = new ConsoleServer(m_consoleServerPort);
        m_consoleServer.setActionManager(m_actionManager);
        m_consoleServer.setNodeManager(m_nodeManager);
        m_consoleServer.setConfiguration(m_config.subset("consoleServer"));

        // Set all the dependencies
        m_nodeManager.setActionManager(m_actionManager);

        if (m_isXbeeAdapterEnabled)
        {
            MsgAdapterXbee xbeeAdapter = new MsgAdapterXbee(m_commPort);
            m_msgDispatcher.addMsgAdapter(xbeeAdapter);
        }

        if (m_isNetAdapterEnabled)
        {
            MsgAdapterNet netAdapter = new MsgAdapterNet(m_baseDir);
            m_msgDispatcher.addMsgAdapter(netAdapter);
        }
        
        // Add the meta node adapter
        MsgAdapterMeta metaAdapter = new MsgAdapterMeta();
        m_msgDispatcher.addMsgAdapter(metaAdapter);

        // Add any additional adapters
        for (MsgAdapterBase adapter : m_msgAdapters)
        {
            m_msgDispatcher.addMsgAdapter(adapter);
        }

        // Start everything up
        if (m_msgDispatcher.start())
        {
            // Once the message system is up, we can start everything else
            m_nodeManager.start();
            m_actionManager.start();
            m_consoleServer.start();


            // Either run in daemon mode or local console
            if (m_isdaemon)
            {
                while(true == m_isRunning)
                {
                    try
                    {
                        synchronized(this)
                        {
                            wait();
                        }
                    } catch (InterruptedException e)
                    {
                        Logger.i(TAG, "thread interrupted");
                    }
                }
            }
            else
            {
                m_consoleServer.localConsole();
            }
        }
        else
        {
            Logger.w(TAG, "Could not start MsgDispatcher");
        }
        
        shutDown();
    }


    /**
     * @return A string for the base file directory
     */
    private String getBaseDirString() {
        if (SysUtils.isLinux()) 
        {
            return C_STR_LOCATION_LINUX;
        }
        else if (SysUtils.isMac()) 
        {
            return C_STR_LOCATION_MAC;
        }
        else if (SysUtils.isWindows())
        {
            return C_STR_LOCATION_WIN;
        }
        else
        {
            Logger.e(TAG, "Unsupported OS for base directory");
            return "";
        }
    }

    /**
     * Shut down the controller
     */
    public void shutDown()
    {
        if (m_isRunning)
        {
            m_isRunning = false; 

            Logger.i(TAG, "Shutting down controller");


            synchronized(this)
            {
                this.notify();
            }
            m_consoleServer.stop();
            m_msgDispatcher.stop();
            m_nodeManager.stop();
            m_actionManager.stop();
            m_config.writeConfiguration();
            Logger.stop();
            m_isShutDown = true;
        }
    }

    /**
     * @return the message dispatcher for the controller
     */
    public MsgDispatcher getMsgDispatcher()
    {
        return m_msgDispatcher;
    }

    /**
     * @param p_enable
     */
    public void setNetAdapterEnable(boolean p_enable)
    {
        m_isNetAdapterEnabled = p_enable;
    }

    /**
     * @param p_enable
     */
    public void setXbeeAdapterEnable(boolean p_enable)
    {
        m_isXbeeAdapterEnabled = p_enable;
    }

    /**
     * Add an adapter to the controller
     * 
     * @param p_adapter
     */
    public void addMsgAdapter(MsgAdapterBase p_adapter)
    {
        m_msgAdapters.add(p_adapter);
    }

    public NodeManager getNodeManager()
    {
        return m_nodeManager;
    }
}
