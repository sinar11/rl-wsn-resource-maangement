// @(#)CPUAvr.java   12/2003
// Copyright (c) 1998-2003, Distributed Real-time Computing Lab (DRCL) 
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// 
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer. 
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution. 
// 3. Neither the name of "DRCL" nor the names of its contributors may be used
//    to endorse or promote products derived from this software without specific
//    prior written permission. 
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// 

package drcl.inet.sensorsim;

/**
 * This class implements a CPU model with reasonable values for active, idle, sleep and off currents.
 *
 * @author Ahmed Sobeih
 * @version 1.0, 12/19/2003
 *
 * Modified by Nicholas Merizzi March 2005 -> To integrate the wireless and sensorsim
 * energy models into one model.
*/

public class CPUAvr extends CPUBase
{

	public CPUAvr() {
		super();
        initializeSimple();
		flag=false;
	}
 

    /**
     * Reports the current to the battery model.
     * @param type The actual radio mode
     * @param time how long its been in that mode for
     */
	public void reportCurrent(int type, double time)
    {
		batteryPort.doSending(new BatteryContract.Message(type, time));
	}

    /**
     * Initializes the simple battery model.
    */
	public void initializeSimple()
    {
		lastTimeOut=0.0;
	}

	/**
     * Sets the CPU mode and reports the current to the battery model.
    */
	public synchronized int setCPUMode(int mode)
    {
        double now = getTime() ;

        //CPU_IDLE=0, CPU_SLEEP=1, CPU_ACTIVE=2, CPU_OFF=3
        if (now >= lastTimeOut) {
            reportCurrent(this.cpuMode, (now - lastTimeOut));
            lastTimeOut = now;
        }else {
            System.out.println("CPUAvr.java Negative Time Gap!");
        }
		this.cpuMode = mode;
		reportCpuMode(mode);
        //lastTimeOut = now;
		return 1;
	}
}
