package emergency_p;

import desmoj.core.simulator.*;
import emergency_p.EmergencyModel;
import co.paralleluniverse.fibers.SuspendExecution;

public class DocProcess extends SimProcess{
	
	private EmergencyModel model;
	
	public DocProcess (Model owner, String name, boolean showInTrace) {
		super(owner, name, showInTrace);
	    model = (EmergencyModel) owner;
	}
	
	public void lifeCycle() throws SuspendExecution {
        
        // doc is working 24 hours
        while (true) {
            
            // no waiting patients -> wait
            if (model.patientQueue.isEmpty()) {
                model.docQueue.insert(this);
                passivate();
            }
            
            // waiting patients -> treat first patient in queue
            else {
                PatientProcess patient = model.patientQueue.first();
                model.patientQueue.remove(patient);
                
                // patient gets treatment
                hold(new TimeSpan(model.getTreatmentTime()));
                
                // patient is sent home or has to wait again for second treatment
                patient.activate(new TimeSpan(0.0));
            }
        }
    }
}
