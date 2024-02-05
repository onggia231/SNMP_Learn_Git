package telsoft.core.clf.snmp.thread;

import com.telsoft.thread.ManageableThread;
import com.telsoft.thread.ParameterType;
import com.telsoft.thread.ParameterUtil;
import com.telsoft.thread.ThreadConstant;
import com.telsoft.util.AppException;
import java.net.SocketTimeoutException;
import java.util.Vector;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import telsoft.core.clf.snmp.SNMPCLFTrapSender;

public class SNMPCLF extends ManageableThread {
    private Logger logger = LoggerFactory.getLogger(SNMPCLF.class);
    @Getter
    private String community;
    @Getter
    private String trapOid;
    @Getter
    private String ipAddress;
    @Getter
    private int port;
    @Getter
    private volatile VTrap vTrap;
    public VTrap getVTrap() {
        return vTrap;
    }

    public void beforeSession() throws Exception {
        logMonitor("Start processing");
        super.beforeSession();
    }

    public void afterSession() throws Exception {
        super.afterSession();
        logMonitor("Stopped processing");
    }

    @Override
    protected void processSession() throws Exception {
        try {
            logMonitor("Started Snmp!");
            while (miThreadCommand != ThreadConstant.THREAD_STOP) {
                try {
                    SNMPCLFTrapSender snmpclfTrapSender = new SNMPCLFTrapSender(this);
                    new Thread(snmpclfTrapSender).start();
                } catch (SocketTimeoutException ignored) {
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.getMessage());
            logMonitor(ex.getMessage());
        } finally {
            logMonitor("Stopped SNMP");
        }
    }

    public Vector getParameterDefinition() {
        Vector vtReturn = new Vector();
        vtReturn.add(createParameter("Community", "", ParameterType.PARAM_TEXTBOX_MAX, "9999999990", "Community"));
        vtReturn.add(createParameter("TrapOid", "", ParameterType.PARAM_TEXTBOX_MAX, "9999999990", "TrapOid"));
        vtReturn.add(createParameter("IpAddress", "", ParameterType.PARAM_TEXTBOX_MAX, "9999999990", "IpAddress"));
        vtReturn.add(createParameter("Port", "", ParameterType.PARAM_TEXTBOX_MASK, "9999", "Port", ""));
        vtReturn.add(ParameterUtil.createParameter("VTrap", "", 4, VTrap.class, "VTrap"));
        vtReturn.addAll(super.getParameterDefinition());
        return vtReturn;
    }

    public void fillParameter() throws AppException {
        community = loadString("Community");
        trapOid = loadString("TrapOid");
        ipAddress = loadString("IpAddress");
        port = loadUnsignedInteger("Port");
        String verTrap = loadString("VTrap");
        vTrap = VTrap.valueOf(verTrap);
        super.fillParameter();
    }

    public enum VTrap {
        V1Trap, V2Trap;
    }

}
