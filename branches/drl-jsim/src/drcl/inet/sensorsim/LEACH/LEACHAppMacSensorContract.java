package drcl.inet.sensorsim.LEACH;

import drcl.comp.Contract;

/**
 * @author Nicholas Merizzi
 * @version 1.0, 05/29/2005
 *
 * This contract is for defining communication between the LEACH application layer and
 * the MAC layer. This flattens the traditional newtork layer abstraction in order
 * to maximize performance and allow the application direct control over the lower layers.
 * This contract allows for a LEACH capable sensor to set the spreading code that it
 * should use and how many advertisements it has heard so far.
 * 
 */
public class LEACHAppMacSensorContract extends Contract
{

    public static final LEACHAppMacSensorContract INSTANCE = new LEACHAppMacSensorContract();

    public LEACHAppMacSensorContract()
    {
        super();
    }

    public LEACHAppMacSensorContract(int role_)
    {
        super(role_);
    }

    public String getName()
    {
        return "LEACH <-> MAC Contract";
    }

    public Object getContractContent()
    {
        return null;
    }

    /** This class implements the underlying message of the contract. */
    public static class Message extends drcl.comp.Message
    {

        //Possible Message Types
        public static final int SETTING_CHheard  = 0;
        public static final int SETTING_myADVnum = 1;
        public static final int GETTING_myADVnum = 2;
        public static final int SETTING_code     = 3;
        public static final int SETTING_isCH     = 4;

        int type;
        int value;
        boolean isCH; //only used for LEACH MODE

        public Message (int type_, int value_)
        {
            type = type_;
            value = value_;
        }

        /*constructor only for LEACH mode*/
        public Message (int type_, boolean isCH_)
        {
            type = type_;
            isCH = isCH_;
        }

        public boolean get_isCH()
        {
            return isCH;
        }
        
        public int getType()
        {
            return type;
        }

        public int getValue()
        {
            return value;
        }

        public Object clone()
        {
            // the contract is between two components; don't clone pkt
            return new Message(type, value);
        }

        public Contract getContract()
        {
            return INSTANCE;
        }

        public String toString(String separator_)
        {
            String str;
            str = "LEACHApp-MacSensor Contract Message:" + separator_ ;
            return str;
        }
    }
}