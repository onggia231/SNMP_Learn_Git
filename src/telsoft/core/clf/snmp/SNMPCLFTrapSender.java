package telsoft.core.clf.snmp;

import com.telsoft.thread.ManageableThread;
import com.telsoft.thread.ThreadConstant;
import java.io.IOException;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import telsoft.core.clf.snmp.thread.SNMPCLF;

public class SNMPCLFTrapSender extends Thread {

  @Getter
  @Setter
  private SNMPCLF threadManager;

    public static final String community = "public";

    //  Sending Trap for sysLocation of RFC1213
//    public static final String trapOid = ".1.3.6.1.2.1.1.6";
//    public static final String trapOid = ".1.3.6.1.4.1.1981";
//    public static final String trapOid = ".1.3.6.1.4.1.1981.1.4.3.0";
    public static final String trapOid = ".1.3.6.1.4.1.1";
//    public static final String ipAddress = "127.0.0.1";
    public static final String ipAddress = "10.155.73.231";

    public static final int port = 162;

  public SNMPCLFTrapSender() {
  }

  public SNMPCLFTrapSender(ManageableThread thread) throws IOException {
    threadManager = (SNMPCLF) thread;
  }

    public static void main(String[] args) {
        SNMPCLFTrapSender snmp4JTrap = new SNMPCLFTrapSender();

        /* Sending V1 Trap */
        snmp4JTrap.sendSnmpV1TrapTest();

        /* Sending V2 Trap */
//        snmp4JTrap.sendSnmpV2Trap();
    }

  public void run() {
    try {
      while (threadManager.miThreadCommand != ThreadConstant.THREAD_STOP) {
//        threadManager.sleep(100000,1000);
        snmpCLFTrapSender();
//        Thread.sleep(threadManager.miDelayTime*10000);
        Thread.sleep(getThreadManager().miDelayTime*10000L);
//        Thread.sleep(10000L);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
//    catch (InterruptedException e) {
//      throw new RuntimeException(e);
//    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      closeSnmpTrap();
    }
  }

  public void closeSnmpTrap() {
    threadManager.logMonitor("Close channel");
  }

  public void snmpCLFTrapSender() throws IOException {
    SNMPCLFTrapSender snmp4JTrap = new SNMPCLFTrapSender(threadManager);
    if (String.valueOf(threadManager.getVTrap()).equals("V1Trap")) {
      /* Sending V1 Trap */
      snmp4JTrap.sendSnmpV1Trap();
    } else {
      /* Sending V2 Trap */
      snmp4JTrap.sendSnmpV2Trap();
    }
  }

  public void sendSnmpV1TrapTest()
  {
    try
    {
      //Create Transport Mapping
      TransportMapping transport = new DefaultUdpTransportMapping();
      transport.listen();


      //Create Target
      CommunityTarget comtarget = new CommunityTarget();
      comtarget.setCommunity(new OctetString(community));
      comtarget.setVersion(SnmpConstants.version1);
      comtarget.setAddress(new UdpAddress(ipAddress + "/" + port));
      comtarget.setRetries(2);
      comtarget.setTimeout(5000);

      //Create PDU for V1
      PDUv1 pdu = new PDUv1();
      pdu.setType(PDU.V1TRAP);
      pdu.setEnterprise(new OID(trapOid));
      pdu.setGenericTrap(PDUv1.ENTERPRISE_SPECIFIC);
      pdu.setSpecificTrap(2);
      pdu.setAgentAddress(new IpAddress(ipAddress));

      //Send the PDU
      Snmp snmp = new Snmp(transport);
      System.out.println("Sending V1 Trap to " + ipAddress + " on Port " + port);
      snmp.send(pdu, comtarget);
      snmp.close();
    }
    catch (Exception e)
    {
      System.err.println("Error in Sending V1 Trap to " + ipAddress + " on Port " + port);
      System.err.println("Exception Message = " + e.getMessage());
    }
  }

  public void sendSnmpV1Trap() {
    try {
      //Create Transport Mapping
      TransportMapping transport = new DefaultUdpTransportMapping();
//      TransportMapping transport = new DefaultTcpTransportMapping();
      transport.listen();

      //Create Target
      CommunityTarget comtarget = new CommunityTarget();
      comtarget.setCommunity(new OctetString(threadManager.getCommunity()));
      comtarget.setVersion(SnmpConstants.version1);
      comtarget.setAddress(
          new UdpAddress(threadManager.getIpAddress() + "/" + threadManager.getPort()));
      comtarget.setRetries(1);
      comtarget.setTimeout(5000);

      //Create PDU for V1
      PDUv1 pdu = new PDUv1();
      pdu.setType(PDU.V1TRAP);
      pdu.setEnterprise(new OID(threadManager.getTrapOid()));
      pdu.setGenericTrap(PDUv1.ENTERPRISE_SPECIFIC);
      pdu.setSpecificTrap(1);
      pdu.setAgentAddress(new IpAddress(threadManager.getIpAddress()));

      //Send the PDU
      Snmp snmp = new Snmp(transport);
      System.out.println("Sending V1 Trap to " + threadManager.getIpAddress() + " on Port "
          + threadManager.getPort());
      snmp.send(pdu, comtarget);
      snmp.close();
    } catch (Exception e) {
      System.err.println("Error in Sending V1 Trap to " + threadManager.getIpAddress() + " on Port "
          + threadManager.getPort());
      System.err.println("Exception Message = " + e.getMessage());
    }
  }

  public void sendSnmpV2Trap() {
    try {
      //Create Transport Mapping
      TransportMapping transport = new DefaultUdpTransportMapping();
//      TransportMapping transport = new DefaultTcpTransportMapping();
      transport.listen();

      //Create Target
      CommunityTarget comtarget = new CommunityTarget();
      comtarget.setCommunity(new OctetString(threadManager.getCommunity()));
      comtarget.setVersion(SnmpConstants.version2c);
      comtarget.setAddress(
          new UdpAddress(threadManager.getIpAddress() + "/" + threadManager.getPort()));
      comtarget.setRetries(2);
      comtarget.setTimeout(5000);

      //Create PDU for V2
      PDU pdu = new PDU();
      // need to specify the system up time
      pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new OctetString(new Date().toString())));
      pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(threadManager.getTrapOid())));
      pdu.add(new VariableBinding(SnmpConstants.snmpTrapAddress,
          new IpAddress(threadManager.getIpAddress())));
      // variable binding for Enterprise Specific objects, Severity (should be defined in MIB file)
      pdu.add(new VariableBinding(new OID(threadManager.getTrapOid()), new OctetString("Major")));
      pdu.setType(PDU.NOTIFICATION);

      //Send the PDU
      Snmp snmp = new Snmp(transport);
      System.out.println("Sending V2 Trap to " + threadManager.getIpAddress() + " on Port "
          + threadManager.getPort());
      snmp.send(pdu, comtarget);
      snmp.close();
    } catch (Exception e) {
      System.err.println("Error in Sending V2 Trap to " + threadManager.getIpAddress() + " on Port "
          + threadManager.getPort());
      System.err.println("Exception Message = " + e.getMessage());
    }
  }
}
