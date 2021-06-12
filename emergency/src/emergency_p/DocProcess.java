package emergency_p;

import desmoj.core.exception.DelayedInterruptException;
import desmoj.core.exception.InterruptException;
import desmoj.core.simulator.*;

import co.paralleluniverse.fibers.SuspendExecution;

public class DocProcess extends SimProcess{
	
	private EmergencyModel model;
	//private CSVFile file = CSVFile.getInstant();
	private SimClock docClock;
	private int shiftStart;
	private int shiftEnd;
	
	public DocProcess (Model owner, String name, boolean showInTrace, int shiftStart, int shiftEnd) {
		super(owner, name, showInTrace);
	    model = (EmergencyModel) owner;
	    docClock = model.getExperiment().getSimClock();
	    this.shiftStart = shiftStart;
	    this.shiftEnd = shiftEnd;
	}
	
	public void lifeCycle() throws SuspendExecution {
        
        while (true) {
        	restIfShiftOver();
            
        	// no waiting emergencies
        	if (EmergencyModel.emergencyQueue.isEmpty()) {
        		// no waiting patients for second treatment
        		if (model.secTreatQueue.isEmpty()) {
        			// no waiting patients -> wait
           		 	if (EmergencyModel.patientQueue.isEmpty()) {
                       model.docQueue.insert(this);
                       passivate();
                       
                    // waiting patients -> treat first patient in queue
                    } else {
                   	   PatientProcess patient = EmergencyModel.patientQueue.first();
                       EmergencyModel.patientQueue.remove(patient);
                       patient.setBeginTreat(docClock.getTime().getTimeAsDouble());
                       
                       // patient gets treatment
                       hold(new TimeSpan(model.getTreatmentTime(patient, false, false, patient.getTreatment())));
                       
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
                    hold(new TimeSpan(model.getTreatmentTime(patient, false, false, patient.getTreatment())));
                    
                    // patient is sent home or has to wait again for second treatment
                    patient.activate(new TimeSpan(0.0));
                    patient.setEndTreat(docClock.getTime().getTimeAsDouble());
        		}
        	
        	// waiting emergencies -> treat first emergency in queue
        	} else {
        		PatientProcess patient = EmergencyModel.emergencyQueue.first();
                EmergencyModel.emergencyQueue.remove(patient);
                
                	if (patient.getWaitingTime(docClock.getTime().getTimeAsDouble()) > model.getDeathTime()) { // patient dies after 15 minutes
                	patient.setBeginTreat(docClock.getTime().getTimeAsDouble());
                	EmergencyModel.countOfDeadPatients.update();
                	sendTraceNote("patient dies in waiting queue");
                    patient.setTreatment(2);
                    patient.setEndTreat(docClock.getTime().getTimeAsDouble());                	
                    patient.activate(new TimeSpan(0.0));
                    
                	
                } else {
                	patient.setBeginTreat(docClock.getTime().getTimeAsDouble());
                    
                    // patient gets treatment
                    hold(new TimeSpan(model.getTreatmentTime(patient, true, false, patient.getTreatment())));
                    
                    // patient is sent home or has to wait again for second treatment
                    patient.activate(new TimeSpan(0.0));
                    patient.setEndTreat(docClock.getTime().getTimeAsDouble());

                }
        	}
        }
    }
	
	boolean restIfShiftOver() {
		if (shiftEnd != shiftStart && model.getDayHours() > shiftEnd) {
			try {
				hold(new TimeSpan(getOffHours() * 60 - getOvertimeInMinutes()));
			} catch (InterruptException | SuspendExecution e) {
				throw new RuntimeException(e);
			}
			return true;
		}
		return false;
	}
	
	private int getOvertimeInMinutes() {
		int currentTimeInMinutes = model.getDayMinutes();
		int shiftEndInMinutes = shiftEnd * 60;
		
		if (currentTimeInMinutes > shiftEndInMinutes) {
			return currentTimeInMinutes - shiftEndInMinutes;
		} else { // overtime passed midnight
			return currentTimeInMinutes + 1440 - shiftEndInMinutes;
		}
	}
	
	private int getOffHours() {
		if (shiftEnd > shiftStart) {
			return shiftEnd - shiftStart;
		} else {
			return 24 - shiftEnd + shiftStart;
		}
	}
}

