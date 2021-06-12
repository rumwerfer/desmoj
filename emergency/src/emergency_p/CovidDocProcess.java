package emergency_p;

import desmoj.core.simulator.*;

import co.paralleluniverse.fibers.SuspendExecution;

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
			if (model.covidQueue.isEmpty()) {
				model.covidDocQueue.insert(this);
				passivate();
			} else {
				PatientProcess patient = model.covidQueue.first();
                model.covidQueue.remove(patient);
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
