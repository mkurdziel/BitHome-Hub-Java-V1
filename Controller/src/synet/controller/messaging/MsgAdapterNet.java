/**
 * 
 */
package synet.controller.messaging;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.comparator.NameFileComparator;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import name.pachler.nio.file.ClosedWatchServiceException;
import name.pachler.nio.file.FileSystems;
import name.pachler.nio.file.Path;
import name.pachler.nio.file.Paths;
import name.pachler.nio.file.StandardWatchEventKind;
import name.pachler.nio.file.WatchEvent;
import name.pachler.nio.file.WatchKey;
import name.pachler.nio.file.WatchService;
import name.pachler.nio.file.ext.Bootstrapper;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Attribute;
import nu.xom.Elements;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.ValidityException;

import synet.controller.Protocol.EsnDataTypes;
import synet.controller.actions.ActionBase;
import synet.controller.actions.ActionManager;
import synet.controller.actions.ActionRequest;
import synet.controller.actions.IAction;
import synet.controller.actions.INodeAction;
import synet.controller.actions.INodeParameter;
import synet.controller.actions.NodeAction;
import synet.controller.actions.NodeParameter;
import synet.controller.actions.ParameterBase;
import synet.controller.messaging.messages.MsgConstants;
import synet.controller.messaging.messages.MsgSystemCatalogTransmit;
import synet.controller.messaging.messages.MsgSystemNodelistReceive;
import synet.controller.messaging.messages.MsgSystemNodelistTransmit;
import synet.controller.messaging.messages.MsgSystemUserActionListReceive;
import synet.controller.messaging.messages.MsgSystemUserActionListTransmit;
import synet.controller.messaging.messages.MsgTx;
import synet.controller.nodes.NodeBase;
import synet.controller.nodes.NodeNet;
import synet.controller.utils.Logger;
import synet.controller.utils.SysUtils;
import synet.controller.utils.XmlUtils;

/**
 * @author mkurdziel
 *
 */
public class MsgAdapterNet extends MsgAdapterBase
{
    private static final String C_STR_USERACTIONS_ROOT = "userActions";

    private static final String TAG = "MsgAdapterNet";

    private static final String C_STR_CATALOG_NODEID = "nodeId";
    private static final String C_STR_CATALOG_LASTUPDATE = "lastUpdate";
    private static final String C_STR_CATALOG_FILENAME = "catalog";
    private static final String C_STR_CATALOG_ACTIONS = "actions";

    private static final String C_STR_INFO_STARTTIME = "startTime";
    private static final String C_STR_INFO_ROOT = "controller";
    private static final String C_STR_INFO_ISRUNNING = "isRunning";

    private static final String C_STR_NODELIST_TYPE = "type";
    private static final String C_STR_NODELIST_ROOT = "nodes";
    private static final String C_STR_NODELIST_NODE = "node";
    private static final String C_STR_NODELIST_FILENAME = "nodelist";
    private static final String C_STR_NODELIST_LASTUPDATE = "lastUpdate";
    
    private static final String C_STR_USERACTIONS_FILENAME = C_STR_USERACTIONS_ROOT;

    private static final String C_STR_INFO_FILENAME = "info";
    private static final String C_STR_ACTIONREQUEST_FILENAME = "actionRequest";
    private static final String C_STR_SUFFIX = ".xml";
    private static final String C_STR_NAME_QUERY = "queries";

    private static final String C_STR_NAME_DEBUG_FILENAME = "DEBUG.flg";



    private String m_strCmdLocation = null;
    private String m_strQueryLocation = null;
    private String m_strSeparator = null;
    private boolean m_isRunning = false;
    private File m_netFileDir = null;
    private boolean m_isDebug = false;

    private WatchService m_watchService = null;
    private WatchKey m_watchKey = null;

    private MsgAdapterNetFileThread m_fileThread;
    private DateTimeFormatter m_dateTimeFormatter = SysUtils.getDateTimeFormatter();

    /**
     * Default constructor
     */
    public MsgAdapterNet(String p_baseDir)
    {
        m_strSeparator = SysUtils.getSeparatorString();
        m_strCmdLocation = p_baseDir;
        m_strQueryLocation = String.format("%s%s%s", p_baseDir, m_strSeparator, C_STR_NAME_QUERY);
        m_fileThread = new MsgAdapterNetFileThread();
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#startAdapater()
     */
    @Override
    public boolean startAdapater()
    {
        loadConfiguration();

        m_isRunning = SysUtils.checkAndCreatePath(m_strCmdLocation);
        m_isRunning &= SysUtils.checkAndCreatePath(m_strQueryLocation);

        m_isDebug = checkDebugFile();

        if (m_isDebug)
        {
            Logger.i(TAG, "using debug mode. Persisting query files");
        }


        if (m_isRunning)
        {
            m_netFileDir = new File(m_strCmdLocation);

            startWatchService();

            // Read in the persistence files and populate the system
            readPersistenceFiles();

            writeSystemInfo();
        }

        return m_isRunning;
    }

    /**
     * @return true if the debug file was found
     */
    private boolean checkDebugFile()
    {
        File debugFile = new File(m_strQueryLocation + m_strSeparator + C_STR_NAME_DEBUG_FILENAME);
        return debugFile.exists();
    }

    /**
     * Load any variables from the configuration
     */
    private void loadConfiguration() {

    }

    /**
     * Save any variables for the configuration
     */
    private void saveConfiguration()
    {
        getConfiguration().addProperty("queryDir", m_strQueryLocation);
        getConfiguration().addProperty("cmdDir", m_strCmdLocation);
    }

    /**
     * Start the filesystem watch service to watch for queries
     */
    private void startWatchService() 
    { 
        Logger.i(TAG, "starting file watch service");

        if (!(SysUtils.isMac() || SysUtils.isWindows()))
        {
            Bootstrapper.setForcePollingEnabled(true);
        }

        m_watchService = FileSystems.getDefault().newWatchService();
        Path watchedPath = Paths.get(m_strQueryLocation);

        try {
            m_watchKey = watchedPath.register(m_watchService, StandardWatchEventKind.ENTRY_CREATE);
        } catch (UnsupportedOperationException e){
            Logger.e(TAG, "file watching not supported", e);
            return;
        } catch (IOException e){
            Logger.e(TAG, "IOException occured", e);
            return;
        }
        // Start the watch thread
        m_fileThread.start();
    }

    /**
     * Stop the filesystem watch service
     */
    private void stopWatchService() 
    {
        if (m_watchService != null)
        {
            try {
                m_watchKey.cancel();
                m_watchService.close();
            } catch (IOException e) {
                Logger.e(TAG, "IOException closing watch service", e);
            }
        }
        Logger.i(TAG, "stopped file watch service");
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#stopAdapter()
     */
    @Override
    public void stopAdapter()
    {
        Logger.i(TAG, "stopping adapter");

        m_isRunning = false;

        stopWatchService();

        writeSystemInfo();

        m_netFileDir = null;

        saveConfiguration();
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#getNodeClass()
     */
    @Override
    public Class<?> getNodeClass()
    {
        return NodeNet.class;
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#getNodeClass()
     */
    @Override
    public String getNodeTypeIdentifierString()
    {
        return NodeNet.NODE_TYPE_STRING;
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#sendMessage(synet.controller.messaging.messages.MsgTx)
     */
    @Override
    public void sendMessage(MsgTx p_msg)
    {
        Logger.d(TAG, "sendMessage is not yet implemented");
    }

    /* (non-Javadoc)
     * @see synet.controller.messaging.MsgAdapterBase#broadcastMessage(synet.controller.messaging.messages.MsgTx)
     */
    @Override
    public void broadcastMessage(MsgTx p_msg)
    {
        switch(p_msg.getAPI())
        {
            case MsgConstants.SN_API_SYSTEM_NODELIST_TRANSMIT:
                writeNodeList((MsgSystemNodelistTransmit) p_msg);
                break;
            case MsgConstants.SN_API_SYSTEM_CATALOG_TRANSMIT:
                writeCatalog((MsgSystemCatalogTransmit) p_msg);
                break;
            case MsgConstants.SN_API_SYSTEM_USERACTIONLIST_TRANSMIT:
                writeUserActions((MsgSystemUserActionListTransmit) p_msg);
                break;
        }
    }


    /**
     * Write the node catalog from the message
     * 
     * @param p_msg
     */
    private void writeCatalog(MsgSystemCatalogTransmit p_msg)
    {
        writeCatalog(p_msg.getNode());
    }

    /**
     * Write the catalog for the node
     * 
     * <catalog nodeId="0013A20040303382" lastUpdate="2012-02-20T16:27:09.619-07:00">
     * <function id="1" name="FadeLights" returnType="0" description="Fade all lights">
     * <parameters>  
     * <parameter id="1" name="Red" dataType="2" validationType="1" minValue="0" maxValue="4095" maxStringLen="0"/>
     * <parameter id="2" name="Green" dataType="2" validationType="1" minValue="0" maxValue="4095" maxStringLen="0"/>
     * <parameter id="3" name="Blue" dataType="2" validationType="1" minValue="0" maxValue="4095" maxStringLen="0"/>
     * <parameter id="4" name="FadeTime" dataType="2" validationType="1" minValue="0" maxValue="10000" maxStringLen="0"/>
     * <parameters>
     * </function>
     *
     * 
     * @param p_msg
     */
    private void writeCatalog(NodeBase p_node)
    {
        if(m_isRunning && p_node != null)
        {
            Element root = new Element(C_STR_CATALOG_FILENAME);

            // node ID attribute
            Attribute nodeId = 
                new Attribute(C_STR_CATALOG_NODEID, p_node.getNodeIdStringNoX());
            root.addAttribute(nodeId);

            // Last update attribute
            Attribute lastUpdate = 
                new Attribute(C_STR_CATALOG_LASTUPDATE, 
                        (new DateTime()).toString(m_dateTimeFormatter));
            root.addAttribute(lastUpdate);

            // Create the sub element for the functions
            Element actionsElement = new Element(C_STR_CATALOG_ACTIONS);
            root.appendChild(actionsElement);

            // Add each node as an element
            for(INodeAction nodeAction : p_node.getNodeActions())
            {
                Element nodeElement = nodeAction.serialize();

                Logger.d(TAG, p_node.getDescString() + " serializing parameters for " + nodeAction.getName() + " num Parameters " + nodeAction.getParameterCount());
                for(INodeParameter parameter : nodeAction.getParameters())
                {
                    nodeElement.appendChild(parameter.serialize());
                }
                actionsElement.appendChild(nodeElement);
            }

            Document doc = new Document(root);

            writeXmlFile(doc, String.format("%s_%s", C_STR_CATALOG_FILENAME, p_node.getNodeIdStringNoX()));
        }
    }

    /**
     * Write the node list from the message
     * 
     * @param p_msg
     */
    private void writeNodeList(MsgSystemNodelistTransmit p_msg)
    {
        writeNodeList(p_msg.getNodeList());
    }
    

    /**
     * Write the user actions from the message
     * @param p_msg
     */
    private void writeUserActions(MsgSystemUserActionListTransmit p_msg)
    {
        writeUserActions(p_msg.getUserActions());
    }

    /**
     * Write out the node list
     * 
     * <?xml version="1.0" encoding="ISO-8859-1"?>
     * <nodes lastUpdate="">
     *  <node id="0013A20040303382" status=1 lastSeen="2012-02-20T16:27:09.619-07:00">
     *    <name>Unknown Node</name>
     *    <synetID>a001</synetID>
     *    <manufacturer>aabb</manufacturer>
     *    <profile>1</profile>
     *    <revision>e</revision>
     *  </node>
     *</nodes>
     */
    private void writeNodeList(NodeBase[] p_nodeList)
    {
        Logger.v(TAG, "Writing new node list file with " + p_nodeList.length + " nodes");
        if(m_isRunning && p_nodeList != null)
        {
            Element root = new Element(C_STR_NODELIST_ROOT);
            // Last update attribute
            Attribute lastUpdate = 
                new Attribute(C_STR_NODELIST_LASTUPDATE, 
                        (new DateTime()).toString(m_dateTimeFormatter));
            root.addAttribute(lastUpdate);

            // Add each node as an element
            for(NodeBase node : p_nodeList)

            {
                root.appendChild(node.serialize());
            }

            Document doc = new Document(root);
            System.out.println(root.toXML());
            writeXmlFile(doc, C_STR_NODELIST_FILENAME);
        }
    }
    
    /**
     * Write the user action list
     * 
     * @param p_userActions
     */
    private void writeUserActions(IAction[] p_userActions)
    {
        Logger.v(TAG, "Writing new user action file");
        
        if (m_isRunning)
        {
            Element root = new Element(C_STR_USERACTIONS_ROOT);
            
            // Last update attribute
            Attribute lastUpdate = 
                new Attribute(C_STR_NODELIST_LASTUPDATE, 
                        (new DateTime()).toString(m_dateTimeFormatter));
            root.addAttribute(lastUpdate);
            
            // Add each node as an eleent
            for (IAction action : p_userActions)
            {
                root.appendChild(action.serialize());
            }
            
            Document doc = new Document(root);
            
            writeXmlFile(doc, C_STR_USERACTIONS_FILENAME);
        }
    }
    
    /**
     * Process an incoming user action list and message the rest of the system
     * 
     * @param p_xml
     */
    private void processUserActionList(Document p_xml)
    {
        if (p_xml != null)
        {
            Element root = p_xml.getRootElement();
            if (root.getLocalName().equals(C_STR_USERACTIONS_ROOT))
            {
                // Print out the update time
                Attribute lastUpdate = root.getAttribute(C_STR_NODELIST_LASTUPDATE);
                if (lastUpdate != null)
                {
                    DateTime dtLU = DateTime.parse(lastUpdate.getValue(), m_dateTimeFormatter);
                    Logger.i(TAG, "Loading User Action list file last updated on " + dtLU.toString());
                }

                Elements actionElements = root.getChildElements("action");
                if (actionElements != null)
                {
                    for(int i=0; i<actionElements.size(); ++i)
                    {
                        String type;

                        Element actionElement = actionElements.get(i);
                        type = XmlUtils.getXmlAttributeString(actionElement, "actionType");
                        if (type != null)
                        {
                            // Create the message 
                            MsgSystemUserActionListReceive msg = new MsgSystemUserActionListReceive(
                                    type, 
                                    actionElement);
                            // This is synchronous because we want it 
                            // to be processed immediatly and hold up everything
                            // else
                            msg.setIsSynchronous(true);

                            fireMsgReceivedEvent(msg);
                        }
                        else
                        {
                            Logger.w(TAG, "Node list - cannot process node without a type");
                        }
                    }
                }
                else
                {
                    Logger.w(TAG, "Node list has no nodes");
                }
            }
            else
            {
                Logger.w(TAG,"Node list root is incorrect");
            }
        }
        else
        {
            Logger.w(TAG, "processing null Node list");
        }
    }

    /**
     * Process an incoming node list and message the rest of the system
     * 
     * @param p_xml
     */
    private void processNodeList(Document p_xml)
    {
        if (p_xml != null)
        {
            Element root = p_xml.getRootElement();
            if (root.getLocalName().compareTo(C_STR_NODELIST_ROOT) == 0)
            {
                // Print out the update time
                Attribute lastUpdate = root.getAttribute(C_STR_NODELIST_LASTUPDATE);
                if (lastUpdate != null)
                {
                    DateTime dtLU = DateTime.parse(lastUpdate.getValue(), m_dateTimeFormatter);
                    Logger.i(TAG, "Loading Node list file last updated on " + dtLU.toString());
                }

                Elements nodeElements = root.getChildElements(C_STR_NODELIST_NODE);
                if (nodeElements != null)
                {
                    for(int i=0; i<nodeElements.size(); ++i)
                    {
                        Logger.v(TAG, "Decoding node element");
                    
                        String type;


                        Element nodeElement = nodeElements.get(i);
                        type = XmlUtils.getXmlAttributeString(nodeElement, C_STR_NODELIST_TYPE);
                        if (type != null)
                        {
                            // Create the message 
                            MsgSystemNodelistReceive msg = new MsgSystemNodelistReceive(
                                    type, 
                                    nodeElement);
                            // This is synchronous because we want it 
                            // to be processed immediatly and hold up everything
                            // else
                            msg.setIsSynchronous(true);

                            fireMsgReceivedEvent(msg);
                        }
                        else
                        {
                            Logger.w(TAG, "Node list - cannot process node without a type");
                        }
                    }
                }
                else
                {
                    Logger.w(TAG, "Node list has no nodes");
                }
            }
            else
            {
                Logger.w(TAG,"Node list root is incorrect");
            }
        }
        else
        {
            Logger.w(TAG, "processing null Node list");
        }
    }


    /**
     * Process an incoming catalog
     * 
     * @param p_xml
     */
    private void processCatalog(Document p_xml)
    {
        if (p_xml != null)
        {
            Element root = p_xml.getRootElement();
            if (root.getLocalName().compareTo(C_STR_CATALOG_FILENAME) == 0)
            {
                // Print out the update time
                DateTime dateTimeLastUpdate = XmlUtils.getXmlAttributeDateTime(root, C_STR_CATALOG_LASTUPDATE);
                Logger.i(TAG, "Loading Node list file last updated on " + dateTimeLastUpdate.toString());

                Long nodeId = XmlUtils.getXmlAttributeLong(root, C_STR_CATALOG_NODEID);

                Element actionsElement = root.getFirstChildElement(C_STR_CATALOG_ACTIONS);
                if (actionsElement != null)
                {
                    Elements actionElements = actionsElement.getChildElements();
                    for( int i=0; i<actionElements.size(); ++i)
                    {
                        Element actionElement = actionElements.get(i);

                        // Deserialize the new action
                        NodeAction newAction = new NodeAction(actionElement, nodeId, i+1);

                        // Add it to the action manager
                        ActionManager.getInstance().addNodeAction(newAction);

                        // Parse out all the parameters
                        Elements paramElements = actionElement.getChildElements();
                        for( int k=0; k<paramElements.size(); ++k)
                        {
                            Element paramElement = paramElements.get(k);
                            NodeParameter newParam = new NodeParameter(
                                    paramElement,
                                    newAction.getActionId(),
                                    newAction.getNodeId(),
                                    i+1,
                                    k+1 );

                            ActionManager.getInstance().addNodeParameter(newParam);
                        }

                        // Count the parameters we've added
                        newAction.updateNumParameters();

                    }
                }
                else
                {
                    Logger.w(TAG, "Catalog list has no nodes");
                }
            }
            else
            {
                Logger.w(TAG,"Catalog list root is incorrect");
            }
        }
        else
        {
            Logger.w(TAG, "processing null Catalog");
        }
    }



    /**
     * Write the XML file out and clean up old ones
     * 
     * @param p_doc
     * @param p_prefix
     */
    private synchronized void writeXmlFile(Document p_doc, String p_prefix)
    {
        try
        {
            FileOutputStream fos = new FileOutputStream(
                    String.format("%s/%s_%s%s", 
                            m_strCmdLocation,
                            p_prefix,
                            (new DateTime()).toString(m_dateTimeFormatter),
                            C_STR_SUFFIX));

            // Using a serializer with indention set to 2 spaces,
            // write the XML document to the file
            Serializer output = new Serializer(fos, "ISO-8859-1");
            output.setIndent(2);
            output.write(p_doc);
            fos.close();
        }
        catch (FileNotFoundException e)
        {
            Logger.w(TAG, "could not write file", e);
        }
        catch (UnsupportedEncodingException e)
        {
            Logger.w(TAG, "unsupported encoding exception", e);
        }
        catch (IOException e)
        {
            Logger.w(TAG, "IO Exception", e);
        }

        cleanOldFiles(p_prefix);
    }

    /**
     *  Write any system information to the net directory
     */
    private void writeSystemInfo()
    {
        Logger.v(TAG, "writing system info");

        Element root = new Element(C_STR_INFO_ROOT);
        root.addAttribute(new Attribute(C_STR_INFO_STARTTIME, 
                (new DateTime()).toString(m_dateTimeFormatter)));
        root.addAttribute(new Attribute(C_STR_INFO_ISRUNNING, String.valueOf(m_isRunning)));

        Document doc = new Document(root);

        writeXmlFile(doc, C_STR_INFO_FILENAME);
    }

    /**
     * Go through the net directory and clean out any old files based on 
     * the passed in prefix. Only the newest file with the particular prefix
     * will be kept and the others removed
     * 
     * @param p_prefix
     */
    private void cleanOldFiles(final String p_prefix)
    {
        // Make sure the directory is still open
        if (m_netFileDir != null) 
        {
            // This filter is for the prefix
            FileFilter fileFilter = new FileFilter() {
                public boolean accept(File file) {
                    return file.getName().startsWith(p_prefix);
                }
            };

            // Get the file list
            File[] filteredFiles = m_netFileDir.listFiles(fileFilter);

            // Make sure we have old files and keep the newest
            if (filteredFiles.length > 1)
            {
                // Sort by the newest modified
                Arrays.sort(filteredFiles, NameFileComparator.NAME_REVERSE);

                // The reverse sort leaves the zero element as the newest file.
                // Delete the rest
                for(int i=1; i<filteredFiles.length; i++)
                {
                    if(!filteredFiles[i].delete())
                    {
                        Logger.e(TAG, "could not delete old file: " + filteredFiles[i].getName());
                    }
                    else
                    {
                        Logger.v(TAG, "Deleted file: " + filteredFiles[i].getName());
                    }
                }
            }
        }
    }

    /**
     * Read the net file directory and process any persistence files
     */
    private void readPersistenceFiles()
    {
        // Read through the net file directory and grab any
        // persistence files
        if (m_netFileDir != null)
        {
            File[] perFiles = m_netFileDir.listFiles();

            // Delay catalog processing until after node lists are processed
            ArrayList<String> catalogs = new ArrayList<String>();
            // Delay user actions until after catalogs are processed
            String userActionFile = null;

            for (File perFile : perFiles)
            {

                if (perFile.getName().contains(C_STR_CATALOG_FILENAME))
                {
                    Logger.v(TAG, "reading catalog file: " + perFile);
                    catalogs.add(perFile.getAbsolutePath());
                }
                else if (perFile.getName().contains(C_STR_NODELIST_FILENAME))
                {
                    Logger.v(TAG, "reading nodelist file: " + perFile);
                    processNodeList(readXML(perFile.getAbsolutePath()));
                }
                else if (perFile.getName().contains(C_STR_USERACTIONS_FILENAME))
                {
                    Logger.v(TAG, "reading user action file: " + perFile);
                    userActionFile = perFile.getAbsolutePath();
                }
            }

            // Now that all the nodes are in the system, we can 
            // process the catalogs
            for(String catalog : catalogs)
            {
                processCatalog(readXML(catalog));
            }
            // Now that the catalogs are in the system, we can 
            // process the user actions that link to them
            if (userActionFile != null)
            {
                processUserActionList(readXML(userActionFile));
            }
        }
        else
        {
            Logger.w(TAG, "Could not read net directory to get persistence files");
        }
    }

    /**
     * Read in XML file and return an XML document
     * 
     * @param p_fileName
     * @return
     */
    private Document readXML(String p_fileName)
    {
        // Make sure we're handing an xml file here
        if (p_fileName.endsWith(C_STR_SUFFIX))
        {
            Builder parser = new Builder();
            File inFile = new File(p_fileName);
            if (inFile.exists())
            {
                try {
                    Document doc = parser.build(inFile);
                    return doc;
                } catch (ValidityException e) {
                    Logger.e(TAG, "validity exception while reading file: " + p_fileName, e);
                } catch (ParsingException e) {
                    Logger.e(TAG, "paring exception while reading file: " + p_fileName, e);
                } catch (IOException e) {
                    Logger.e(TAG, "IOException while reading file: " + p_fileName, e);
                }
            }
            else
            {
                Logger.e(TAG, "trying to read file that doesn't exist: " + p_fileName);
            }
        }	
        return null;
    }

    /**
     * Thread to handle file changes
     */
    private class MsgAdapterNetFileThread extends Thread
    {
        private static final String TAG = "MsgAdapterNetFileThread";
        private static final int C_I_ACTION_TIMEOUT = 5000;
        private static final String C_STR_REQUEST_PARAM_VALUE = "value";
        private static final String C_STR_REQUEST_PARAM_ID = "id";
        private static final String C_STR_REQUEST_PARAMETER = "parameter";
        private static final String C_STR_REQUEST_ACTION_ID = "actionId";
        private static final String C_STR_REQUEST_ROOT = "actionRequest";

        @Override
        public void run() {
            Logger.i(TAG, "Starting");
            super.run();

            // Loop while the adapter is running
            while(m_isRunning)
            {
                // take() will block until a file has been created/deleted
                WatchKey signalledKey;
                try {
                    signalledKey = m_watchService.take();
                } catch (InterruptedException ix){
                    // we'll ignore being interrupted
                    continue;
                } catch (ClosedWatchServiceException cwse){
                    Logger.d(TAG,"watch service closed, terminating.");
                    break;
                }

                // get list of events from key
                List<WatchEvent<?>> list = signalledKey.pollEvents();

                // VERY IMPORTANT! call reset() AFTER pollEvents() to allow the
                // key to be reported again by the watch service
                signalledKey.reset();

                // we'll simply print what has happened; real applications
                // will do something more sensible here
                for(WatchEvent<?> e : list){
                    if(e.kind() == StandardWatchEventKind.ENTRY_CREATE){
                        String fileName = ((Path)e.context()).toString();
                        if (fileName.startsWith(C_STR_ACTIONREQUEST_FILENAME) &&
                                fileName.endsWith(C_STR_SUFFIX))
                        {
                            processQueryFile(fileName);
                        }
                    } else if(e.kind() == StandardWatchEventKind.OVERFLOW){
                        Logger.e(TAG, "OVERFLOW: more changes happened than we could retreive");
                    }
                }
            }

            Logger.i(TAG, "Exiting thread: " + m_isRunning);
        }

        /**
         * Process an incoming XML file
         * 
         * @param p_fileName
         */
        private void processQueryFile(String p_fileName)
        {
            Logger.v(TAG, "processing query file: " + p_fileName);

            String strFileName = m_strQueryLocation + m_strSeparator + p_fileName;

            Document queryDoc = readXML(strFileName);

            processQuery(queryDoc, p_fileName);

            // If we are not in debug mode, delete the file
            if(false == m_isDebug)
            {
                File queryFile = new File(strFileName);
                if (queryFile.exists())
                {
                    try
                    {
                        FileUtils.forceDelete(queryFile);
                        Logger.v(TAG, "deleting query file " + queryFile.getAbsolutePath());
                    } 
                    catch (IOException e)
                    {
                        Logger.w(TAG, "unable to delete " + strFileName, e);
                    }
                }
                else
                {
                    Logger.w(TAG, "query file does not exist to delete: " + strFileName);
                }
            }
        }

        /**
         * Process a query formatted in XML
         * 
         * @param p_xmlDoc
         * @param pFileName 
         */
        private void processQuery(Document p_xmlDoc, String p_fileName) {

            String[] fileParts = p_fileName.split("_");

            Element root = p_xmlDoc.getRootElement();
            if (root.getLocalName().compareTo(C_STR_REQUEST_ROOT) == 0)
            {
                short actionId = XmlUtils.getXmlAttributeShort10(root, C_STR_REQUEST_ACTION_ID);

                if (actionId != 0)
                {
                    Elements paramElements = root.getChildElements(C_STR_REQUEST_PARAMETER);

                    IAction action = ActionManager.getInstance().getAction(actionId);

                    if (action != null)
                    {
                        for (int i=0; i<paramElements.size(); ++i)
                        {
                            short paramId = XmlUtils.getXmlAttributeShort10(paramElements.get(i), C_STR_REQUEST_PARAM_ID);
                            String value = XmlUtils.getXmlAttributeString(paramElements.get(i), C_STR_REQUEST_PARAM_VALUE);

                            ParameterBase param = ActionManager.getInstance().getParameter(paramId);

                            if (param != null)
                            {
                                param.setValue(value);

                            }
                            else
                            {
                                Logger.w(TAG, "null parameter for id " + String.format("%d", paramId));
                            }
                        }
                    }
                    else 
                    {
                        Logger.e(TAG, "null action for id " + String.format("0x%x", actionId));
                    }

                    ActionRequest request = ActionManager.getInstance().executeAction(actionId, C_I_ACTION_TIMEOUT);
                    if(request.waitForExecution())
                    {
                        writeQueryResponse(fileParts[1], fileParts[2], action.getStringReturnValue(), 0, "");
                    }
                    else
                    {
                        writeQueryResponse(fileParts[1], fileParts[2], "", 1, request.getErrorMessage());
                    }
                }
                else
                {
                    Logger.w(TAG, "request has a zero Action ID");
                }
            }

            Logger.v(TAG, "query type: " + root.getLocalName());
        }



        /**
         * Write the query response XML file
         * 
         * @param p_actionId
         * @param p_requestId
         * @param p_stringReturnValue
         * @param p_errorCode
         * @param p_errorMsg
         */
        private void writeQueryResponse(
                String p_actionId, 
                String p_requestId, 
                String p_stringReturnValue,
                int p_errorCode, 
                String p_errorMsg) 
        {
            Element root = new Element("actionResponse");
            root.addAttribute(new Attribute("actionId", p_actionId));
            root.addAttribute(new Attribute("timeResponded", DateTime.now().toString(SysUtils.getDateTimeFormatter())));
            Element returnedElement = new Element("returned");
            returnedElement.addAttribute(new Attribute("value", p_stringReturnValue));
            returnedElement.addAttribute(new Attribute("errorCode", String.valueOf(p_errorCode)));
            returnedElement.addAttribute(new Attribute("errorMsg", p_errorMsg));
            root.appendChild(returnedElement);

            Document doc = new Document(root);

            try
            {
                FileOutputStream fos = new FileOutputStream(
                        String.format("%s/%s/actionResponse_%s_%s", 
                                m_strCmdLocation,
                                C_STR_NAME_QUERY,
                                p_actionId,
                                p_requestId,
                                C_STR_SUFFIX));

                // Using a serializer with indention set to 2 spaces,
                // write the XML document to the file
                Serializer output = new Serializer(fos, "ISO-8859-1");
                output.setIndent(2);
                output.write(doc);
                fos.close();
            }
            catch (FileNotFoundException e)
            {
                Logger.w(TAG, "could not write file", e);
            }
            catch (UnsupportedEncodingException e)
            {
                Logger.w(TAG, "unsupported encoding exception", e);
            }
            catch (IOException e)
            {
                Logger.w(TAG, "IO Exception", e);
            }
        }
    }

    @Override
    public void processSystemNodeListReceive(MsgSystemNodelistReceive pMsg) {

    }
}
