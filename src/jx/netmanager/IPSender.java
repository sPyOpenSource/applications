package jx.netmanager;

import jx.zero.*;
import jx.net.PacketsConsumer;
import jx.net.PacketsConsumer1;
import jx.net.IPAddress;
import jx.net.UnknownAddressException;

/**
 * IP communication endpoint
 * @author Michael Golm
 */
class IPSender implements jx.net.IPSender, Service {
    PacketsConsumer etherLayer;
    PacketsConsumer ipSender;
    PacketsConsumer1 ipSender1;
    IPAddress dst;

/*
    IPSender(NetInit net, IPAddress dst) throws UnknownAddressException  {
	this.dst = dst;
	byte dest[] = net.arp.lookup(dst);
	etherLayer = net.ether.getTransmitter(dest, "IP");
	ipSender = net.ip.getTransmitter(etherLayer, dst, 0x801);

	PacketsConsumer1 etherLayer1 = net.ether.getTransmitter1(dest, "IP");
	ipSender1 = net.ip.getTransmitter1(etherLayer1, dst, 0x801);
    }      
*/

    IPSender(NetInit net, IPAddress dst, int id) throws UnknownAddressException  {
        this.dst = dst;
        byte dest[] = net.arp.lookup(dst);
        etherLayer = net.ether.getTransmitter(dest, "IP");
        ipSender = net.ip.getTransmitter(etherLayer, dst, id);

	PacketsConsumer1 etherLayer1 = net.ether.getTransmitter1(dest, "IP");
	ipSender1 = net.ip.getTransmitter1(etherLayer1, dst, id);
    }


    public Memory send1(Memory m, int offset, int size) {
	return ipSender1.processMemory(m, offset, size);
    }

    public Memory send(Memory m) {
	return ipSender.processMemory(m/*m.revoke()*/);
    }

    public void close() {
    }

    public IPAddress getDestination() { return dst; }

}
