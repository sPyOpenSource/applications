/*
 * $Id$
 *
 * Copyright (C) 2003-2015 JNode.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
 
package org.jnode.net.ipv4.dhcp;

//import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import jx.net.IPAddress;
import jx.net.NetInit;
import jx.net.protocol.bootp.BOOTP;
import jx.net.protocol.bootp.BOOTPFormat;

/**
 * System independent base class.
 * Implementations should override doConfigure.
 *
 * @author markhale
 */
public class AbstractDHCPClient extends BOOTP {

    public AbstractDHCPClient(NetInit net, byte[] hwaddr) {
        super(net, hwaddr);
    }

    //private static final Logger log = Logger.getLogger(AbstractDHCPClient.class);

    /**
     * Create a DHCP discovery packet
     * @param hdr
     * @return 
     * @throws java.io.IOException
     */
    //@Override
    protected DatagramPacket createRequestPacket(BOOTPFormat hdr) throws IOException {
        DHCPMessage msg = new DHCPMessage(hdr, DHCPMessage.DHCPDISCOVER);
        return msg.toDatagramPacket();
    }

    //@Override
    protected boolean processResponse(int transactionID, DatagramPacket packet) throws IOException {
        DHCPMessage msg = new DHCPMessage(packet);
        BOOTPFormat hdr = msg.getHeader();
        if (hdr.getOpcode() != BOOTPFormat.REPLY) {
            // Not a response
            return false;
        }
        if (hdr.getTransactionID() != transactionID) {
            // Not for me
            return false;
        }

        // debug the DHCP message
        /*if (log.isDebugEnabled()) {
            log.debug("Got Client IP address  : " + hdr.getClientIPAddress());
            log.debug("Got Your IP address    : " + hdr.getYourIPAddress());
            log.debug("Got Server IP address  : " + hdr.getServerIPAddress());
            log.debug("Got Gateway IP address : " + hdr.getGatewayIPAddress());
            for (int n = 1; n < 255; n++) {
                byte[] value = msg.getOption(n);
                if (value != null) {
                    switch (value.length) {
                        case 1:
                            log.debug("Option " + n + " : " + (int) (value[0]));
                            break;
                        case 2:
                            log.debug("Option " + n + " : " + ((value[0] << 8) | value[1]));
                            break;
                        case 4:
                            log.debug("Option " + n + " : " +
                                    InetAddress.getByAddress(value).toString());
                            break;
                        default:
                            log.debug("Option " + n + " : " + new String(value));
                            break;
                    }
                }
            }
        }*/

        switch (msg.getMessageType()) {
            case DHCPMessage.DHCPOFFER:
                byte[] serverID = msg.getOption(DHCPMessage.SERVER_IDENTIFIER_OPTION);
                byte[] requestedIP = hdr.getYiaddr().getBytes();
                hdr = new BOOTPFormat(
                        BOOTPFormat.REQUEST, transactionID, 0, 
                        hdr.getClientIPAddress(), hdr.getClientHwAddress());
                msg = new DHCPMessage(hdr, DHCPMessage.DHCPREQUEST);
                msg.setOption(DHCPMessage.REQUESTED_IP_ADDRESS_OPTION, requestedIP);
                msg.setOption(DHCPMessage.SERVER_IDENTIFIER_OPTION, serverID);
                packet = msg.toDatagramPacket();
                packet.setAddress(IPAddress.BROADCAST_ADDRESS);
                packet.setPort(SERVER_PORT);
                socket.send(packet);
                break;
            case DHCPMessage.DHCPACK:
                doConfigure(msg);
                return true;
            case DHCPMessage.DHCPNAK:
                break;
        }
        return false;
    }

    protected void doConfigure(DHCPMessage msg) throws IOException {
        doConfigure(msg.getHeader());
    }
}
