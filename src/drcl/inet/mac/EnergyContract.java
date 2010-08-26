package drcl.inet.mac;

import drcl.comp.Contract;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 05/29/2005
 *
 * This contract was developed to allow the application layer to directly control
 * the hardware of a sensor. They key to obtaining stronger performance is granting
 * more power to the highly demanded application layer which holds all the domain
 * specific information.
 *
 * This contract allows one to query the energy (can be used for resource adaptive nodes),
 * to set the radio mode, and set the energy level if a sensor was *hypothetically* charged
 * at some point.
*/

public class EnergyContract extends Contract
{
	public static final EnergyContract INSTANCE = new EnergyContract();

	public EnergyContract() { super(); }

	public EnergyContract(int role_) { super(role_); }

	public String getName()       { return "Energy Contract"; }

	public Object getContractContent() { return null; }

	/** This class implements the underlying message of the contract. */
	public static class Message extends drcl.comp.Message {

        public static final int ENERGY_QUERY = 0;
        public static final int SET_RADIO_MODE = 1;
        public static final int GET_RADIO_MODE = 2;
        public static final int SET_STEADY_STATE = 3;
        public static final int SET_ENERGY_LEVEL = 4;
        public static final int SET_SPREAD_CODE = 5;

        int radioMode;
        double energyLevel;
        int type;

        //the following two fields are for LEACH related apps only
        boolean steady_state;
        boolean isCH;

        public Message ()	{ }

        /**
         * constructor
         * Note that radioMode_ will also represent the spread code if
         * the type_ that is passed in is 5.
        */
		public Message (int type_, double energy_, int radioMode_)
        {
            radioMode = radioMode_;
            energyLevel = energy_;
            type = type_;
		}

        /**
         * this constructor is only used for LEACH. We cannot shut the radio off
         * accurately from the app layer so this contract allows to tell the
         * wireless card if its a CH and if we are in steady state.
         * @param type_
         * @param steady_state_
         * @param isCH_
         */
        public Message (int type_, boolean steady_state_, boolean isCH_)
        {
            type = type_;
            steady_state = steady_state_;
            isCH = isCH_;
        }

        public boolean get_steady_state()
        {
            return (steady_state);
        }

        public boolean isCH_()
        {
            return(isCH);
        }

        public int getRadioMode()
        {
            return radioMode;
        }

        public double getEnergyLevel()
        {
            return energyLevel;
        }

        public int getType()
        {
            return type;
        }
        
	    public Object clone()
        {
			// the contract is between two components; don't clone pkt
			return new Message(type, energyLevel, radioMode);
		}

		public Contract getContract()
		{ return INSTANCE; }

		public String toString(String separator_)
        {
	        String str;
        	str = "Energy Contract Message";
		    return str;
		}
	}
}

