package emergency_p;

import desmoj.core.simulator.*;
import co.paralleluniverse.fibers.SuspendExecution;

public class DocProcess extends SimProcess{
	
	private EmergencyModel model;
	//private CSVFile file = CSVFile.getInstant();
	private SimClock docClock;
	
	public DocProcess (Model owner, String name, boolean showInTrace) {
		super(owner, name, showInTrace);
	    model = (EmergencyModel) owner;
	    docClock = model.getExperiment().getSimClock();
	}
	
	public void lifeCycle() throws SuspendExecution {
        
        // doc is working 24 hours
        while (true) {
            
        	// no waiting emergencies
        	if (model.emergencyQueue.isEmpty()) {
        		// no waiting patients for second treatment
        		if (model.secTreatQueue.isEmpty()) {
        			// no waiting patients -> wait
           		 	if (model.patientQueue.isEmpty()) {
                       model.docQueue.insert(this);
                       passivate();
                    // waiting patients -> treat first patient in queue
                    } else {
                   	   PatientProcess patient = model.patientQueue.first();
                       model.patientQueue.remove(patient);
                       patient.setBeginTreat(docClock.getTime().getTimeAsDouble());
                       
                       // patient gets treatment
                       hold(new TimeSpan(model.getTreatmentTime(patient, false, patient.getTreatment())));
                       
                       // patient is sent home or has to wait again for second treatment
                       patient.activate(new TimeSpan(0.0));
                       patient.setEndTreat(docClock.getTime().getTimeAsDouble());
                    }
           		// waiting patients for second treatment -> treat first patients waiting for second treatment in queue
        		} else {
        			PatientProcess patient = model.secTreatQueue.first();
                    model.secTreatQueue.remove(patient);
                    patient.setBeginTreat(docClock.getTime().getTimeAsDouble());
                    
                    // patient gets treatment
                    hold(new TimeSpan(model.getTreatmentTime(patient, false, patient.getTreatment())));
                    
                    // patient is sent home or has to wait again for second treatment
                    patient.activate(new TimeSpan(0.0));
                    patient.setEndTreat(docClock.getTime().getTimeAsDouble());
        		}
        	
        	// waiting emergencies -> treat first emergency in queue
        	} else {
        		PatientProcess patient = model.emergencyQueue.first();
                model.emergencyQueue.remove(patient);
                patient.setBeginTreat(docClock.getTime().getTimeAsDouble());
                
                // patient gets treatment
                hold(new TimeSpan(model.getTreatmentTime(patient, true, patient.getTreatment())));
                
                // patient is sent home or has to wait again for second treatment
                patient.activate(new TimeSpan(0.0));
                patient.setEndTreat(docClock.getTime().getTimeAsDouble());
        	}
        }
    }
}
