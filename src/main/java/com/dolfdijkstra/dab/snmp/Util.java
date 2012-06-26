package com.dolfdijkstra.dab.snmp;

import org.snmp4j.PDU;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

public class Util {
    static VariableBinding[] toVariableBinding(String... oid) {
        VariableBinding[] b = new VariableBinding[oid.length];
        for (int i = 0; i < oid.length; i++) {
            b[i] = new VariableBinding(new OID(oid[i]));
        }
        return b;
    }

    public static PDU toPDU(String... oid) {
        PDU pdu = new PDU();

        pdu.addAll(toVariableBinding(oid));
        pdu.setType(PDU.GET);
        return pdu;
    }

    public static PDU toPDU(String[][] map) {
        PDU pdu = new PDU();

        pdu.addAll(toVariableBindingArray(map));
        pdu.setType(PDU.GET);
        return pdu;
    }

    public static VariableBinding[] toVariableBindingArray(String[][] map) {
        VariableBinding[] bindings = new VariableBinding[map.length];
        for (int i = 0; i < map.length; i++) {
            bindings[i] = new VariableBinding(new OID(map[i][1]));
        }
        return bindings;
    }

}
