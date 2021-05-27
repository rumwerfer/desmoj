package emergency_p;

import java.util.ArrayList;

/**
 * This class provides a array list structure with a singleton constructor.
 * There can only be one instant of this class.
 * It consists of an array list of array lists: one entry (array list) for every treatment of a patient
 * @author Sofia Bonini, Sabine Hasenleithner, Dario Hornsteiner
 *
 */
public class PatientData {
	private ArrayList<ArrayList<?>> patients;
	private static PatientData patientData = null;

    private PatientData() {
        patients = new ArrayList<ArrayList<?>>();
    }
	
	public static PatientData getInstant() {
		if (patientData == null)
			patientData = new PatientData();
		return patientData;
	}
	
	public void appendData(ArrayList<?> arrlist) {
        patients.add(arrlist);
    }

    public ArrayList<ArrayList<?>> getData() {
        return patients;
    }

    public String toString() {
    	StringBuilder str = new StringBuilder();
    	
    	for(int i = 0; i < patients.size(); i++) {
    		str.append(patients.get(i).toString() + "\n");
    	}
    	
    	return str.toString();
    }
}
