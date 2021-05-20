package emergency_p;

import desmoj.core.simulator.*;
import emergency_p.Emergency_model;
import emergency_p.CounterProcess;
import co.paralleluniverse.fibers.SuspendExecution;

public class PatientProcess extends SimProcess {
    private Emergency_model myModel;

    public PatientProcess(Model owner, String name, boolean showInTrace) {
        super(owner, name, showInTrace);

        myModel = (Emergency_model) owner;
    }

    public void lifeCycle() throws SuspendExecution{

        // patient enters emergency room --> goes into queue
        myModel.patientQueue.insert(this);
        sendTraceNote("patient queue length: " + 
            myModel.patientQueue.length());

        // at least one of the working doctors is available
        if (!myModel.freeCounterQueue.isEmpty()) {
            CounterProcess counter = myModel.freeCounterQueue.first();
            myModel.freeCounterQueue.remove(counter);
            
            counter.activateAfter(this);
            
            // patient is being treated
            passivate();
        }
        // no doctor is available
        else {
            passivate();
        }
        
        // patient was treated and leaves or waits again for second treatment
        // TODO;
        sendTraceNote("patient was treated and leaves emergency room");
    }
}
