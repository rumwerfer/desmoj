package emergency_p;

import desmoj.core.simulator.*;
import emergency_p.Emergency_model;
import emergency_p.PatientProcess;
import co.paralleluniverse.fibers.SuspendExecution;

public class NewPatientProcess extends SimProcess {
    private Emergency_model myModel;

    public NewPatientProcess (Model owner, String name, boolean showInTrace) {
	   super(owner, name, showInTrace);

	   myModel = (Emergency_model) owner;
    }
    
    public void lifeCycle() throws SuspendExecution {
	
        while (true) {
            // hold this process until new patient arrives
            hold (new TimeSpan(myModel.getPatientArrivalTime()));
     
            // create new patient
            PatientProcess newPatient = new PatientProcess (myModel, "patient", true);
    
            // new patient enters emergency room
            newPatient.activateAfter(this);
    
        }
    }

}
