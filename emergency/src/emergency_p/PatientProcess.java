package emergency_p;

import desmoj.core.simulator.*;
import emergency_p.EmergencyModel;
import emergency_p.DocProcess;
import co.paralleluniverse.fibers.SuspendExecution;

public class PatientProcess extends SimProcess {
	
    private EmergencyModel model;
    private Priority priority;
    private boolean isEmergency;

    public PatientProcess(Model owner, String name, boolean showInTrace, boolean isEmergency) {
        super(owner, name, showInTrace);
        model = (EmergencyModel) owner;
        priority = isEmergency ? Priority.HIGH : Priority.LOW;
        this.isEmergency = isEmergency;
    }

    public void lifeCycle() throws SuspendExecution {

    	// each patient gets two treatments
    	for (int treatment = 0; treatment < 2; treatment++) {
    		
    		// patient enters queue
            model.patientQueue.insert(this);
            sendTraceNote("patient queue length: " + model.patientQueue.length());

            // at least one of the working doctors is available
            if (!model.docQueue.isEmpty()) {
                DocProcess doc = model.docQueue.first();
                model.docQueue.remove(doc);
                
                doc.activateAfter(this);
                
                // patient is being treated
                passivate();
            }
            
            // no doctor is available
            else {
                passivate();
            }
            
            // change priority after first treatment
            priority = Priority.MEDIUM;
            
            sendTraceNote("patient received treatment " + treatment);
    	}
    	
        sendTraceNote("patient leaves emergency room");
    }

}
