package telsoft.core.clf;


import com.k.log.Loggable;
import com.telsoft.dictionary.Dictionary;
import com.telsoft.dictionary.DictionaryNode;
import com.telsoft.thread.*;
import com.telsoft.util.AuthenticateInterface;
import com.telsoft.util.Global;
import com.telsoft.util.LogInterface;
import com.telsoft.util.StringUtil;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.net.ServerSocket;
import java.sql.Connection;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.Vector;

public class TaskManager extends ThreadManager implements ProcessorListener, Loggable {
    public static TaskManager INSTANCE;
    private Dictionary config = new Dictionary();
    private boolean mbDebug = false;
    private AuthenticateInterface authenticator = null;
    private String mstrAppName;

    public TaskManager() {
        super(false);
    }

    public static Vector getParameterList(DictionaryNode nd, String str) {
        Vector vtReturn = new Vector();
        if (nd != null && nd.getChildList() != null) {
            Vector vt = nd.getChildList();

            for (int iIndex = 0; iIndex < vt.size(); ++iIndex) {
                DictionaryNode ndChild = (DictionaryNode) vt.elementAt(iIndex);
                String strChildName = StringUtil.nvl(ndChild.mstrName, "");
                boolean hasChild = ndChild.getChildList() != null && ndChild.getChildList().size() > 0;
                Vector vtRow;
                if (str.equals("")) {
                    if (!hasChild) {
                        vtRow = new Vector();
                        vtRow.addElement(strChildName);
                        vtRow.addElement(StringUtil.nvl(ndChild.mstrValue, ""));
                        vtReturn.addElement(vtRow);
                    }

                    vtReturn.addAll(getParameterList(ndChild, strChildName));
                } else {
                    if (!hasChild) {
                        vtRow = new Vector();
                        vtRow.addElement(str + "." + strChildName);
                        vtRow.addElement(StringUtil.nvl(ndChild.mstrValue, ""));
                        vtReturn.addElement(vtRow);
                    }

                    vtReturn.addAll(getParameterList(ndChild, str + "." + strChildName));
                }
            }
        }

        return vtReturn;
    }

    public static void main(String[] args) {
        try {
            String log4jConfigFile = System.getProperty("user.dir") + File.separator + "configuration/log4j2.xml";
            ConfigurationSource source = new ConfigurationSource(new FileInputStream(log4jConfigFile));
            Configurator.initialize(ClassLoader.getSystemClassLoader(), source);
            INSTANCE = new TaskManager();
            INSTANCE.open();
            INSTANCE.run();
        } catch (Throwable var3) {
            Logger log = LoggerFactory.getLogger("MAIN");
            log.error("FATAL error occured, system is interrupted", var3);

            System.exit(-1);

        }

    }

    public ManageableThread getThreadByClass(Class c) {
        for (int iIndex = 0; iIndex < this.mvtThread.size(); ++iIndex) {
            ManageableThread thread = (ManageableThread) this.mvtThread.get(iIndex);
            if (thread.getClass().getName().equals(c.getName())) {
                return thread;
            }
        }
        return null;


    }
    public void initSystem() throws Exception {

        getLogger().debug("Loading config file");

        this.config = new Dictionary("configuration/server.txt");
        getLogger().debug("Loading parameters");
        String strAppName = StringUtil.nvl(this.getConfig().getString("AppName"), "");
        this.setAppName(strAppName);

        boolean var2 = true;

        int iPort;
        try {
            iPort = Integer.parseInt(this.getConfig().getString("PortID"));
        } catch (Exception var4) {
            throw new Exception("Error load PortID parameter", var4);
        }

        this.authenticator = new FileAuthenticator();
        getLogger().debug("Init eir components");
        this.init(this.getConfig());
        getLogger().debug("Seting up others");
        this.getThreadListers().clear();
        this.getThreadListers().add(new FileThreadLister2("configuration/thread/"));
        this.setProcessorListener(this);
        getLogger().debug("Starting controller");
        ServerSocket serverSocket = new ServerSocket(iPort);
        this.setServerSocket(serverSocket);
        this.threadServer = new ThreadServer(this.getServerSocket(), this, this.createPacketProcessor());
        this.threadServer.start();
        this.threadController = new ThreadController(this);
        this.threadController.start();
        getLogger().debug("Init done");
    }

    private void init(Dictionary config) throws Exception {
        getLogger().debug("Changing timezone");
        int iTimeZoneOffset = Integer.parseInt(config.getString("TimeZone.Offset"));
        String strTimeZoneDescription = config.getString("TimeZone.Description");
        TimeZone.setDefault(new SimpleTimeZone(iTimeZoneOffset, strTimeZoneDescription));
        int iMaxConnectionAllowed = Integer.parseInt(config.getString("MaxConnectionAllowed"));
        if (iMaxConnectionAllowed > 0) {
            this.setMaxConnectionAllowed(iMaxConnectionAllowed);
        }

    }

    public String getAppName() {
        return this.mstrAppName;
    }

    protected void setAppName(String strAppName) {
        this.mstrAppName = strAppName;
        Global.APP_NAME = this.mstrAppName;
    }

    public Connection getConnection() throws Exception {
        return null;
    }

    public String getParameter(String strKey) {
        return this.getConfig().getString(strKey);
    }

    public void setParameter(String strKey, String strValue) throws Exception {
        DictionaryNode nd;
        if (strKey.equals("DefaultDatabase")) {
            nd = this.getConfig().getChild("Connection." + strValue);
            if (nd == null) {
                nd = this.getConfig().getChild("Connection");
                Vector vtConnection = nd.getChildList();
                String strChildList = "";

                for (int iIndex = 0; iIndex < vtConnection.size(); ++iIndex) {
                    strChildList = strChildList + ((DictionaryNode) vtConnection.elementAt(iIndex)).mstrName + ",";
                }

                if (!strChildList.equals("")) {
                    strChildList = strChildList.substring(0, strChildList.length() - 1);
                }

                throw new Exception("Database '" + strValue + "' was not declared. Available database is: " + strChildList);
            }
        }

        nd = this.getConfig().getChild(strKey);
        if (nd == null) {
            throw new Exception("Parameter '" + strKey + "' not found");
        } else {
            nd.mstrValue = StringUtil.nvl(strValue, "");
        }
    }

    public Vector getParameterList() {
        return getParameterList(this.getConfig().mndRoot, "");
    }

    public void onCreate(ThreadProcessor processor) throws Exception {
        processor.log = new LogInterface() {
            public String logHeader(String strModuleName, String strUserName, String strActionType) throws Exception {
                return "";
            }

            public Vector logBeforeUpdate(String strLogID, String strTableName, String strCondition) throws Exception {
                return null;
            }

            public void logAfterUpdate(Vector vtChangeID) throws Exception {
            }

            public void logAfterInsert(String strLogID, String strTableName, String strCondition) throws Exception {
            }

            public void logBeforeDelete(String strLogID, String strTableName, String strCondition) throws Exception {
            }

            public void logColumnChange(String strChangeID, String strColumnName, String strOldValue, String strNewValue) throws Exception {
            }

            public String logTableChange(String strLogID, String strTableName, String strRowID, String strActionType) throws Exception {
                return "";
            }

            public void logModuleAccess(String strModuleName, String strUserID, String strIPAddress) throws Exception {
            }

            public void logLogin(String strUserId, String strIPAddress, String sessionId, String loginStatus, Date loginTime) throws Exception {
            }

            public String logHeader(String strModuleName, String strUserName, String strActionType, String strDescription) throws Exception {
                return "";
            }

            public void logLogin(String strUserId, String strIPAddress, String sessionId, String loginStatus, Date loginTime, String strUsername) throws Exception {
            }
        };
        processor.authenticator = this.authenticator;
    }

    public void run() {
        getLogger().debug("System running");
        super.run();
    }

    public void close() {
        getLogger().debug("Gateway is closing");
        super.close();
        getLogger().debug("Everything done, system go down");
        System.out.println("===== Exit JVM =============================================================");
    }

    public boolean isDebug() {
        return this.mbDebug;
    }

    public Dictionary getConfig() {
        return this.config;
    }

    public void open() throws Exception {
        this.initSystem();
    }

    public void onOpen(ThreadProcessor processor) throws Exception {
    }
}
