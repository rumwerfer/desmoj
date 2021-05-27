package emergency_p;

import desmoj.core.simulator.*;
import co.paralleluniverse.fibers.SuspendExecution;

public class NewPatientProcess extends SimProcess {
	
    private EmergencyModel model;
    private int count = 0;

    public NewPatientProcess (Model owner, String name, boolean showInTrace) {
	   super(owner, name, showInTrace);
	   model = (EmergencyModel) owner;
    }
    
    public void lifeCycle() throws SuspendExecution {
	
        while (true) {
        	
            // wait for predefined time
            hold (new TimeSpan(model.getPatientArrivalTime()));
     
            // create new patient
            PatientProcess newPatient = new PatientProcess (model, String.valueOf(count), true, model.isEmergency());
            count++;
            // new patient enters emergency room
            newPatient.activateAfter(this);
        }
    }

}
