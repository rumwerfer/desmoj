package emergency_p;

import desmoj.core.simulator.*;
import desmoj.core.statistic.Count;
import desmoj.core.statistic.Tally;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import desmoj.core.dist.*;

public class EmergencyModel extends Model {
	
	private ContDistExponential patientArrivalTime; // random numbers defined by mean
    private ContDistUniform treatmentTime;
    private ContDistUniform isEmergency;
    protected Count countOfNonWaitingPatients;
    protected Count countOfMaxFiveMinWaitingTime;
    protected Tally tallyWaiting;
    protected Tally tallyWaitingRegular;
    protected Tally tallyWaitingEmergency;
    protected ProcessQueue<PatientProcess> patientQueue;
    protected ProcessQueue<PatientProcess> emergencyQueue;
    protected ProcessQueue<PatientProcess> secTreatQueue;
   	protected ProcessQueue<DocProcess> docQueue;
   	//private static CSVFile file = CSVFile.getInstant();
   	private static PatientData patientData = PatientData.getInstant();
   	
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
        doc1.activate();
        doc2.activate();
    }
	
	public void init() {		
		// negative-exponentially distributed
    	patientArrivalTime = new ContDistExponential(this, "patient arrival time", meanArrivalTime, true, true);
    	patientArrivalTime.setNonNegative(true); // prevent negative arrival times
    	
    	// Counter for statistical evaluation
    	countOfNonWaitingPatients = new Count(this, "number of not-waiting patients", true, false);
    	countOfMaxFiveMinWaitingTime = new Count(this, "number of patients with waiting time < 5", true, false);
    	
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
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		Experiment emergencyExp = new Experiment("emergency");
		EmergencyModel emergencyModel = new EmergencyModel(null, "emergency model", true, true);  
		emergencyModel.connectToExperiment(emergencyExp);
		emergencyExp.setShowProgressBarAutoclose(true);
		
		// stop simulation after 22 days
		TimeInstant startTime = new TimeInstant(0.0);
		TimeInstant endTime = new TimeInstant(31680); // 22 * 24 * 60
		
		emergencyExp.tracePeriod(startTime, endTime);
		emergencyExp.debugPeriod(startTime, endTime);
		emergencyExp.stop(endTime);

		emergencyExp.start(); 
		emergencyExp.report();
		emergencyExp.finish();
		
		// sort patientID's
		Collections.sort(patientData.getData(), new Comparator<ArrayList<String>>() {    
			@Override
			public int compare(ArrayList<String> o1, ArrayList<String> o2) {
				return (o1.get(0)).compareTo(o2.get(0));
			}               
		});
		
		//System.out.println(patientData.toString());
		
		/*
		PrintWriter writer;
		try {
			writer = new PrintWriter(new File("PatientTimes.csv"));
			writer.write((file.getData().toString()));
            writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
    public double getPatientArrivalTime() {
    	double time = patientArrivalTime.sample();
    	return time;
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
}
