package emergency_p;

import desmoj.core.simulator.*;
import emergency_p.Emergency_model;
import co.paralleluniverse.fibers.SuspendExecution;

public class CounterProcess extends SimProcess{
	private Emergency_model myModel;
	
	public CounterProcess (Model owner, String name, boolean showInTrace) {
		super(owner, name, showInTrace);

	    myModel = (Emergency_model) owner;
	}
	
	public void lifeCycle() throws SuspendExecution {
        
        // Counter is open 24 hours
        while (true){
            
            // no patient is waiting
            if (myModel.patientQueue.isEmpty()) {
                
                // Schalter in entsprechende WS
                myModel.docQueue.insert(this);
                
                // abwarten weiterer Aktionen
                passivate();
            }
            
            // patients are waiting
            else {
                PatientProcess patient = myModel.patientQueue.first();
                myModel.patientQueue.remove(patient);
                
                // patient gets treatment
                hold(new TimeSpan(myModel.getTreatmentTime()));
                
                // patient is sent home or has to wait again for second treatment
                // TODO;
                patient.activate(new TimeSpan(0.0));
            }
        }
    }

}
