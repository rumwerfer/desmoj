package emergency_p;

import desmoj.core.simulator.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import co.paralleluniverse.fibers.SuspendExecution;

public class PatientProcess extends SimProcess {
	
    private EmergencyModel model;
    private boolean emergency;
    
    private SimClock patientClock;
    private String name;
    private CSVFile file = CSVFile.getInstant();
    private PatientData patientData = PatientData.getInstant();
    private double arrivalTime = 0;
    private double beginTreat = 0;
    private double endTreat = 0;
    private double beginWait = 0;
    private double waiting2 = 0;
    private double treat = 0;
    private int treatment = 0;
    private double waiting1 = 0;
    private double waiting = 0;
    
    public PatientProcess(Model owner, String name, boolean showInTrace, boolean emergency) {
        super(owner, name, showInTrace);
        model = (EmergencyModel) owner;
        this.setQueueingPriority(emergency ? 3 : 1);
        this.emergency = emergency;
        this.name = name;
        patientClock = model.getExperiment().getSimClock(); 
    }

    public void lifeCycle() throws SuspendExecution {
    	arrivalTime = patientClock.getTime().getTimeAsDouble();

    	// each patient gets two treatments
    	for ( treatment = 0; treatment < 2; treatment++) {
    		
    		// patient waiting for second treatment enters queue
		    if (treatment > 0) {
		    	model.secTreatQueue.insert(this);
		    	
    		// emergency patient enters emergency queue
		    } else if (emergency) {
    			EmergencyModel.emergencyQueue.insert(this);
    			
    		// non-emergency patient enters regular queue
    		} else {
                EmergencyModel.patientQueue.insert(this);
    		}
    		
            sendTraceNote("patient queue length: " + (EmergencyModel.patientQueue.length() + EmergencyModel.emergencyQueue.length() + model.secTreatQueue.length()));
    		
            Optional<DocProcess> doc = findDoc();
            
            // no doc available, wait
            if (doc.isEmpty()) {
            	beginWait = patientClock.getTime().getTimeAsDouble();
                passivate();
            }
            
            // doc available, patient does not have to wait
            else {
            	EmergencyModel.countOfNonWaitingPatients.update();
                model.docQueue.remove(doc.get());
                doc.get().activateAfter(this);
                passivate(); // patient is being treated
            }
                        
            // write waiting and treatment time of patient to the patient data
            writePatient(treatment);
            
            // change priority after first treatment
            this.setQueueingPriority(2);
            sendTraceNote("patient received treatment " + treatment);
    	}
        sendTraceNote("patient leaves emergency room");
    	model.addToQuantil(endTreat - arrivalTime);

    }
    
    private Optional<DocProcess> findDoc() {
    	DocProcess doc = null;
        while (doc == null && !model.docQueue.isEmpty()) {
        	doc = model.docQueue.removeFirst();
        	if (doc.restIfShiftOver()) { // find new doc if shift is over
        		doc = null;
        	}
        }
        return Optional.ofNullable(doc);
    }
    
	public void writePatient(int treatment) {
    	treat = endTreat - beginTreat;
    	
    	if (treatment == 0) {
    		if (beginWait == 0) {
            	waiting1 = 0;
            	
            } else {
            	waiting1 = beginTreat - beginWait;
            }
    		
    		patientData.appendData(new ArrayList(Arrays.asList(name, (treatment+1), emergency, arrivalTime, waiting1, treat)));
    		file.appendData("\n " + name +", " + String.valueOf(treatment+1)  +", " +  String.valueOf(emergency)  +", " +  String.valueOf(arrivalTime)  +", " +  String.valueOf(waiting1)  +", " +  String.valueOf(treat));
        	
    	} else {
    		if (beginWait == 0) {
            	waiting2 = 0;
            	
            } else {
            	waiting2 = beginTreat - beginWait;
            }
    		
    		waiting = waiting1 + waiting2;
    		
    		if (waiting <= 5)
        		EmergencyModel.countOfMaxFiveMinWaitingTime.update();
        	
        	EmergencyModel.tallyWaiting.update(waiting);
        	
        	if (emergency)
        		EmergencyModel.tallyWaitingEmergency.update(waiting);
        	
        	if (!emergency)
        		EmergencyModel.tallyWaitingRegular.update(waiting);
        	
        	patientData.appendData(new ArrayList(Arrays.asList(name, (treatment+1), emergency, arrivalTime, waiting2, treat)));
        	file.appendData("\n " + name +", " + String.valueOf(treatment+1)  +", " +  String.valueOf(emergency)  +", " +  String.valueOf(arrivalTime)  +", " +  String.valueOf(waiting2)  +", " +  String.valueOf(treat));
    	}
    	
        beginWait = 0;
    }
    
    public int getTreatment() {
    	return treatment;
    }
    
    public double getTreatTime() {
    	return treat;
    }
    
    public void setBeginTreat(double value) {
    	beginTreat = value;
    }
    
    public void setEndTreat(double value) {
    	endTreat = value;
    }
    
    public double getWaitingTime(double time) {
    	if (beginWait == 0) {
    		return 0;
    	}
    	return time - beginWait;
    }
    
    public double getbeginWait() {
    	return beginWait;
    }

    boolean isEmergency() {
    	return emergency;
    }
    
    public void setTreatment(int count) {
    	treatment = count;
    }
}
