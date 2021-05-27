package emergency_p;

/**
 * This class provides a csv-file structure with a singleton constructor.
 * There can only be one instant of this class.
 * @author Sofia Bonini, Sabine Hasenleithner, Dario Hornsteiner
 *
 */
public class CSVFile {
	private StringBuilder data;
	private static CSVFile csvFile = null;
	
	private CSVFile() {
		// empty constructor
	}

    private CSVFile(String header) {
        data = new StringBuilder();
        data.append(header);
    }
	
	public static CSVFile getInstant() {
		if (csvFile == null)
			csvFile = new CSVFile("patient, treatment, emergency, arrivalTime, beginWaitingTime, endWaitingTime, beginTreatmentTime, endTreatmentTime");
		return csvFile;
	}
	
	public void appendData(String str) {
        data.append(str);
    }

    public StringBuilder getData() {
        return data;
    }
}
