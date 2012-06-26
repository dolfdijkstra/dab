package com.dolfdijkstra.dab.snmp;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SnmpGet implements Runnable {

    private TransportMapping transport;
    private CommunityTarget comtarget;

    private Snmp snmp;
    private Metric[] metrics;

    private AtomicInteger counter = new AtomicInteger();

    public SnmpGet(CommunityTarget comtarget, Metric... metrics) throws IOException {
        this.metrics = metrics;
        this.comtarget = comtarget;
        connect();
    }

    public void initMetrics() {

        for (Metric m : metrics) {
            try {
                m.init(this);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    public void connect() throws IOException {
        transport = new DefaultUdpTransportMapping();
        transport.listen();

        // Create Snmp object for sending data to Agent
        snmp = new Snmp(transport);
    }

    public void disconnect() {
        try {
            if (snmp != null)
                snmp.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        int count = counter.incrementAndGet();
        try {
            for (Metric m : metrics) {
                if (count % m.getPollTime() == m.getPollTime() - 1)
                    get(m);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void get(Metric metric) throws IOException {

        // System.out.println("Sending Request to Agent...");
        ResponseEvent response = snmp.get(metric.getPDU(), comtarget);
        // Process Agent Response
        if (response != null) {
            // System.out.println("Got Response from Agent");
            PDU responsePDU = response.getResponse();

            if (responsePDU != null) {
                int errorStatus = responsePDU.getErrorStatus();

                if (errorStatus == PDU.noError) {
                    metric.handleResponse(responsePDU);
                } else {
                    int errorIndex = responsePDU.getErrorIndex();
                    String errorStatusText = responsePDU.getErrorStatusText();
                    System.out.println("Error: Request Failed");
                    System.out.println("Error Status = " + errorStatus);
                    System.out.println("Error Index = " + errorIndex);
                    System.out.println("Error Status Text = " + errorStatusText);
                }
            } else {
                System.out.println("Error: Response PDU is null");
            }
        } else {
            System.out.println("Error: Agent Timeout... ");
        }

    }

    public int getInt(String oid) throws IOException {
        VariableBinding[] v = getVariable(oid);
        if (v != null && v.length == 1)
            return v[0].getVariable().toInt();
        return -1;

    }

    public VariableBinding[] getVariable(String oid) throws IOException {

        PDU p = Util.toPDU(oid);
        return getVariable(p);
    }

    public VariableBinding[] getVariable(PDU p) throws IOException {
        ResponseEvent response = snmp.get(p, comtarget);
        // Process Agent Response
        if (response != null) {
            // System.out.println("Got Response from Agent");
            PDU responsePDU = response.getResponse();

            if (responsePDU != null) {
                int errorStatus = responsePDU.getErrorStatus();

                if (errorStatus == PDU.noError) {

                    VariableBinding[] v = responsePDU.toArray();
                    return v;

                } else {
                    int errorIndex = responsePDU.getErrorIndex();
                    String errorStatusText = responsePDU.getErrorStatusText();
                    System.out.println("Error: Request Failed for " + responsePDU.toString());
                    System.out.println("Error Status = " + errorStatus);
                    System.out.println("Error Index = " + errorIndex);
                    System.out.println("Error Status Text = " + errorStatusText);
                }
            } else {
                System.out.println("Error: Response PDU is null");
            }
        } else {
            System.out.println("Error: Agent Timeout... ");
        }
        return null;
    }

    public String getString(String oid) throws IOException {
        VariableBinding[] v = getVariable(oid);
        if (v != null && v.length == 1)
            return v[0].getVariable().toString();
        return null;
    }

}
