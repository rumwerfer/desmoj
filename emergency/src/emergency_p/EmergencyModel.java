package emergency_p;

import desmoj.core.simulator.*;
import desmoj.core.statistic.Count;
import desmoj.core.statistic.Tally;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import desmoj.core.dist.*;

public class EmergencyModel extends Model {
	
	private ContDistExponential patientArrivalTime; // random numbers defined by mean
    private ContDistUniform treatmentTime;
    private ContDistUniform isEmergency;
    protected static Count countOfNonWaitingPatients;
    protected static Count countOfMaxFiveMinWaitingTime;
    protected static Count countOfDeadPatients;
    protected static Tally tallyWaiting;
    protected static Tally tallyWaitingRegular;
    protected static Tally tallyWaitingEmergency;
    protected static ProcessQueue<PatientProcess> patientQueue;
    protected static ProcessQueue<PatientProcess> emergencyQueue;
    protected ProcessQueue<PatientProcess> secTreatQueue;
   	protected ProcessQueue<DocProcess> docQueue;
   	private static CSVFile file = CSVFile.getInstant();
    private static PatientData patientData = PatientData.getInstant();
   	private static ArrayList<Double> quantil= new ArrayList<Double>();
   	private static double quantil90;
   	
   	private double shareOfEmergencies = 0.2; // percentage of how many patients are emergencies
   	private double meanArrivalTime = 40.0; // on average a patient arrives every 40 minutes
   	private double meanTreatmentTime = 25.0; // non-emergency cases
   	private double emergencyTimeFactor = 2.0; // how much longer treatments of emergency cases take
   	
	public EmergencyModel(Model owner, String name, boolean showInReport, boolean showIntrace) {
		super(owner, name, showInReport, showIntrace);
	}
	
	public String description() {
    	return "Emergency Model (process oriented): " +
               "simulates an emergency room with patients waiting for their treatment, " +
               "two doctors treating the patients.";
    }	

	public void doInitialSchedules() {

        NewPatientProcess patientCreator = new NewPatientProcess(this, "patient creation", true);
        patientCreator.activate();

        DocProcess doc1 = new DocProcess(this, "doc", true);
        DocProcess doc2 = new DocProcess(this, "doc", true);
//        DocProcess doc3 = new DocProcess(this, "doc", true);
        doc1.activate();
        doc2.activate();
//        doc3.activate();
    }
	
	public void init() {		
		// negative-exponentially distributed
    	patientArrivalTime = new ContDistExponential(this, "patient arrival time", meanArrivalTime, true, true);
    	patientArrivalTime.setNonNegative(true); // prevent negative arrival times
    	
    	// Counter for statistical evaluation
    	countOfNonWaitingPatients = new Count(this, "number of not-waiting patients", true, false);
    	countOfMaxFiveMinWaitingTime = new Count(this, "number of patients with waiting time < 5", true, false);
    	countOfDeadPatients = new Count(this, "number of emergenncy patients who would have died", true, false);
    	
    	// Tallies for statistical evaluation
    	tallyWaiting = new Tally(this, "waiting time", true, false);
    	tallyWaitingRegular = new Tally(this, "non-emergency patients waiting Time", true, false);
    	tallyWaitingEmergency = new Tally(this, "emergency patients waiting Time", true, false);

    	// treatment takes 15 minutes to 1 hour (evenly distributed)
        treatmentTime = new ContDistUniform(this, "treatment time", meanTreatmentTime-10, meanTreatmentTime+35, true, true);	
        treatmentTime.setNonNegative(true); // prevent negative treatment times
        
        // patients below 0.2 are emergencies -> 20%
        isEmergency = new ContDistUniform(this, "emergency", 0.0, 1.0, true, true);

       	patientQueue = new ProcessQueue<PatientProcess>(this, "patient queue",true, true);	
       	emergencyQueue = new ProcessQueue<PatientProcess>(this, "emergency queue",true, true);
       	secTreatQueue = new ProcessQueue<PatientProcess>(this, "second Treatment queue",true, true);
    	docQueue = new ProcessQueue<DocProcess>(this, "doc queue",true, true);
    	docQueue.setQueueCapacity(2); // only two doctors are available
    }
	
	public static void main(String[] args) {
		Experiment emergencyExp = new Experiment("emergency");
		EmergencyModel emergencyModel = new EmergencyModel(null, "emergency model", true, true);  
		emergencyModel.connectToExperiment(emergencyExp);
		emergencyExp.setShowProgressBarAutoclose(true);
		
		// stop simulation after 22 days
		TimeInstant startTime = new TimeInstant(0.0);
		TimeInstant TwoDaysTime = new TimeInstant(2880.0); // 2 * 24 * 60
		TimeInstant endTime = new TimeInstant(31680); // 22 * 24 * 60
		
		// ----------------------------------------------------initialization-phase--------------------------------------------------------------//
		emergencyExp.tracePeriod(startTime, endTime);
		emergencyExp.debugPeriod(startTime, endTime);
		emergencyExp.stop(TwoDaysTime); // result after 2 days

		emergencyExp.start(); 
		
		System.out.println("\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> start 2 days initialization <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		printStatistics();
		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> end 2 days initialization <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n");
				
		// ----------------------------------------------------initialization-phase--------------------------------------------------------------//
		
		emergencyExp.stop(endTime); // result after 20 days

		emergencyExp.proceed(); 
		emergencyExp.report();
		emergencyExp.finish();
		
		System.out.println("\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> start 20 days simulation <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		printStatistics();
		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> end 20 days simulation <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");

		// CSV File
		PrintWriter writer;
		try {
			writer = new PrintWriter(new File("PatientTimes.csv"));
			writer.write((file.getData().toString()));
            writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    public double getPatientArrivalTime() {
    	double time = patientArrivalTime.sample();
    	return time;
    }
    
    public void addToQuantil(Double time) {
    	quantil.add(time);
    }

    public double getTreatmentTime(PatientProcess p, boolean emergency, int treatment) {
    	double time = treatmentTime.sample();
    	ContDistUniform secondTreatment = new ContDistUniform(this, "second treatment time", 5, p.getTreatTime(), false, true);
    	
    	// time for second treatment is shorter
    	if (treatment > 0) {
    		return secondTreatment.sample();
    	}

    	return emergency ? time * emergencyTimeFactor : time;
    }
    
    // 20% of all patients are emergencies
    public boolean isEmergency() {
    	return isEmergency.sample() < shareOfEmergencies;
    }
    
    public static void get90Quantil() {
    	int result = 0;
    	
    	Collections.sort(quantil);  
    	result = (int) (quantil.size() * 0.9);
    	
    	if(result < quantil.size())
    		quantil90 = quantil.get(result);
    	else
    		quantil90 = 0.0;
    }
    
    public static void printStatistics() {
    	get90Quantil();
    	System.out.println();
		
		// max and avg number of waiting patients of both groups
		System.out.println("max. number of waiting regular patients: " + patientQueue.maxLength() + " patients");
		System.out.println("max. number of waiting emergency patients: " + emergencyQueue.maxLength() + " emergency patients");
		System.out.println("avg. number of waiting regular patients: " + patientQueue.averageLength() + " patients");
		System.out.println("avg. number of waiting emergency patients: " + emergencyQueue.averageLength() + " emergency patients");
		
		// mean waiting time of both groups
		System.out.println("mean waiting time of both patient groups: " + tallyWaiting.getMean() + " minutes");
		
		// number of non-waiting patients and less-than-5-min-waiting patients
		System.out.println("number of non-waiting patients: " + countOfNonWaitingPatients.getValue() + " patients");
		System.out.println("number of patients with waiting time <= 5: " + countOfMaxFiveMinWaitingTime.getValue() + " patients");
		
		// 90% quantiles
		System.out.println("90%-Quantile : " + quantil90 + " minutes");
		
		System.out.println("number of emergency patients who would be dead: " + countOfDeadPatients.getValue() + " patients");
		System.out.println("max. waiting time of emergency patients: " + tallyWaitingEmergency.getMaximum());
		System.out.println("max. waiting time of regular patients: " + tallyWaitingRegular.getMaximum());
		System.out.println("min. waiting time of emergency patients: " + tallyWaitingEmergency.getMinimum());
		System.out.println("min. waiting time of regular patients: " + tallyWaitingRegular.getMinimum());
		System.out.println("mean waiting time of emergency patients: " + tallyWaitingEmergency.getMean());
		System.out.println("mean waiting time of regular patients: " + tallyWaitingRegular.getMean());
		
		System.out.println();
    }
}
