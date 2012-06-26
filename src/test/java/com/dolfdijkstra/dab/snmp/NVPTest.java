package com.dolfdijkstra.dab.snmp;

import org.junit.Test;

import static org.junit.Assert.*;

public class NVPTest {

    @Test
    public void testNVPStringInt() {
        NVP nvp = new NVP("foo", Integer.MAX_VALUE);
        assertTrue(Integer.MAX_VALUE-nvp.getValue()==0);
    }

    @Test
    public void testNVPStringDouble() {
        NVP nvp = new NVP("foo", Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE,nvp.getValue(),0);
    }

    @Test
    public void testNVPStringLong() {
        NVP nvp = new NVP("foo", Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE,nvp.getValue(),0);
    }

}
