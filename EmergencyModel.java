package emergency_p;

import desmoj.core.simulator.*;
import desmoj.core.statistic.Count;
import desmoj.core.statistic.Tally;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYDotRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;

import desmoj.core.dist.*;

public class EmergencyModel extends Model {
	
	private ContDistExponential patientArrivalTime; // random numbers defined by mean
    private ContDistUniform treatmentTime;
    private ContDistUniform isEmergency;
    protected static Count countOfNonWaitingPatients;
    protected static Count countOfMaxFiveMinWaitingTime;
    protected static Count countOfDeadPatients;
    protected static Tally tallyWaiting;
    protected static Tally tallyWaitingRegular;
    protected static Tally tallyWaitingEmergency;
    protected static ProcessQueue<PatientProcess> patientQueue;
    protected static ProcessQueue<PatientProcess> emergencyQueue;
    protected ProcessQueue<PatientProcess> secTreatQueue;
   	protected ProcessQueue<DocProcess> docQueue;
   	private static CSVFile file = CSVFile.getInstant();
    private static PatientData patientData = PatientData.getInstant();
   	private static ArrayList<Double> quantil= new ArrayList<Double>();
   	private static double quantil90;
   	static double[] meanWaiting;
   	
   	
   	private static XYSeries maxNumberWaitingRegular = new XYSeries("max. number of waiting regular patients");
   	private static XYSeries maxNumberWaitingEmergency = new XYSeries("max. number of waiting emergency patients");
   	private static XYSeries avgNumberWaitingRegular = new XYSeries("avg. number of waiting regular patients");
   	private static XYSeries avgNumberWaitingEmergency = new XYSeries("avg. number of waiting emergency patients");
   	
   	private static XYSeries meanWaitingTimeBoth = new XYSeries("mean waiting time of both patient groups");
	
   	private static XYSeries nonWaiting  = new XYSeries("number of non-waiting patients");
   	private static XYSeries less5Waiting = new XYSeries("number of patients with waiting time <= 5");
	
   	private static XYSeries quantile90Percent = new XYSeries("90%-Quantile");
	
   	private static XYSeries deadPatients = new XYSeries("number of emergency patients who would be dead");
   	
   	private static XYSeries totalPatients = new XYSeries("number of total patients");
   	
//   	private static XYSeries maxWaitingTimeEmergency = new XYSeries("max. waiting time of emergency patients");
//   	private static XYSeries maxWaitingTimeRegular = new XYSeries("max. waiting time of regular patients");
//   	private static XYSeries minWaitingTimeEmergency = new XYSeries("min. waiting time of emergency patients");
//   	private static XYSeries minWaitingTimeRegular = new XYSeries("min. waiting time of regular patients");
//   	private static XYSeries avgWaitingTimeEmergency = new XYSeries("mean waiting time of emergency patients");
//   	private static XYSeries avgWaitingTimeRegular = new XYSeries("mean waiting time of regular patients");

   	

   	
   	private double shareOfEmergencies = 0.2; // percentage of how many patients are emergencies
   	private double meanArrivalTime = 40.0; // on average a patient arrives every 40 minutes
   	private double meanTreatmentTime = 25.0; // non-emergency cases
   	private double emergencyTimeFactor = 2.0; // how much longer treatments of emergency cases take
   	
   	private static long genRandom() {
    	Random random = new Random();
    	return Math.abs(random.nextLong());
    }
   	
	public EmergencyModel(Model owner, String name, boolean showInReport, boolean showIntrace) {
		super(owner, name, showInReport, showIntrace);
	}
	
	public String description() {
    	return "Emergency Model (process oriented): " +
               "simulates an emergency room with patients waiting for their treatment, " +
               "two doctors treating the patients.";
    }	

	public void doInitialSchedules() {

        NewPatientProcess patientCreator = new NewPatientProcess(this, "patient creation", true);
        patientCreator.activate();
        //DocProcess [] doctores = new DocProcess[20];
        //for (int index = 0; index < 20; index++) {
        //	doctores[index] = new DocProcess(this, "doc", true);
        // 	doctores[index].activate();
        //}

        DocProcess doc1 = new DocProcess(this, "doc", true);
        DocProcess doc2 = new DocProcess(this, "doc", true);
        //DocProcess doc3 = new DocProcess(this, "doc", true);
       
        doc1.activate();
        doc2.activate();
        //doc3.activate();

    }
	
	public void init() {		
		// negative-exponentially distributed
    	patientArrivalTime = new ContDistExponential(this, "patient arrival time", meanArrivalTime, true, true);
    	patientArrivalTime.setNonNegative(true); // prevent negative arrival times
    	
    	// Counter for statistical evaluation
    	countOfNonWaitingPatients = new Count(this, "number of not-waiting patients", true, false);
    	countOfMaxFiveMinWaitingTime = new Count(this, "number of patients with waiting time < 5", true, false);
    	countOfDeadPatients = new Count(this, "number of emergenncy patients who would have died", true, false);
    	
    	// Tallies for statistical evaluation
    	tallyWaiting = new Tally(this, "waiting time", true, false);
    	tallyWaitingRegular = new Tally(this, "non-emergency patients waiting Time", true, false);
    	tallyWaitingEmergency = new Tally(this, "emergency patients waiting Time", true, false);

    	// treatment takes 15 minutes to 1 hour (evenly distributed)
        treatmentTime = new ContDistUniform(this, "treatment time", meanTreatmentTime-10, meanTreatmentTime+35, true, true);	
        treatmentTime.setNonNegative(true); // prevent negative treatment times
        
        // patients below 0.2 are emergencies -> 20%
        isEmergency = new ContDistUniform(this, "emergency", 0.0, 1.0, true, true);

       	patientQueue = new ProcessQueue<PatientProcess>(this, "patient queue",true, true);	
       	emergencyQueue = new ProcessQueue<PatientProcess>(this, "emergency queue",true, true);
       	secTreatQueue = new ProcessQueue<PatientProcess>(this, "second Treatment queue",true, true);
    	docQueue = new ProcessQueue<DocProcess>(this, "doc queue",true, true);
    	//docQueue.setQueueCapacity(20); // only two doctors are available
    	
    	
    }
	
	public static void main(String[] args) {
		int runs = 20;
		
		 meanWaiting = new double[runs];
		
		
		for (int index = 0; index < runs; index++) {
			Experiment emergencyExp = new Experiment("emergency");
			EmergencyModel emergencyModel = new EmergencyModel(null, "emergency model", true, true);  
			emergencyModel.connectToExperiment(emergencyExp);
			emergencyExp.setShowProgressBarAutoclose(true);
			
			// stop simulation after 22 days
			TimeInstant startTime = new TimeInstant(0.0);
			TimeInstant TwoDaysTime = new TimeInstant(2880.0); // 2 * 24 * 60
			TimeInstant endTime = new TimeInstant(31680); // 22 * 24 * 60
			
			// set Random Seed
	        emergencyExp.setSeedGenerator(genRandom());
			
			// ----------------------------------------------------initialization-phase--------------------------------------------------------------//
			emergencyExp.tracePeriod(startTime, endTime);
			emergencyExp.debugPeriod(startTime, endTime);
			emergencyExp.stop(TwoDaysTime); // result after 2 days
	
			emergencyExp.start(); 
			
			System.out.println("\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> start 2 days initialization <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
			//printStatistics(index);
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> end 2 days initialization <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n");
			
			
					
			// ----------------------------------------------------initialization-phase--------------------------------------------------------------//
			
			emergencyExp.stop(endTime); // result after 20 days
	
			emergencyExp.proceed(); 
			emergencyExp.report();
			emergencyExp.finish();
			
			System.out.println("\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> start 20 days simulation <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
			printStatistics(index);
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> end 20 days simulation <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
			
			
	
			// CSV File
			PrintWriter writer;
			try {
				writer = new PrintWriter(new File("PatientTimes.csv"));
				writer.write((file.getData().toString()));
	            writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
  	
    	//*******************Diagrams****************************************
		
    	// Diagram1 for max and average number of waiting patients
		XYSeriesCollection dataset1 = new XYSeriesCollection();
    	dataset1.addSeries(maxNumberWaitingRegular);
    	dataset1.addSeries(maxNumberWaitingEmergency);
    	dataset1.addSeries(avgNumberWaitingRegular);
    	dataset1.addSeries(avgNumberWaitingEmergency);
    	
    	XYLineAndShapeRenderer line = new XYLineAndShapeRenderer();
    	NumberAxis xax = new NumberAxis("run");
    	NumberAxis yax = new NumberAxis("number of patients");
    	xax.setTickUnit(new NumberTickUnit(1));
    	//yax.setTickUnit(new NumberTickUnit(1));
    	XYPlot plot1 = new XYPlot(dataset1,xax,yax, line);
    	JFreeChart chart1 = new JFreeChart(plot1);
    	ApplicationFrame punkteframe1 = new ApplicationFrame("max and average number of waiting patients"); 

		ChartPanel chartPanel1 = new ChartPanel(chart1);
    	punkteframe1.setContentPane(chartPanel1);
    	punkteframe1.pack();
    	punkteframe1.setVisible(true);
    	//**********************************************************************
    	
    	// Diagram2 for mean waiting time both patient groups
    	XYSeriesCollection dataset2 = new XYSeriesCollection();
    	dataset2.addSeries(meanWaitingTimeBoth);
    	    	
    	XYLineAndShapeRenderer line2 = new XYLineAndShapeRenderer();
    	NumberAxis xax2 = new NumberAxis("run");
    	NumberAxis yax2 = new NumberAxis("minutes");
    	xax2.setTickUnit(new NumberTickUnit(1));
    	//yax.setTickUnit(new NumberTickUnit(1));
    	XYPlot plot2 = new XYPlot(dataset2,xax2,yax2, line2);
    	JFreeChart chart2 = new JFreeChart(plot2);
    	ApplicationFrame punkteframe2 = new ApplicationFrame("mean waiting time both patient groups"); 

    	ChartPanel chartPanel2 = new ChartPanel(chart2);
    	punkteframe2.setContentPane(chartPanel2);
    	punkteframe2.pack();
    	punkteframe2.setVisible(true);
    	//**********************************************************************
    	// Diagram3 for 90% quantile (max total time in the emergency for 90% of patients)
    	XYSeriesCollection dataset3 = new XYSeriesCollection();
    	dataset3.addSeries(quantile90Percent);
    	    	
    	XYLineAndShapeRenderer line3 = new XYLineAndShapeRenderer();
    	NumberAxis xax3 = new NumberAxis("run");
    	NumberAxis yax3 = new NumberAxis("minutes");
    	xax3.setTickUnit(new NumberTickUnit(1));
    	//yax.setTickUnit(new NumberTickUnit(1));
    	XYPlot plot3 = new XYPlot(dataset3,xax3,yax3, line3);
    	JFreeChart chart3 = new JFreeChart(plot3);
    	ApplicationFrame punkteframe3 = new ApplicationFrame("90% quantile (max time overall spent in emergency room)"); 

    	ChartPanel chartPanel3 = new ChartPanel(chart3);
    	punkteframe3.setContentPane(chartPanel3);
    	punkteframe3.pack();
    	punkteframe3.setVisible(true);
    	//**********************************************************************
    	// Diagram4 for number of non-waiting, less than 5 minutes waiting and dead patients
    	XYSeriesCollection dataset4 = new XYSeriesCollection();
    	dataset4.addSeries(nonWaiting);
    	dataset4.addSeries(less5Waiting);
    	dataset4.addSeries(deadPatients);
    	dataset4.addSeries(totalPatients);
    	    	
    	XYLineAndShapeRenderer line4 = new XYLineAndShapeRenderer();
    	NumberAxis xax4 = new NumberAxis("run");
    	NumberAxis yax4 = new NumberAxis("number of patients");
    	xax4.setTickUnit(new NumberTickUnit(1));
    	//yax.setTickUnit(new NumberTickUnit(1));
    	XYPlot plot4 = new XYPlot(dataset4,xax4,yax4, line4);
    	JFreeChart chart4 = new JFreeChart(plot4);
    	ApplicationFrame punkteframe4 = new ApplicationFrame("number of non-waiting, less than 5 minutes and dead patients"); 

    	ChartPanel chartPanel4 = new ChartPanel(chart4);
    	punkteframe4.setContentPane(chartPanel4);
    	punkteframe4.pack();
    	punkteframe4.setVisible(true);
    	//**********************************************************************
    	
    	//******************Histogram********************************
    	// Historgram for mean waiting time both patient groups
    	HistogramDataset dataset5 = new HistogramDataset();
    	dataset5.addSeries("Test", meanWaiting, 20, 0, 100);
   	
    	JFreeChart chart5 = ChartFactory.createHistogram(
    			"Histogram for mean waiting time of both patient groups", "mean waiting time in minutes","number of runs",
    			dataset5, PlotOrientation.VERTICAL, true, true, false);
//    	XYPlot plot5 = new XYPlot(dataset5,xax5,yax5, line4);
//    	XYPlot plot5 = (XYPlot) chart5.getPlot();
//    	plot5.setDomainPannable(true);
//        plot5.setRangePannable(true);
//        plot5.setForegroundAlpha(0.85f);
//        NumberAxis yAxis = (NumberAxis) plot5.getRangeAxis();
//        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
//        XYBarRenderer renderer5 = (XYBarRenderer) plot5.getRenderer();
//        renderer5.setDrawBarOutline(false);
//        // flat bars look best...
//        renderer5.setBarPainter(new StandardXYBarPainter());
//        renderer5.setShadowVisible(false);
    	ApplicationFrame punkteframe5 = new ApplicationFrame("histogram mean waiting time both patient groups"); 

    	ChartPanel chartPanel5 = new ChartPanel(chart5);
    	punkteframe5.setContentPane(chartPanel5);
    	punkteframe5.pack();
    	punkteframe5.setVisible(true);
    	//**********************************************************************

	}
	
    public double getPatientArrivalTime() {
    	double time = patientArrivalTime.sample();
    	return time;
    }
    
    public void addToQuantil(Double time) {
    	quantil.add(time);
    }

    public double getTreatmentTime(PatientProcess p, boolean emergency, int treatment) {
    	double time = treatmentTime.sample();
    	ContDistUniform secondTreatment = new ContDistUniform(this, "second treatment time", 5, p.getTreatTime(), false, true);
    	
    	// time for second treatment is shorter
    	if (treatment > 0) {
    		return secondTreatment.sample();
    	}

    	return emergency ? time * emergencyTimeFactor : time;
    }
    
    // 20% of all patients are emergencies
    public boolean isEmergency() {
    	return isEmergency.sample() < shareOfEmergencies;
    }
    
    public static void get90Quantil() {
    	int result = 0;
    	
    	Collections.sort(quantil);  
    	result = (int) (quantil.size() * 0.9);
    	
    	if(result < quantil.size())
    		quantil90 = quantil.get(result);
    	else
    		quantil90 = 0.0;
    }
    
    public static void printStatistics(int run) {
    	get90Quantil();
    	System.out.println();
		
		// max and avg number of waiting patients of both groups
		System.out.println("max. number of waiting regular patients: " + patientQueue.maxLength() + " patients");
		maxNumberWaitingRegular.add(run, patientQueue.maxLength());
		System.out.println("max. number of waiting emergency patients: " + emergencyQueue.maxLength() + " emergency patients");
		maxNumberWaitingEmergency.add(run, emergencyQueue.maxLength());
		System.out.println("avg. number of waiting regular patients: " + patientQueue.averageLength() + " patients");
		avgNumberWaitingRegular.add(run, patientQueue.averageLength());
		System.out.println("avg. number of waiting emergency patients: " + emergencyQueue.averageLength() + " emergency patients");
		avgNumberWaitingEmergency.add(run, emergencyQueue.averageLength());
		
		// mean waiting time of both groups
		System.out.println("mean waiting time of both patient groups: " + tallyWaiting.getMean() + " minutes");
		meanWaitingTimeBoth.add(run, tallyWaiting.getMean());
		meanWaiting[run] = tallyWaiting.getMean();
		
		// number of non-waiting patients and less-than-5-min-waiting patients
		System.out.println("number of non-waiting patients: " + countOfNonWaitingPatients.getValue() + " patients");
		nonWaiting.add(run, countOfNonWaitingPatients.getValue());
		System.out.println("number of patients with waiting time <= 5: " + countOfMaxFiveMinWaitingTime.getValue() + " patients");
		less5Waiting.add(run, countOfMaxFiveMinWaitingTime.getValue());
		
		// 90% quantiles
		System.out.println("90%-Quantile : " + quantil90 + " minutes");
		quantile90Percent.add(run, quantil90);
		
		System.out.println("number of emergency patients who would be dead: " + countOfDeadPatients.getValue() + " patients");
		deadPatients.add(run, countOfDeadPatients.getValue());
		
		System.out.println("number of total patients: " + (patientQueue.getObservations() + emergencyQueue.getObservations()));
		totalPatients.add(run, (patientQueue.getObservations() + emergencyQueue.getObservations()));
		
		System.out.println("max. waiting time of emergency patients: " + tallyWaitingEmergency.getMaximum());
		System.out.println("max. waiting time of regular patients: " + tallyWaitingRegular.getMaximum());
		System.out.println("min. waiting time of emergency patients: " + tallyWaitingEmergency.getMinimum());
		System.out.println("min. waiting time of regular patients: " + tallyWaitingRegular.getMinimum());
		System.out.println("mean waiting time of emergency patients: " + tallyWaitingEmergency.getMean());
		System.out.println("mean waiting time of regular patients: " + tallyWaitingRegular.getMean());
		
		System.out.println();
    }
}
