/** author = Gilles TREDAN
 *  date = 21.07.05
 */

package drcl.inet.sensorsim.tracer;

/**
 * @author Gilles Tredan
 * @version 1.0, 07/24/2005
 *
 * this class is used to stores logs in a sorted order.
 * further uses are opened
*/


public class Tracelogger {

	public static final int max_logs = 200;
	Report[][] data;    //structure where logs are kept first col = node_num; second = event number
	int[] nbelems;	    //structure where number of events related to each node are kept
	int[] totalorder;   //structure used to keep total chronology of events arrival
	int total_event_count=0;


	public Tracelogger(int nb_nodes)
    {
		data = new Report[nb_nodes][max_logs];
		nbelems = new int[nb_nodes];
		totalorder = new int[nb_nodes*max_logs];
		for(int i = 0; i<nb_nodes;i++){
			//init nbelems to 0 everywhere
			nbelems[i]=0;
		}
	}


	public void add(Report rep)
    {
		//adds a report
		data[(int)rep.node_num ][nbelems[(int)rep.node_num ]] = rep;
		nbelems[(int)rep.node_num ]++;
		
		//copy it in the totalorder structure;
		totalorder[total_event_count] = (int) rep.node_num ;
		total_event_count++;
	}


	public void print(int node_num)
    {
		// prints the data for the concerned node
		if(nbelems[node_num]==0)
            System.out.println( "No log for node "+ node_num);

		for(int i = 0; i<nbelems[node_num]; i++) {
			System.out.println(data[node_num][i]);
		}
	}


	public String printEvent(int node_num, int event)
    {
		// prints the event number event for the concerned node
		if(nbelems[node_num]<=event) return  "No such log for node "+node_num;
		
		return data[node_num][event].toString( "");
		
	}


	public String printDate(int node_num, int event)
    {
		// prints the event number event for the concerned node
		if(nbelems[node_num]<=event) return  "No such log for node "+node_num;
		
		return data[node_num][event].date+"";
	}


	public String printGlobalEvent(int event)
    {
		// print the event identified by a global number (ie : in chronological order only, no node-related
		if(event > total_event_count) return "No such event";
		int local_index = 0;
		int concerned_node = totalorder[event];
		for(int i = 0; i<event; i++){
			//with my methode we need to get the local event count (ie related to a node)
			//to know at which line of the right table we can find the corresponding event;
			// it's a huge waste of processor time, i'll change that if i can
			if(totalorder[i]==concerned_node) local_index++;
		}
		return data[concerned_node][local_index].toString( "");
	}


	public String printGlobalEventDate(int event)
    {
		// print the event's date identified by a global number (ie : in chronological order only, no node-related
		if(event > total_event_count) return "No such event";
		int local_index = 0;
		int concerned_node = totalorder[event];
		for(int i = 0; i<event; i++){
			//with my methode we need to get the local event count (ie related to a node)
			//to know at which line of the right table we can find the corresponding event;
			// it's a huge waste of processor time, i'll change that if i can
			if(totalorder[i]==concerned_node) local_index++;
		}
		return data[concerned_node][local_index].date+"";
	}
	
	
	public int eventcount(int node_num)
    {
		// return the event count for the node node_num
		return nbelems[node_num];
	}


	public int alleventcount()
    {
		// prints the total number of events
		return total_event_count;
	}
	
}
