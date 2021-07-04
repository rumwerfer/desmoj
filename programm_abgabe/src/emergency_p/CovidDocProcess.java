package emergency_p;

import desmoj.core.simulator.*;

import co.paralleluniverse.fibers.SuspendExecution;

/**
 * This class handles the special COVID-docs
 * 
 * @author Sofia Bonini, Dario Hornsteiner, Sabine Hasenleithner
 */

public class CovidDocProcess extends SimProcess{
	private EmergencyModel model;
	private SimClock docClock;
	
	public CovidDocProcess (Model owner, String name, boolean showInTrace) {
		super(owner, name, showInTrace);
	    model = (EmergencyModel) owner;
	    docClock = model.getExperiment().getSimClock();
	}
	
	public void lifeCycle() throws SuspendExecution {
		while (true) {
			if (EmergencyModel.covidQueue.isEmpty()) {
				model.covidDocQueue.insert(this);
				passivate();
			} else {
				PatientProcess patient = EmergencyModel.covidQueue.first();
                EmergencyModel.covidQueue.remove(patient);
                patient.setBeginTreat(docClock.getTime().getTimeAsDouble());
                
                // patient gets treatment
                hold(new TimeSpan(model.getTreatmentTime(patient, false, true, patient.getTreatment())));
                
                // patient is sent home
                patient.activate(new TimeSpan(0.0));
                patient.setEndTreat(docClock.getTime().getTimeAsDouble());
                
                // doctor has to desinfect
                hold(new TimeSpan(10.0));
			}
		}
	}

}
