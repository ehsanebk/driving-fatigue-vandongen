package actr.tasks.drivingPVT;

import actr.tasks.driving.Values;

public class SessionPVT {

	Values RT = new Values();
	Values timeOfReactionsFromStart;
	
	double startTime = 0;
	double totalSessionTime = 0;
	int stimulusIndex = 0;
	int numberOfResponses = 0; // number of responses, this can be diff from the
	// stimulusIndex because of false responses
	
	double timeOfTheDay;
	double bioMathValue;
	double timeAwake;
	
	public SessionPVT() {
		RT = new Values();
		timeOfReactionsFromStart = new Values();
		//blocks = new Vector<Block>();
	}

	public boolean sleepAttacks(){
		for (int i = 0; i < RT.size(); i++) 
			if ( RT.get(i) == 30000)
				return true;
		return false;
	}
	
	public double getSessionAveAlertRT(){
		return RT.averageInRange(150, 500);
	}
	
	public double getSessionAveRT(){
		return RT.average();
	}
	
	public int getSessionNumberOfLapses(){
		int l = 0;
		for (int i = 0; i < RT.size(); i++) 
			if ( RT.get(i) >= 500 && RT.get(i) < 30000)
				l++;
		return l;
	}
	
	public int getSessionNumberOfFalseAlerts(){
		int l = 0;
		for (int i = 0; i < RT.size(); i++) 
			if ( RT.get(i) < 150)
				l++;
		return l;
	}
	
	/**
	 * @return Log-transformed Signal-to-Noise Ratio (LSNR) approximation
	 */
	public double getSessionLSNR_apx(){
		// LSNR_apx = B ((1/N) sum_1^N (1 / RT_i))    B = 3855ms
		int N = 0;
		int B = 3855;
		double sum = 0;
		for (int i = 0; i < RT.size(); i++) 
			if ( RT.get(i) >= 150 && RT.get(i) < 30000){
				sum = sum + 1.0 / RT.get(i);
				N++;
			}
		return B * ((1.0/N) * sum);
	}
	
	public Values getRTblock(int blockNumber){ // starts from 0
		Values v = new Values();
		if (blockNumber == 0)
			v.add(RT.get(0));
		for (int i = 1; i < timeOfReactionsFromStart.size(); i++) 
			if (timeOfReactionsFromStart.get(i-1) > 300.0*(blockNumber)-1 && timeOfReactionsFromStart.get(i-1) <= 300.0*(blockNumber+1)-1){
				if (RT.get(i) < 30000)
					v.add(RT.get(i));
			}
		return v;
	}
	
	/**
	 * @param blockNumber
	 * starts from 0
	 * @return
	 */
	public int getBlockLapses(int blockNumber){
		Values RTblock = getRTblock(blockNumber);
		int l = 0;
		for (int i = 0; i < RTblock.size(); i++) 
			if (RTblock.get(i) >= 500 && RT.get(i) < 30000)
				l++;
		return l;
	}

	/**
	 * starts from 0
	 * @return Log-transformed Signal-to-Noise Ratio (LSNR) approximation
	 */
	public double getBlockLSNR_apx(int blockNumber){
		Values RTblock = getRTblock(blockNumber);
		// LSNR_apx = B ((1/N) sum_1^N (1 / RT_i))    B = 3855ms
		int N = 0;
		int B = 3855;
		double sum = 0;
		for (int i = 0; i < RTblock.size(); i++) 
			if ( RTblock.get(i) >= 150 && RT.get(i) < 30000){
				sum = sum + 1.0 / RTblock.get(i);
				N++;
			}
		return B * ((1.0/N) * sum);
	}

	public double getBlockAveAlertResponses(int blockNumber){
		Values RTblock = getRTblock(blockNumber);
		return RTblock.averageInRange(150, 500);
	}
	
}
