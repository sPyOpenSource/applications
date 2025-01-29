package jx.netmanager;

import jx.zero.*;
import jx.net.PacketsConsumer;
import jx.net.IPData;
import jx.net.IPConsumer;

import jx.buffer.multithread.MultiThreadBufferList;
import jx.buffer.multithread.Buffer;



class IPReceiver implements jx.net.IPReceiver, Service {
    PacketsConsumer etherLayer;
    PacketsConsumer ipSender;

    //Memory buf;
    CPUManager cpuManager = (CPUManager)InitialNaming.getInitialNaming().lookup("CPUManager");
    IPConsumer consumer;
    NetInit net;
    MultiThreadBufferList usableBufs, filledBufs;
  
    public IPReceiver(NetInit net, Memory[] bufs) {
	this.net = net;
	this.usableBufs = new MultiThreadBufferList(bufs);
	this.usableBufs.enableRecording("IP-available-queue");
	this.filledBufs = new MultiThreadBufferList();
	this.filledBufs.enableRecording("IP-receive-queue");

	consumer = new IPConsumer() {
                @Override
		public Memory processIP(IPData buf) {
		    Buffer h = usableBufs.nonblockingUndockFirstElement();
		    if (h == null) {
			Debug.out.println("jx.netmanager.IPReceiver: no buffer available, must drop packet!");
			return buf.getMemory();
		    }
		    Memory in = h.getData();
		    h.setData(buf.getMemory());
		    h.setMoreData(buf);
		    filledBufs.appendElement(h);
		    return in;
		}
	    };
		
	net.ip.registerIPConsumer(consumer);
    }

    public IPReceiver(NetInit net, Memory[] bufs, final String proto) {
	this.net = net;
	this.usableBufs = new MultiThreadBufferList(bufs);
	this.filledBufs = new MultiThreadBufferList();

	consumer = new IPConsumer() {
                @Override
		public Memory processIP(IPData buf) {
		    Debug.out.println("IPConsumer for proto "+proto+" called.");
		    Buffer h = usableBufs.nonblockingUndockFirstElement();
		    if (h == null) {
			Debug.out.println("jx.netmanager.IPReceiver: no buffer available, must drop packet!");
			return buf.getMemory();
		    }
		    Memory in = h.getData();
		    h.setData(buf.getMemory());
		    h.setMoreData(buf);
		    filledBufs.appendElement(h);
		    return in;
		}
	    };
		
	net.ip.registerConsumer(consumer, proto);
    }

    @Override
    public IPData receive(Memory buf, int timeoutMillis) {
	Clock clock = (Clock)InitialNaming.getInitialNaming().lookup("Clock");
	Buffer h =null;
	CycleTime now = new CycleTime();
	CycleTime start = new CycleTime();
	CycleTime diff = new CycleTime();
	clock.getCycles(start);
	while (h == null) {
	    clock.getCycles(now);
	    clock.subtract(diff, now, start);
	    if (clock.toMilliSec(diff) >= timeoutMillis)
		return null;
	    h = filledBufs.nonblockingUndockFirstElement();
	    Thread.yield();
	}
	IPData result = (IPData) h.getMoreData();
	buf = buf.revoke();
	h.setData(buf); // extendfull?
	usableBufs.appendElement(h);
	return result;
     }

    @Override
    public IPData receive(Memory buf) {
	Buffer h = filledBufs.undockFirstElement();
	IPData result = (IPData) h.getMoreData();
	buf = buf.revoke();
	h.setData(buf);// extendfull?
	usableBufs.appendElement(h);
	return result;
    }

    @Override
    public void close() {
	net.ip.unregisterIPConsumer(consumer);
    }

}
