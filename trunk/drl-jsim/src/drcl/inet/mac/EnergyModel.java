// @(#)EnergyModel.java   1/2004
// Copyright (c) 1998-2004, Distributed Real-time Computing Lab (DRCL) 
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

package drcl.inet.mac;

/**
 * The class implements a simple energy model.
 * @author Rong Zheng
 *
 * Modified by Nicholas Merizzi May 2005
 */
public class EnergyModel extends drcl.DrclObj {

    private final double VOLTAGE = 1.0;
    double energy = 100.25;      //total energy in joules

    long nid;   //for debugging purposes.

    //values for RADIO COMPONENT
    private double Pt         = 0.096;       // Amplifier power (in watts);
    private double Pt_consume = 0.016;       // Transmittting Current that is required ( in amps)
    private double Pr_consume = 0.008;       // Receiving Current that is required (in amps)
    private double P_idle     = 0.0002;      // When Idle the unit still consumes current (this figure is an approximation)
    private double P_sleep    = 0.000008;    // current drawn when radio is in sleep mode.
    private double P_off      = 0.000001;    // 1 micro (10^-6) amps that is drawn when radio is off

    // Values for CPU COMPONENT
	private double CPU_ACTIVE_CUR = 2.9e-3; // 2.9 mA
	private double CPU_IDLE_CUR = 2.9e-3;  	// 2.9 mA
	private double CPU_SLEEP_CUR = 1.9e-6;  // 1.9 microA
	private double CPU_OFF_CUR = 1e-9;    	// 1 nanoA

    boolean isOn = true;
    boolean isSleep = false;

    /*The following are simply to keep track of what components are using
      the most energy overtime. Mainly used if creating adaptive WSN protocols*/
    double TotalRadioRx = 0.0;
    double TotalRadioTx = 0.0;
    double TotalRadioIdle = 0.0;
    double TotalRadioSleep = 0.0;

    double TotalRadioActiveTime = 0.0;
    double TotalRadioSleepTime = 0.0;
    double TotalRadioIdleTime = 0.0;
    double TotalRadioRxTime = 0.0;

    double TotalCPU = 0.0;
    double TotalCPUsleep = 0.0;
    double TotalCPUactive = 0.0;
    double TotalCPUsleepTime = 0.0;
    double TotalCPUactiveTime = 0.0;

    public EnergyModel() {
    }

	/** Set energy consumption
	  * @param Pt_ signal power
	  * @param Pt_consume_ power consumption of transmission
	  * @param Pr_consume_ power consumption for reception
	  * @param P_idle_ idle power consumption
	  * @param P_off_  shutdown energy consumption
	  */
    public void setEnergyConsumption(double Pt_, double Pt_consume_, double Pr_consume_, double P_idle_, double P_sleep_, double P_off_) {
        Pt = Pt_;
        Pt_consume = Pt_consume_;
        Pr_consume = Pr_consume_;
        P_idle = P_idle_;
        P_off = P_off_;
        P_sleep = P_sleep_;
    }

	public String toString()
    {
		return "EnergyModel:t_signal_power=" + Pt + ",t_consume=" + Pt_consume + ",r_consume=" + Pr_consume + ",idle_consume=" + P_idle + ",P_off=" + P_off;
	}

    public double getEnergy() { return energy;  }
    public void setNid(long nid) { this.nid = nid; }
    public void setEnergy(double energy_) { energy = energy_;  }
  
    /********************************************************************************************/

	public double getCPUActiveCur() {return CPU_ACTIVE_CUR;}            /** Gets the active current. */
    public double getCPUIdleCur() {return CPU_IDLE_CUR;}                /** Gets the idle current. */
    public double getCPUSleepCur() { return CPU_SLEEP_CUR; }            /** Gets the sleep current. */
	public double getCPUOffCur() { return CPU_OFF_CUR; }                /** Gets the OFF current. */

    public double getTotalRadioRx() { return TotalRadioRx; }
    public double getTotalRadioTx() { return TotalRadioTx; }
    public double getTotalRadioIdle() { return TotalRadioIdle; }
    public double getTotalRadioSleep() {return TotalRadioSleep;}

    public double getTotalRadioSleepTime() { return TotalRadioSleepTime; }
    public double getTotalRadioActiveTime() { return TotalRadioActiveTime; }
    public double getTotalRadioIdleTime() { return TotalRadioIdleTime; }
    public double getTotalRadioRxTime() { return TotalRadioRxTime; }

    public double getTotalCPU() {return TotalCPU; }
    public double getTotalCPUactive() { return TotalCPUactive; }
    public double getTotalCPUsleep() { return TotalCPUsleep; }
    public double getTotalCPUsleepTime() { return TotalCPUsleepTime; }
    public double getTotalCPUactiveTime() { return TotalCPUactiveTime; }

    public double setCPUActiveCur(double a) {return CPU_ACTIVE_CUR=a;}  /** Sets the active current. */
	public double setCPUIdleCur(double a) {return CPU_IDLE_CUR = a;}    /** Sets the idle current. */
	public double setCPUSleepCur(double a) {return CPU_SLEEP_CUR=a;}    /** Sets the sleep current. */
	public double setCPUOffCur(double a) {return CPU_OFF_CUR =a; }      /** Sets the OFF current. */

    public void setPt(double pt) {  Pt = pt;  }
    public double getPt() { return (Pt); }

    public void setPt_consume(double pt_consume) { Pt_consume = pt_consume; }
    public void setPr_consume(double pr_consume) {Pr_consume = pr_consume;}
    public void setP_idle(double p_idle) { P_idle = p_idle; }
    public void setP_sleep(double p_sleep) { P_sleep = p_sleep; }
    public void setP_off(double p_off) { P_off = p_off; }

    /********************************************************************************************/

    public boolean RadioUpdateIdleEnergy(double time)
    {
        TotalRadioIdle = TotalRadioIdle + (P_idle*time);
        TotalRadioIdleTime = TotalRadioIdleTime + time;
        energy -= (VOLTAGE) * (P_idle*time);
        if (energy <= 0) {
            energy = 0;
            isOn = false;
        }

       /* System.out.println("****************");
        System.out.println("The radio Idle energy removed is: " + (P_idle*time));
        System.out.println("The time duraction was: " + time);
       */
        return isOn;
    }


    public boolean RadioUpdateTxEnergy(double time)
    {
        TotalRadioTx = TotalRadioTx + (Pt_consume*time);
        TotalRadioActiveTime = TotalRadioActiveTime + time;
        energy -= (VOLTAGE)* (Pt_consume*time);
        if (energy <= 0) {
            energy = 0;
            isOn = false;
        }
       return isOn;
    }


    public boolean RadioUpdateRxEnergy(double time)
    {
        TotalRadioRx = TotalRadioRx + (Pr_consume*time);
        energy -= (VOLTAGE)* (Pr_consume*time);
        TotalRadioRxTime = TotalRadioRxTime + time;
        if (energy <= 0) {
            energy = 0;
            isOn = false;
        }
       /* System.out.println("****************");
        System.out.println("The radio Rx energy removed is: " + (Pr_consume*time));
        System.out.println("The time duraction was: " + time);
       */
        return isOn;
    }
    
    public boolean RadioUpdateSleepEnergy(double time)
    {
        TotalRadioSleep = TotalRadioSleep + (P_sleep*time);
        energy -= (VOLTAGE) * (P_sleep*time);
        TotalRadioSleepTime = TotalRadioSleepTime + time;
        /*System.out.println("****************");
        System.out.println("The radio sleep energy removed is: " + (P_sleep*time));
        System.out.println("The time duraction was: " + time);
        */
        if (energy <= 0) {
            energy = 0;
            isOn = false;
        }
        
        return isOn;
    }

   /********************************************************************************************/
    public boolean CPUupdateIdleEnergy(double time) {
        energy -= (VOLTAGE) *( CPU_IDLE_CUR * time);
        TotalCPU = TotalCPU+ (CPU_IDLE_CUR * time);

        if (energy <= 0) {
            energy = 0;
            isOn = false;
        }
        return isOn;
    }

    public boolean CPUupdateActiveEnergy(double time) {
        energy -= (VOLTAGE)*(CPU_ACTIVE_CUR * time);
        TotalCPU = TotalCPU+ (CPU_ACTIVE_CUR * time);
        TotalCPUactive = TotalCPUactive + (CPU_ACTIVE_CUR * time);
        TotalCPUactiveTime = TotalCPUactiveTime + time;

        if (energy <= 0) {
            energy = 0;
            isOn = false;
        }

        return isOn;
    }
    public boolean CPUupdateOffEnergy(double time) {
        TotalCPU = TotalCPU+ (CPU_OFF_CUR * time);
        energy -= (VOLTAGE) *(CPU_OFF_CUR * time);
        if (energy <= 0) {
            energy = 0;
            isOn = false;
        }

        return isOn;
    }
    public boolean CPUupdateSleepEnergy(double time) {

        energy -= (VOLTAGE)*(CPU_SLEEP_CUR * time);
        TotalCPU = TotalCPU+ (CPU_SLEEP_CUR * time);
        TotalCPUsleep = TotalCPUsleep + (CPU_SLEEP_CUR * time);
        TotalCPUsleepTime = TotalCPUsleepTime + time;

        if (energy <= 0) {
            energy = 0;
            isOn = false;
        }

        return isOn;
    }

    /*********************************************************************************/

    /**
     * Added by Nicholas:
     *  To support a cross-layer design where only the minimum
     *  amount of energy is used to send to a particular destination.
     * @param amnt
     */
    public void remove(double amnt)
    {
        //System.out.println("\nEM.java.remove: is removing: " + amnt);
        energy -= amnt;
        if (energy <= 0) {
            energy = 0;
            isOn = false;
        }
    }

    public void specialRemoveRX(double amnt){
        TotalRadioRx = TotalRadioRx + amnt;
    }

    public void specialRemoveTX(double amnt){
        TotalRadioTx = TotalRadioTx + amnt;
    }

}