package com.dolfdijkstra.dab.snmp;

import org.snmp4j.PDU;

public interface Metric {
    public void init(SnmpGet snmp) throws  Exception;

    PDU getPDU();

    void handleResponse(PDU responsePDU);
    
    int getPollTime();
}