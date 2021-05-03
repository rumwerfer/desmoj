import desmoj.core.simulator.*;
import java.util.concurrent.TimeUnit;

public class ExampleModel extends Model {
  
  public ExampleModel() {
    super(null, "ExampleModel", true, true);
  }

  public void init() {}

  public void doInitialSchedules() {}

  public String description() { return "ExampleModel"; }

  public static void main(String[] args) {

    ExampleModel model = new ExampleModel();
    Experiment exp = new Experiment("");
    model.connectToExperiment(exp);

    exp.setShowProgressBar(true);
    exp.setShowProgressBarAutoclose(true); // close when finished

    exp.setExecutionSpeedRate(60); // couple simulation time to real time
    // rate = 60 -> 1 min simulation time = 1 sec real time

    TimeInstant stopTime = new TimeInstant(5, TimeUnit.MINUTES);
    exp.tracePeriod(new TimeInstant(0), stopTime);
    exp.stop(stopTime);

    exp.start();
  }
}
