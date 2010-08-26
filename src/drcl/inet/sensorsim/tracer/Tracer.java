package drcl.inet.sensorsim.tracer;

import drcl.comp.Port;

/**
 * @author Gilles Tredan
 * @version 1.0, 07/24/2005
 *
*/

public class Tracer extends drcl.net.Module  {
	
	protected Tracelogger tracelog;
	
	{
		removeDefaultUpPort() ;
	}
	
	
	public Tracer(Tracelogger trace)
    {
		tracelog = trace;
		
	}

	 protected synchronized void dataArriveAtDownPort(Object data_, Port downPort_)
     {
		 System.out.println(((Report) data_).toString( " ") );
		 tracelog.add( (Report) data_);
	 }
	 
	 
	 
}
