package drcl.inet.sensorsim.tracer;

/**
 * @author Gilles Tredan
 * @version 1.0, 07/24/2005
 *
 * This class is used by modules to report to the tracer.
 * This allows to make a kind of prettyprint...
*/

public class Report
{
	public static final int col_size = 120;
	
	long node_num;
	long dest;
	String module;
	String message;
	double date;
	
	public Report( long node_num,long dest, String module, String message,  double date)
    {
	
		this.date = date;
		this.dest = dest;
		this.message = message;
		this.module = module;
		this.node_num = node_num;
	}

	public String toString(String sep_)
	{
		String str;
        	str = "["+node_num+"]("+module+"):"+((dest==node_num | dest ==-1)?"":"->["+dest+"]")+message+sep_;
//        	while(str.length() <col_size) str +=" ";
//        	str+=date;
		return str;
	}
	
}
