package test.fs;

import jx.zero.Clock;
import jx.zero.CycleTime;

public final class DummyClock implements Clock {
    int t;
    @Override
    public int getTimeInMillis() { return t++; }
    @Override
    public long getTicks() {return 0;}
    @Override
    public int getTicks_low(){return 0;}
    @Override
    public int getTicks_high(){return 0;}
    @Override
    public  void getCycles(CycleTime c){}
    @Override
    public void subtract(CycleTime result, CycleTime a, CycleTime b){}
    @Override
    public int toMicroSec(CycleTime c) {return 0;}
    @Override
    public int toNanoSec(CycleTime c) {return 0;}
    @Override
    public int toMilliSec(CycleTime c) {return 0;}
}
