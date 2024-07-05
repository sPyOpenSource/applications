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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import jx.net.IPAddress;
import jx.net.IPv4Address;
import jx.net.NetInit;
import jx.net.protocol.bootp.BOOTP;
import jx.net.protocol.bootp.BOOTPFormat;
import jx.zero.Debug;
import jx.zero.Memory;

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

    /**
     * Create a DHCP discovery packet
     * @param hdr
     * @return 
     * @throws java.io.IOException
     */
    //@Override
    public DatagramPacket createRequestPacket(BOOTPFormat hdr) throws IOException {
        DHCPMessage msg = new DHCPMessage(hdr, DHCPMessage.DHCPDISCOVER);
        return msg.toDatagramPacket();
    }

    //@Override
    public IPAddress processResponse(int transactionID, Memory packet, DatagramSocket socket) throws IOException {
        DHCPMessage msg = new DHCPMessage(packet);
        BOOTPFormat hdr = msg.getHeader();
        Debug.out.println("opcode");
        if (hdr.getOpcode() != BOOTPFormat.REPLY) {
            // Not a response
            return null;//false;
        }
        if (hdr.getTransactionID() != transactionID) {
            // Not for me
            return null;//false;
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
                Debug.out.println("serverid");
                byte[] serverID = msg.getOption(DHCPMessage.SERVER_IDENTIFIER_OPTION);
                byte[] requestedIP = hdr.getYiaddr().getBytes();
                Debug.out.println("hdr");
                IPAddress address = hdr.getYiaddr();
                hdr = new BOOTPFormat(
                        BOOTPFormat.REQUEST, transactionID, 0, 
                        hdr.getClientIPAddress(), hdr.getClientHwAddress(),
                        net.getUDPBuffer(500)
                );
                Debug.out.println("msg");
                msg = new DHCPMessage(hdr, DHCPMessage.DHCPREQUEST);
                msg.setOption(DHCPMessage.REQUESTED_IP_ADDRESS_OPTION, requestedIP);
                msg.setOption(DHCPMessage.SERVER_IDENTIFIER_OPTION, serverID);
                Debug.out.println("offer");
                DatagramPacket packet1 = msg.toDatagramPacket();
                packet1.setAddress(IPv4Address.BROADCAST_ADDRESS);
                packet1.setPort(SERVER_PORT);
                socket.send(packet1);
                return address;
            case DHCPMessage.DHCPACK:
                doConfigure(msg);
                return null;//true;
            case DHCPMessage.DHCPNAK:
                break;
        }
        return null;//false;
    }

    protected void doConfigure(DHCPMessage msg) throws IOException {
        //doConfigure(msg.getHeader());
        BOOTPFormat hdr = msg.getHeader();
        //net., hdr.getYiaddr();

        final IPAddress serverAddr = hdr.getServerIPAddress();
        final IPAddress networkAddress = serverAddr.and(serverAddr.getDefaultSubnetmask());

        if (hdr.getGatewayIPAddress().toInetAddress().isAnyLocalAddress()) {
            //cfg.addRoute(serverAddr, null, device, false);
            //cfg.addRoute(networkAddress, null, device, false);
        } else {
            //cfg.addRoute(networkAddress, hdr.getGatewayIPAddress(), device, false);
        }

        byte[] routerValue = msg.getOption(DHCPMessage.ROUTER_OPTION);
        if (routerValue != null && routerValue.length >= 4) {
            IPAddress routerIP = new IPv4Address(routerValue);
            //log.info("Got Router IP address : " + routerIP);
            //cfg.addRoute(IPAddress.ANY, routerIP, device, false);
        }

        // find the dns servers and add to the resolver
        final byte[] dnsValue = msg.getOption(DHCPMessage.DNS_OPTION);
        if (dnsValue != null) {
            for (int i = 0; i < dnsValue.length; i += 4) {
                final IPAddress dnsIP = new IPv4Address(dnsValue);
                
                //log.info("Got Dns IP address    : " + dnsIP);
                try {
                    //ResolverImpl.addDnsServer(dnsIP);
                } catch (Throwable ex) {
                    //log.error("Failed to configure DNS server");
                    //log.debug("Failed to configure DNS server", ex);
                }
            }
        }
    }
}
