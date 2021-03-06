package jx.netmanager;

import jx.zero.*;
import jx.net.PacketsConsumer;
import jx.net.UDPData;
import jx.net.UDPConsumer;

import jx.buffer.multithread.MultiThreadBufferList;
import jx.buffer.multithread.Buffer;



class UDPReceiver implements jx.net.UDPReceiver, Service {
    PacketsConsumer ipLayer;
    PacketsConsumer udpSender;

    CPUManager cpuManager = (CPUManager)InitialNaming.getInitialNaming().lookup("CPUManager");
    UDPConsumer consumer;
    int port;
    NetInit net;
    private MultiThreadBufferList usableBufs, filledBufs;
  
    public UDPReceiver(NetInit net, int localPort, Memory[] bufs, boolean avoidSplitting) {
	this.net = net;
	this.usableBufs = new MultiThreadBufferList(bufs);
	this.usableBufs.setListName("UDP-available-queue");
	this.usableBufs.enableRecording("UDP-available-queue");
	this.filledBufs = new MultiThreadBufferList();
	this.filledBufs.setListName("UDP-receive-queue");
	this.filledBufs.enableRecording("UDP-receive-queue");
	
	//filledBufs.setVerbose(true);
	filledBufs.requireMoredata(true);

	//if (avoidSplitting) {
	    /*consumer1 = new UDPConsumer1() {
                    @Override
		    public Memory processUDP1(UDPData buf) {
			Buffer h = usableBufs.nonblockingUndockFirstElement();
			if (h == null) {
			    Debug.out.println("jx.netmanager.UDPReceiver: no buffer available, must drop packet!");
			    return buf.mem;
			}
			Memory in = h.getData();
			//in = in.revoke();
			h.setData(buf.mem);
			h.setMoreData(buf);
			filledBufs.appendElement(h);
			return in;
		    }
		};
	    net.udp.registerUDPConsumer1(consumer1, localPort);*/
	//} else {
	    consumer = new UDPConsumer() {
                    @Override
		    public Memory processUDP(UDPData buf) {
			Buffer h = usableBufs.nonblockingUndockFirstElement();
			if (h == null) {
			    Debug.out.println("jx.netmanager.UDPReceiver: no buffer available, must drop packet!");
			    return buf.mem;
			}
			Memory in = h.getData();
			in = in.revoke();
			h.setData(buf.mem);
			h.setMoreData(buf);
			filledBufs.appendElement(h);
			return in;
		    }
		};
	    net.udp.registerUDPConsumer(consumer, localPort);
	//}
	
	port = localPort;
		
    }
    /*
    public UDPReceiver(NetInit net, int localPort, Memory[] bufs, boolean xxxx) {
	this.net = net;
	this.usableBufs = new MultiThreadBufferList2(bufs);
	this.filledBufs = new MultiThreadBufferList2();

	consumer = new MemoryConsumer() {
		public Memory processMemory(Memory buf) {
		    Buffer h = usableBufs.nonblockingUndockFirstElement();
		    if (h == null) {
			Debug.out.println("jx.netmanager.UDPReceiver: no buffer available, must drop packet!");
			return buf;
		    }
		    Memory in = h.getData();
		    h.setData(buf);
		    Debug.out.println("APPEND TO FILLED");
		    filledBufs.appendElement(h);
		    return in;
		}
	    };
	port = localPort;
		
	net.udp.registerConsumer(consumer, localPort);
    }
    */

    @Override
    public UDPData receive(Memory buf, int timeoutMillis) {
	Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	Memory h = null;
	CycleTime now = new CycleTime();
	CycleTime start = new CycleTime();
	CycleTime diff = new CycleTime();
	clock.getCycles(start);
	while (h == null) {
	    clock.getCycles(now);
	    clock.subtract(diff, now, start);
	    if (clock.toMilliSec(diff) >= timeoutMillis) {
		for (int i = 0; i < (10 * timeoutMillis); i++) Thread.yield();
		return null;
	    }
	    h = filledBufs.getLast().getData();
	    Thread.yield();
	}
	//buf = buf.revoke();
	//UDPData result = (UDPData) h.getMoreData();
	//if (result == null) throw new Error("received packet but UDPData result==null");
	//h.setData(buf);// extendfull?
	//usableBufs.appendElement(h);
        buf.copyFromMemory(h, 0, 0, h.size());
	return new UDPData();
     }


    @Override
    public UDPData receive(Memory buf) {
        Memory h = filledBufs.getLast().getData();
        while(h == null){
            h = filledBufs.getLast().getData();
        }
        Debug.out.println("size: " + h.size());
        buf.copyFromMemory(h, 0, 0, h.size());
        /*for(int i = 0; i < h.size(); i++){
            Debug.out.println(h.get8(i));
        }*/
        Debug.out.println("copy finished");
	//Buffer h = filledBufs.undockFirstElement();
	UDPData result = null;//(UDPData) h.getMoreData();
	//buf = buf.revoke();
	//h.setData(buf);// extendfull?
	//usableBufs.appendElement(h);
	//UDPData u = new UDPData();
	//u.mem = result;
	//u.sourceAddress = net.udp.getSource(result);
	//u.sourcePort = net.udp.getSourcePort(result);
	return result;
    }

    /*
    public UDPData receive(Memory buf) {
	Buffer h = filledBufs.undockFirstElement();
	Memory result = h.getData();
	h.setData(buf.extendAndRevoke());
	usableBufs.appendElement(h);
	UDPData u = new UDPData();
	u.mem = result;
	u.sourceAddress = net.udp.getSource(result);
	u.sourcePort = net.udp.getSourcePort(result);
	return u;
    }
    */

    @Override
    public void close() {
	net.udp.unregisterUDPConsumer(consumer, port);
    }

}
