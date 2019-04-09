package actr.tasks.drivingPVT;

import java.util.*;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import actr.model.Event;
import actr.model.Symbol;
import actr.task.*;
import actr.tasks.driving.Values;
import actr.tasks.fatigue.*;

/**
 * Model of PVT test and Fatigue mechanism
 * 
 * Paper: Efficient driver drowsiness detection at moderate levels of drowsiness
 * 
 * Pia M. Forsmana, Bryan J. Vilaa,b, Robert A. Short c, Christopher G. Mott d,
 * Hans P.A. Van Dongena,
 * 
 * @author Ehsan Khosroshahi
 */

public class DrivingPVT extends PVT {
	
	
	
	
	/**
	 * Constructs a new PVT.
	 */
	public DrivingPVT() {
		super();
	}
	
	
//	@Override
//	public int analysisIterations() {
// 		return 100;
//	}

	@Override
	public Result analyze(Task[] tasks, boolean output) {

		Result result = new Result();
		try {

			int numberOfSessions = super.timesOfPVT.size();
			Values[] totallSessionLapsesValues = new Values[numberOfSessions];
			Values[] totallSessionLSNR_apx = new Values[numberOfSessions];

			// allocating memory to the vectors
			for (int i = 0; i < numberOfSessions; i++) {
				totallSessionLapsesValues[i] = new Values();
				totallSessionLSNR_apx[i] = new Values();
			}

			for (Task taskCast : tasks) {
				DrivingPVT task = (DrivingPVT) taskCast;
				for (int i = 0; i < numberOfSessions; i++) {
					totallSessionLapsesValues[i].add(task.sessions.get(i).getNumberOfLapses());
					totallSessionLSNR_apx[i].add(task.sessions.get(i).getLSNR_apx());
				}
			}

			DecimalFormat df3 = new DecimalFormat("#.000");

			
			getModel().output("\nAverage Number of lapses in the time points \n");
			getModel().output("Day\t09:00\t12:00\t15:00\t18:00 ");
			for (int i = 0; i < 5; i++) {
				getModel().output((i + 2) + "\t" + totallSessionLapsesValues[i * 4].meanDF3() + "\t"
						+ totallSessionLapsesValues[i * 4 + 1].meanDF3() + "\t" + totallSessionLapsesValues[i * 4 + 2].meanDF3() + "\t"
						+ totallSessionLapsesValues[i * 4 + 3].meanDF3());
			}
			getModel().output("* 34 h break *");
			for (int i = 5; i < 10; i++) {
				getModel().output((i + 4) + "\t" + totallSessionLapsesValues[i * 4].meanDF3() + "\t"
						+ totallSessionLapsesValues[i * 4 + 1].meanDF3() + "\t" + totallSessionLapsesValues[i * 4 + 2].meanDF3() + "\t"
						+ totallSessionLapsesValues[i * 4 + 3].meanDF3());
			}
			getModel().output("\n*******************************************\n");
			
			getModel().output("\nAverage LSNR_apx in the time points \n");
			getModel().output("Day\t09:00\t12:00\t15:00\t18:00 ");
			for (int i = 0; i < 5; i++) {
				getModel().output((i + 2) + "\t" + totallSessionLSNR_apx[i * 4].meanDF3() + "\t"
						+ totallSessionLSNR_apx[i * 4 + 1].meanDF3() + "\t" + totallSessionLSNR_apx[i * 4 + 2].meanDF3() + "\t"
						+ totallSessionLSNR_apx[i * 4 + 3].meanDF3());
			}
			getModel().output("* 34 h break *");
			for (int i = 5; i < 10; i++) {
				getModel().output((i + 4) + "\t" + totallSessionLSNR_apx[i * 4].meanDF3() + "\t"
						+ totallSessionLSNR_apx[i * 4 + 1].meanDF3() + "\t" + totallSessionLSNR_apx[i * 4 + 2].meanDF3() + "\t"
						+ totallSessionLSNR_apx[i * 4 + 3].meanDF3());
			}
			getModel().output("\n*******************************************\n");
			
			/////  Outputting the blocks ////
			Values[][] totallBlockLapsesValues = new Values[numberOfSessions][2];
			Values[][] totallBlockLSNR_apx = new Values[numberOfSessions][2];
			
			// allocating memory to the vectors
			for (int i = 0; i < numberOfSessions; i++) {
				for (int j = 0; j < 2; j++) {
					totallBlockLapsesValues[i][j] = new Values();
					totallBlockLSNR_apx[i][j] = new Values();
					
				}
			}

			for (Task taskCast : tasks) {
				DrivingPVT task = (DrivingPVT) taskCast;
				for (int i = 0; i < numberOfSessions; i++) {
					for (int j = 0; j < 2; j++) {
						totallBlockLapsesValues[i][j].add(task.sessions.get(i).getBlocks().get(j).getNumberOfLapses());
						totallBlockLSNR_apx[i][j].add(task.sessions.get(i).getBlocks().get(j).getLSNR_apx());
						
					}
				}
			}

			getModel().output("\n*******************************************\n");
			
			getModel().output("\nAverage Number of block lapses in the time points \n");
			getModel().output("Day\t09:00\t\t12:00\t\t15:00\t\t18:00\t ");
			getModel().output("   \t1of2\t2of2\t1of2\t2of2\t1of2\t2of2\t1of2\t2of2 ");
			for (int i = 0; i < 5; i++) { getModel().output((i + 2) + "\t" 
					+ totallBlockLapsesValues[i * 4    ][0].meanDF3() + "\t" + totallBlockLapsesValues[i * 4][1].meanDF3() +"\t"
					+ totallBlockLapsesValues[i * 4 + 1][0].meanDF3() + "\t" + totallBlockLapsesValues[i * 4 + 1][1].meanDF3() +"\t"
					+ totallBlockLapsesValues[i * 4 + 2][0].meanDF3() + "\t" + totallBlockLapsesValues[i * 4 + 2][1].meanDF3() +"\t"
					+ totallBlockLapsesValues[i * 4 + 3][0].meanDF3() + "\t" + totallBlockLapsesValues[i * 4 + 3][1].meanDF3() +"\t"
					);
			}
			getModel().output("* 34 h break *");
			for (int i = 5; i < 10; i++) { getModel().output((i + 4) + "\t" 
					+ totallBlockLapsesValues[i * 4    ][0].meanDF3() + "\t" + totallBlockLapsesValues[i * 4][1].meanDF3() +"\t"
					+ totallBlockLapsesValues[i * 4 + 1][0].meanDF3() + "\t" + totallBlockLapsesValues[i * 4 + 1][1].meanDF3() +"\t"
					+ totallBlockLapsesValues[i * 4 + 2][0].meanDF3() + "\t" + totallBlockLapsesValues[i * 4 + 2][1].meanDF3() +"\t"
					+ totallBlockLapsesValues[i * 4 + 3][0].meanDF3() + "\t" + totallBlockLapsesValues[i * 4 + 3][1].meanDF3() +"\t"
					);
			}

			
			getModel().output("\n*******************************************\n");
			
			getModel().output("\nAverage Number of block LSNR_apx in the time points \n");
			getModel().output("Day\t09:00\t\t12:00\t\t15:00\t\t18:00\t ");
			getModel().output("   \t1of2\t2of2\t1of2\t2of2\t1of2\t2of2\t1of2\t2of2 ");
			for (int i = 0; i < 5; i++) { getModel().output((i + 2) + "\t" 
					+ totallBlockLSNR_apx[i * 4    ][0].meanDF3() + "\t" + totallBlockLSNR_apx[i * 4][1].meanDF3() +"\t"
					+ totallBlockLSNR_apx[i * 4 + 1][0].meanDF3() + "\t" + totallBlockLSNR_apx[i * 4 + 1][1].meanDF3() +"\t"
					+ totallBlockLSNR_apx[i * 4 + 2][0].meanDF3() + "\t" + totallBlockLSNR_apx[i * 4 + 2][1].meanDF3() +"\t"
					+ totallBlockLSNR_apx[i * 4 + 3][0].meanDF3() + "\t" + totallBlockLSNR_apx[i * 4 + 3][1].meanDF3() +"\t"
					);
			}
			getModel().output("* 34 h break *");
			for (int i = 5; i < 10; i++) { getModel().output((i + 4) + "\t" 
					+ totallBlockLSNR_apx[i * 4    ][0].meanDF3() + "\t" + totallBlockLSNR_apx[i * 4][1].meanDF3() +"\t"
					+ totallBlockLSNR_apx[i * 4 + 1][0].meanDF3() + "\t" + totallBlockLSNR_apx[i * 4 + 1][1].meanDF3() +"\t"
					+ totallBlockLSNR_apx[i * 4 + 2][0].meanDF3() + "\t" + totallBlockLSNR_apx[i * 4 + 2][1].meanDF3() +"\t"
					+ totallBlockLSNR_apx[i * 4 + 3][0].meanDF3() + "\t" + totallBlockLSNR_apx[i * 4 + 3][1].meanDF3() +"\t"
					);
			}

			
			///////////////////////////////////////////////////////////////////////////////////////////////////		
			// Writing the output to csv files in the specified directory (outputDIR)
			String DIR = getModel().getFatigue().getOutputDIR();

			if (DIR == null)
				return result;
			
			/// Outputting the raw data to a file
			
			File dataFile = new File(DIR + "/"  + "Model_PVT.csv");
			if (!dataFile.exists())
				dataFile.createNewFile();
			PrintWriter fileOut = new PrintWriter(dataFile);
			
			fileOut.println("\n Medel PVT \n");
			
			for (Task taskCast : tasks) {
				DrivingPVT task = (DrivingPVT) taskCast;
				fileOut.print("Session Ave Lapses,");
				for (int i = 0; i < numberOfSessions; i++) {
					fileOut.print(task.sessions.get(i).getNumberOfLapses() + ",");
				}
				fileOut.print("\n");
				fileOut.flush();
				
				fileOut.print("B1 Ave Lapses,");
				for (int i = 0; i < numberOfSessions; i++) {
					fileOut.print(task.sessions.get(i).getBlocks().get(0).getNumberOfLapses() + ",");
				}
				fileOut.print("\n");
				fileOut.flush();
				
				fileOut.print("B2 Ave Lapses,");
				for (int i = 0; i < numberOfSessions; i++) {
					fileOut.print(task.sessions.get(i).getBlocks().get(1).getNumberOfLapses() + ",");
				}
				fileOut.print("\n");
				fileOut.flush();
				
				fileOut.print("Session Ave LSNR_apx,");
				for (int i = 0; i < numberOfSessions; i++) {
					fileOut.print(task.sessions.get(i).getLSNR_apx() + ",");
				}
				fileOut.print("\n");
				fileOut.flush();
				
				fileOut.print("B1 Ave LSNR_apx,");
				for (int i = 0; i < numberOfSessions; i++) {
					fileOut.print(task.sessions.get(i).getBlocks().get(0).getLSNR_apx() + ",");
				}
				fileOut.print("\n");
				fileOut.flush();
				
				fileOut.print("B2 Ave LSNR_apx,");
				for (int i = 0; i < numberOfSessions; i++) {
					fileOut.print(task.sessions.get(i).getBlocks().get(1).getLSNR_apx() + ",");
				}
				
//				fileOut.print("Session Ave RT,");
//				for (int i = 0; i < numberOfSessions; i++) {
//					fileOut.print(task.sessions.get(i).getSessionAveRT() + ",");
//				}
//				
//				fileOut.print("Session Ave Alert RT,");
//				for (int i = 0; i < numberOfSessions; i++) {
//					fileOut.print(task.sessions.get(i).getSessionAveAlertRT() + ",");
//				}
				
				fileOut.print("\n****\n");
				fileOut.flush();
			}
			
			fileOut.close();

			
			// Writing raw data to file based on sessions
			File rawDataOut = new File(DIR + "/"  + "Model_PVT(Raw).csv");
			PrintWriter rawOutputCSV = null;
			rawOutputCSV = new PrintWriter(rawDataOut);
			
			rawOutputCSV.println("\n Model PVT \n");
			for (Task taskCast : tasks) {
				DrivingPVT task = (DrivingPVT) taskCast;
				for (int i = 0; i < numberOfSessions; i++) {
					rawOutputCSV.print("session #"+ i+1 + ",");
					for (int j = 0; j < task.sessions.get(i).getReactionTimes().size(); j++) {
						rawOutputCSV.print((int)task.sessions.get(i).getReactionTimes().get(j) + ",");
					}
					rawOutputCSV.print("\n");
				}
				rawOutputCSV.print("\n****\n");
				rawOutputCSV.flush();

			}
			rawOutputCSV.close();


			// Writing Numbers to the file based on sessions
			File dataSessionFile = new File("./resultPVT/Day_POST.txt");
			if (!dataSessionFile.exists())
				dataSessionFile.createNewFile();
			PrintStream dataSession = new PrintStream(dataSessionFile);
			DrivingPVT task = (DrivingPVT) tasks[0];
			dataSession.println("Day POST");

			dataSession.print("TimeOfDay" + ",");
			for (int i = 0; i < numberOfSessions; i++) 
				dataSession.print(task.sessions.get(i).getTimeOfTheDay() + ",");
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("BioMath" + ",");
			for (int i = 0; i < numberOfSessions; i++)
				dataSession.print(task.sessions.get(i).getBioMathValue() + ",");
			dataSession.print("\n");
			dataSession.flush();

			dataSession.print("AwakeTime" + ",");
			for (int i = 0; i < numberOfSessions; i++)
				dataSession.print(task.sessions.get(i).getTimeAwake() + ",");
			dataSession.print("\n");
			dataSession.flush();

			dataSession.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}

}
