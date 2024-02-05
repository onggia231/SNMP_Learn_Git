package telsoft.core.clf.snmp;

import java.io.IOException;

public class SnmpTest {

  public static void main(String[] args) throws IOException {

    SNMPCLFTrapSender snmpclfTrapSender = new SNMPCLFTrapSender();
    snmpclfTrapSender.snmpCLFTrapSender();

  }

}
