package io.boomerang.util;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import io.boomerang.model.Task;
import io.boomerang.mongo.model.TaskStatus;
import io.boomerang.mongo.service.ActivityTaskService;

/**
 * @author O17006826 This class is used to generate a decimal number that represents the combined
 *         Status to be returned for a certain list of Tasks' Statuses
 */
public class ActivityStatusResolver {
  @Autowired
  private ActivityTaskService taskActivityService;


  public TaskStatus computeStatus(List<Task> taskList) {

    // Each Status will have a bit that corresponds to its index position
    // completed("completed"), 0
    // failure("failure"), 1
    // inProgress("inProgress"), 2
    // notstarted("notstarted"), 3
    // invalid("invalid"), 4
    // skipped("skipped"), 5
    // waiting("waiting"), 6
    // cancelled("cancelled"); 7
    Byte[] statusBitArray = new Byte[TaskStatus.values().length];
    for (Task task : taskList) {
      TaskStatus status = (taskActivityService.findById(task.getTaskId())).getFlowTaskStatus();
      // If we find at least one TaskExec with status inProgress for ex, then set its corresponding
      // bit to 1
      statusBitArray[status.ordinal()] = 1;
    }
    return computeResultingStatus(convertbitArraytoDecimal(statusBitArray));
  }

  private TaskStatus computeResultingStatus(int decimalStatus) {
    TaskStatus resultingTaskStatus = null;
    switch (decimalStatus) {
      case 3: // All Waiting Bit Array: 00000010
      case 18: // Waiting, NotStarted
      case 66:// Waiting, Failure Bit Array: 01000010
      case 82: // Waiting, NotStarted, Failure
      case 130:// Waiting, Complete
      case 146: // Waiting, NotStarted, Complete
      case 194:// Waiting, Complete, Failure
      case 210: // Waiting, Not Started, Complete, Failure
        resultingTaskStatus = TaskStatus.waiting;
        break;
      case 128: // All Complete
        resultingTaskStatus = TaskStatus.completed;
        break;
      case 192: // All Complete OR Failure. At least one Complete and one Failure
        // TODO Is this Complete or Failure?
        resultingTaskStatus = TaskStatus.completed;
        break;
      case 64:
        resultingTaskStatus = TaskStatus.failure;
        break;
      case 16: // All Not Started
        resultingTaskStatus = TaskStatus.notstarted;
        break;
      case 1: // All Cancelled
        resultingTaskStatus = TaskStatus.cancelled;
        break;
      // TODO Need to determine what other Statuses result from the Task Status combinations
      default:
        resultingTaskStatus = TaskStatus.inProgress;
        break;
    }
    return resultingTaskStatus;
  }

  private int convertbitArraytoDecimal(Byte[] statusBitArray) {
    int decimal = -1;
    StringBuilder binaryString = new StringBuilder();
    for (Byte bit : statusBitArray) {
      binaryString.append(bit);
    }
    try {
      decimal = Integer.parseInt(binaryString.toString(), 2);
    } catch (NumberFormatException nex) {
      // ignore
    }
    return decimal;
  }
}
