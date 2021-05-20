package emergency_p;

import desmoj.core.simulator.*;
import desmoj.core.dist.*;

public class EmergencyModel extends Model {
	
	private ContDistExponential patientArrivalTime; // random numbers defined by mean
    private ContDistUniform treatmentTime;			// defined by min and max
    protected ProcessQueue<PatientProcess> patientQueue;
   	protected ProcessQueue<DocProcess> docQueue;
   	
	public EmergencyModel(Model owner, String name, boolean showInReport, boolean showIntrace) {
		super(owner, name, showInReport, showIntrace);
	}
	
	public String description() {
    	return "Emergency Model (process oriented): " +
               "simulates an emergency room with patients waiting for their treatment, " +
               "two doctors treating the patients.";
    }	

	public void doInitialSchedules() {

        NewPatientProcess newPatient = new NewPatientProcess(this, "patient creation", true);
        newPatient.activate();

        DocProcess doc1 = new DocProcess(this, "doc", true);
        DocProcess doc2 = new DocProcess(this, "doc", true);
        doc1.activate();
        doc2.activate();
    }
	
	public void init() {
		// mean of arrival time = 40
    	patientArrivalTime = new ContDistExponential(this, "patient arrival time", 40.0, true, true);
    	patientArrivalTime.setNonNegative(true); // prevent negative arrival times

    	// treatment takes 10 to 30 minutes (evenly distributed)
        treatmentTime = new ContDistUniform(this, "treatment time", 10.0, 30.0, true, true);	

       	patientQueue = new ProcessQueue<PatientProcess>(this, "patient queue",true, true);	
    	docQueue = new ProcessQueue<DocProcess>(this, "doc queue",true, true);
    }
	
	public static void main(String[] args) {
		Experiment emergencyExp = new Experiment("emergency room");
		EmergencyModel emergencyModel = new EmergencyModel(null, "emergency room model", true, true);  
		emergencyModel.connectToExperiment(emergencyExp);
		
		// stop simulation after 22 days
		TimeInstant startTime = new TimeInstant(0.0);
		TimeInstant endTime = new TimeInstant(31680);
		
		emergencyExp.tracePeriod(startTime, endTime);
		emergencyExp.debugPeriod(startTime, endTime);
		emergencyExp.stop(endTime);

		emergencyExp.start(); 
		emergencyExp.report();
		emergencyExp.finish();
	}
	
    public double getPatientArrivalTime() {
	   return patientArrivalTime.sample();
    }

    public double getTreatmentTime() {
        return treatmentTime.sample();
    }
}
