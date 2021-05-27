package emergency_p;

import java.util.ArrayList;

public class PatientData {
	private ArrayList<ArrayList> patients;
	private static PatientData patientData = null;

    private PatientData() {
        patients = new ArrayList();
    }
	
	public static PatientData getInstant() {
		if (patientData == null)
			patientData = new PatientData();
		return patientData;
	}
	
	public void appendData(ArrayList arrlist) {
        patients.add(arrlist);
    }

    public ArrayList getData() {
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
