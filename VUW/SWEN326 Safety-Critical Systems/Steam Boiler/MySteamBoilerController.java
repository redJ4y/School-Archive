package steam.boiler.core;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import steam.boiler.model.SteamBoilerController;
import steam.boiler.util.Mailbox;
import steam.boiler.util.Mailbox.Message;
import steam.boiler.util.Mailbox.MessageKind;
import steam.boiler.util.SteamBoilerCharacteristics;

/**
 * MySteamBoilerController is responsible for controlling the steam boiler
 * hardware. It receives messages and crafts responses on a 5-second cycle.
 *
 * @author Jared Scholz, outline provided by David J. Pearce.
 *
 */
public class MySteamBoilerController implements SteamBoilerController {
  /**
   * Captures the various modes in which the controller can operate.
   *
   * @author David J. Pearce
   *
   */
  private enum State {
    /**
     * Waiting to receive STEAM_BOILER_WAITING.
     */
    WAITING,
    /**
     * Waiting to receive PHYSICAL_UNITS_READY.
     */
    READY,
    /**
     * Maintaining water level with all physical units operational.
     */
    NORMAL,
    /**
     * Maintaining water level with reduced physical units operational.
     */
    DEGRADED,
    /**
     * Maintaining water level with a faulty water level measuring unit.
     */
    RESCUE,
    /**
     * Multiple vital units have failed or water level is critical.
     */
    EMERGENCY_STOP;

    @Override
    public @NonNull String toString() {
      String s = super.toString();
      // Manually override non-null checker
      assert s != null;
      // Now it's NonNull!
      return s;
    }
  }

  /**
   * Records the configuration characteristics for the given boiler problem.
   */
  private final SteamBoilerCharacteristics configuration;
  /**
   * The midpoint water level that should be targeted.
   */
  private final double targetWaterLevel;
  /**
   * Identifies the current mode in which the controller is operating.
   */
  private State mode;
  /**
   * How many times has hardware sent the STOP message in a row.
   */
  private int stopCount;

  /**
   * The expected minimum water level at the start of clock.
   */
  private double expectedMinWaterLevel;
  /**
   * The expected maximum water level at the start of clock.
   */
  private double expectedMaxWaterLevel;
  /**
   * The expected minimum steam output rate at the start of clock.
   */
  private double expectedMinSteamOutput;
  /**
   * At the start of the clock: The pumps that should have been activated. At the
   * end of the clock: The pumps that are to be activated.
   */
  private final boolean[] pumpActivations;

  /**
   * Pumps that are identified as non-functional.
   */
  private final boolean[] pumpFailures;
  /**
   * Pump controllers that are identified as non-functional.
   */
  private final boolean[] controllerFailures;
  /**
   * Whether or not a steam sensor failure has been detected.
   */
  private boolean steamSensorFailure;
  /**
   * Whether or not an unexpected level reading was received.
   */
  private boolean unexpectedLevel;
  /**
   * Whether or not a level failure has been detected.
   */
  private boolean levelFailure;
  /**
   * Whether or not a reduced capacity pump is suspected.
   */
  private boolean reducedPumpSuspected;
  /**
   * Whether the <code>unexpectedLevel</code> is consistent or a one-off
   * inconsistency. After the first unexpected value, this will be set to
   * <code>true</code>, and any subsequent unexpected values may be processed
   * differently. Required by <code>watchLevelReading()</code>.
   */
  private boolean unexpectedLevelConsistent;
  /**
   * The previous value reported by the water level sensor. -1 indicates there is
   * no previous value. Required by <code>watchLevelReading()</code>.
   */
  private double previousLevelReading;

  /**
   * Construct a steam boiler controller for a given set of characteristics.
   *
   * @param configuration The boiler characteristics to be used.
   */
  public MySteamBoilerController(SteamBoilerCharacteristics configuration) {
    this.configuration = configuration;
    this.targetWaterLevel = (this.configuration.getMinimalNormalLevel()
        + this.configuration.getMaximalNormalLevel()) / 2.0;
    this.mode = State.WAITING;
    this.stopCount = 0;
    // Initialize expectation variables
    this.expectedMaxWaterLevel = 0.0;
    this.expectedMaxWaterLevel = 0.0;
    this.expectedMinSteamOutput = 0.0;
    this.pumpActivations = new boolean[this.configuration.getNumberOfPumps()];
    // Initialize failure tracking variables
    this.pumpFailures = new boolean[this.configuration.getNumberOfPumps()];
    this.controllerFailures = new boolean[this.configuration.getNumberOfPumps()];
    this.steamSensorFailure = false;
    this.unexpectedLevel = false;
    this.levelFailure = false;
    this.reducedPumpSuspected = false;
    this.unexpectedLevelConsistent = false;
    this.previousLevelReading = -1.0;
  }

  /**
   * This message is displayed in the simulation window, and enables a limited
   * form of debug output. The content of the message has no material effect on
   * the system, and can be whatever is desired. In principle, however, it should
   * display a useful message indicating the current state of the controller.
   *
   * @return Status message of steam boiler controller.
   */
  @Override
  public String getStatusMessage() {
    return this.mode.toString();
  }

  /**
   * Process a clock signal which occurs every 5 seconds. This requires reading
   * the set of incoming messages from the physical units and producing a set of
   * output messages which are sent back to them.
   *
   * @param incoming The set of incoming messages from the physical units.
   * @param outgoing Messages generated during the execution of this method should
   *                 be written here.
   */
  @Override
  public void clock(@NonNull Mailbox incoming, @NonNull Mailbox outgoing) {
    // Extract required messages
    Message levelMessage = extractOnlyMatch(MessageKind.LEVEL_v, incoming);
    Message steamMessage = extractOnlyMatch(MessageKind.STEAM_v, incoming);
    // Listen for repairs before running a system check
    listenForRepairs(incoming, outgoing, levelMessage, steamMessage);
    // Set the program mode through a system check
    runSystemCheck(levelMessage, steamMessage, incoming, outgoing);
    // Perform functionality based on the program mode:
    if (this.mode == State.EMERGENCY_STOP) {
      // Note: levelMessage and/or steamMessage could be null in this block!
      emergencyStop(outgoing);
    } else if (this.mode == State.WAITING || this.mode == State.READY) {
      // Modes WAITING and READY are both handled by doInitialization()...
      assert levelMessage != null;
      assert steamMessage != null;
      doInitialization(levelMessage.getDoubleParameter(), steamMessage.getDoubleParameter(),
          incoming, outgoing);
    } else {
      assert levelMessage != null;
      double waterLevelToUse = levelMessage.getDoubleParameter();
      this.previousLevelReading = waterLevelToUse;
      // Estimate the current water level if necessary!
      if (this.levelFailure) {
        // Weight expected max higher, as it directly depends on steam output.
        waterLevelToUse = ((3 * this.expectedMaxWaterLevel) + this.expectedMinWaterLevel) / 4.0;
        // ...If the sensor fails while steam output is maximum, all is well.
      }
      assert steamMessage != null;
      double steamOutputToUse = steamMessage.getDoubleParameter();
      // Estimate the current steam output if necessary!
      if (this.steamSensorFailure) {
        // Add a reasonable (linear) value to current level until max is reached...
        steamOutputToUse = Math.min(
            this.expectedMinSteamOutput + (this.configuration.getMaximualSteamRate() / 20.0),
            this.configuration.getMaximualSteamRate());
        // ...If the sensor fails at maximum output, all is well.
      }
      if (activatePumps(waterLevelToUse, steamOutputToUse, outgoing)) {
        switch (this.mode) {
          case NORMAL:
            outgoing.send(new Message(MessageKind.MODE_m, Mailbox.Mode.NORMAL));
            break;
          case DEGRADED:
            outgoing.send(new Message(MessageKind.MODE_m, Mailbox.Mode.DEGRADED));
            break;
          case RESCUE:
            outgoing.send(new Message(MessageKind.MODE_m, Mailbox.Mode.RESCUE));
            break;
          default:
            emergencyStop(outgoing); // Unrecognized State
            break;
        }
      } else {
        emergencyStop(outgoing);
      }
    }
  }

  /**
   * Perform initialization routine/handshake/negotiation with hardware. Enter
   * normal state after PHYSICAL_UNITS_READY is received from hardware.
   *
   * @param currWaterLevel  The presumed current water level.
   * @param currSteamOutput The presumed current steam output rate.
   * @param incoming        The incoming Mailbox from hardware.
   * @param outgoing        The outgoing Mailbox to send messages in.
   */
  private void doInitialization(double currWaterLevel, double currSteamOutput, Mailbox incoming,
      Mailbox outgoing) {
    // WAITING: Wait for STEAM_BOILER_WAITING message from hardware
    if (this.mode == State.WAITING
        && extractOnlyMatch(MessageKind.STEAM_BOILER_WAITING, incoming) != null) {
      // Switch to and perform ready mode functionality in the same clock cycle!
      this.mode = State.READY;
    }
    // READY: Validate, fill tank, then wait for PHYSICAL_UNITS_READY from hardware
    if (this.mode == State.READY) {
      // Perform initial state validation
      if (currSteamOutput != 0.0) {
        // Likely defective steam sensor!
        outgoing.send(new Message(MessageKind.STEAM_FAILURE_DETECTION));
        emergencyStop(outgoing);
        return;
      } else if (currWaterLevel > this.configuration.getMaximalNormalLevel()) {
        // Start from an acceptable water level!
        outgoing.send(new Message(MessageKind.VALVE));
      } else {
        if (!activatePumps(currWaterLevel, currSteamOutput, outgoing)
            && currWaterLevel > this.configuration.getMinimalLimitLevel()) {
          // Ensure water level safety only after the minimal level is reached.
          emergencyStop(outgoing);
          return;
        }
        // Fill until water level is within normal range:
        if (currWaterLevel > this.configuration.getMinimalNormalLevel()) {
          if (extractOnlyMatch(MessageKind.PHYSICAL_UNITS_READY, incoming) != null) {
            this.mode = State.NORMAL;
            // Notify hardware of mode change in the same clock cycle!
            outgoing.send(new Message(MessageKind.MODE_m, Mailbox.Mode.NORMAL));
            return;
          }
          outgoing.send(new Message(MessageKind.PROGRAM_READY));
        }
      }
    }
    outgoing.send(new Message(MessageKind.MODE_m, Mailbox.Mode.INITIALISATION));
  }

  /**
   * Perform the emergency stop routine. This can be called from anywhere, thus
   * also sets the program mode accordingly.
   *
   * @param outgoing The outgoing Mailbox to send messages in.
   */
  private void emergencyStop(Mailbox outgoing) {
    this.mode = State.EMERGENCY_STOP;
    outgoing.clearAll();
    outgoing.send(new Message(MessageKind.VALVE));
    for (int i = 0; i < this.configuration.getNumberOfPumps(); i++) {
      outgoing.send(new Message(MessageKind.CLOSE_PUMP_n, i));
    }
    outgoing.send(new Message(MessageKind.MODE_m, Mailbox.Mode.EMERGENCY_STOP));
  }

  /**
   * Open or close pumps to maintain water level near target, only sending
   * messages to hardware if necessary. Account for pump or controller failures.
   * Update expectations for the next cycle accordingly.
   *
   * @param currWaterLevel  The presumed current water level.
   * @param currSteamOutput The presumed current steam output rate.
   * @param outgoing        The outgoing Mailbox to send messages in.
   * @return boolean of whether or not the expected results are safe (as in,
   *         whether or not they respect hardware requirements). If
   *         <code>false</code> is returned, appropriate action should be taken
   *         (i.e. emergency stop).
   */
  private boolean activatePumps(double currWaterLevel, double currSteamOutput, Mailbox outgoing) {
    // Approximate the amount of water lost through the next cycle
    double waterLossApproximation = 5
        * ((currSteamOutput + this.configuration.getMaximualSteamRate()) / 2.0);
    // Set our target to account for approximated water loss
    double adjustedTarget = this.targetWaterLevel + waterLossApproximation;
    // Check distance from target if we were to turn on each pump...
    double predictedLevel = currWaterLevel;
    double bestDistance = Math.abs(adjustedTarget - predictedLevel);
    for (int i = 0; i < this.configuration.getNumberOfPumps(); i++) {
      double levelWithPump = predictedLevel + (5 * this.configuration.getPumpCapacity(i));
      double distanceWithPump = Math.abs(adjustedTarget - levelWithPump);
      if (this.pumpFailures[i]) {
        // Pump failure: assume pump is operating opposite to expectation.
        if (!this.pumpActivations[i]) {
          predictedLevel = levelWithPump;
          bestDistance = distanceWithPump;
        }
      } else if (this.controllerFailures[i]) {
        // Controller failure: assume pump is stuck at previous position.
        if (this.pumpActivations[i]) {
          predictedLevel = levelWithPump;
          bestDistance = distanceWithPump;
        }
      } else {
        if (distanceWithPump < bestDistance) {
          // Turning on this pump would get us closer to target, activate it.
          if (this.pumpActivations[i] != true) {
            this.pumpActivations[i] = true;
            // Send the message to hardware if necessary
            outgoing.send(new Message(MessageKind.OPEN_PUMP_n, i));
          }
          predictedLevel = levelWithPump;
          bestDistance = distanceWithPump;
        } else {
          if (this.pumpActivations[i] != false) {
            this.pumpActivations[i] = false;
            // Send the message to hardware if necessary
            outgoing.send(new Message(MessageKind.CLOSE_PUMP_n, i));
          }
        }
      }
    }
    // Update our expectations for the next cycle
    this.expectedMinSteamOutput = currSteamOutput;
    this.expectedMinWaterLevel = predictedLevel - (5 * this.configuration.getMaximualSteamRate());
    this.expectedMaxWaterLevel = predictedLevel - (5 * currSteamOutput);
    // Validate the safety of our expectations...
    if (this.expectedMaxWaterLevel > this.configuration.getMaximalLimitLevel()
        || this.expectedMaxWaterLevel < this.configuration.getMinimalLimitLevel()
        || currWaterLevel > this.configuration.getMaximalLimitLevel()
        || currWaterLevel < this.configuration.getMinimalLimitLevel()) {
      return false;
    }
    return true;
  }

  /**
   * Inspect the incoming Mailbox for any repair notifications. An acknowledgement
   * is returned through the outgoing Mailbox only if the repair was valid.
   * Expected values are updated such that a repair is always trusted for at least
   * the current clock cycle.
   *
   * @param incoming     The incoming Mailbox from hardware.
   * @param outgoing     The outgoing Mailbox to send messages in.
   * @param levelMessage Extracted LEVEL_v message.
   * @param steamMessage Extracted STEAM_v message.
   */
  private void listenForRepairs(Mailbox incoming, Mailbox outgoing, @Nullable Message levelMessage,
      @Nullable Message steamMessage) {
    if (this.levelFailure && extractOnlyMatch(MessageKind.LEVEL_REPAIRED, incoming) != null) {
      this.unexpectedLevel = false;
      this.levelFailure = false;
      this.previousLevelReading = -1.0; // Invalidate previous reading
      // Update expectations to trust this repair in the current clock cycle
      if (levelMessage != null) {
        this.expectedMaxWaterLevel = levelMessage.getDoubleParameter() + 1;
        this.expectedMinWaterLevel = levelMessage.getDoubleParameter() - 1;
      }
      outgoing.send(new Message(MessageKind.LEVEL_REPAIRED_ACKNOWLEDGEMENT));
    }
    if (this.steamSensorFailure && extractOnlyMatch(MessageKind.STEAM_REPAIRED, incoming) != null) {
      this.steamSensorFailure = false;
      // Update expectations to trust this repair in the current clock cycle
      if (steamMessage != null) {
        this.expectedMinSteamOutput = steamMessage.getDoubleParameter();
      }
      outgoing.send(new Message(MessageKind.STEAM_REPAIRED_ACKNOWLEDGEMENT));
    }
    Message[] pumpRepairs = extractAllMatches(MessageKind.PUMP_REPAIRED_n, incoming);
    for (int i = 0; i < pumpRepairs.length; i++) {
      int pumpIndex = pumpRepairs[i].getIntegerParameter();
      // Validate the index before using it
      if (pumpIndex >= 0 && pumpIndex < this.configuration.getNumberOfPumps()
          && this.pumpFailures[pumpIndex]) {
        this.pumpFailures[pumpIndex] = false;
        this.previousLevelReading = -1.0; // Invalidate previous level reading
        // Update expectations to trust this repair in the current clock cycle
        boolean[] pumpStates = sort_n_b(extractAllMatches(MessageKind.PUMP_STATE_n_b, incoming));
        // Ensure valid transmission (transmission failure will be discovered later)...
        if (pumpStates.length == this.configuration.getNumberOfPumps()) {
          this.pumpActivations[pumpIndex] = pumpStates[pumpIndex];
        }
        outgoing.send(new Message(MessageKind.PUMP_REPAIRED_ACKNOWLEDGEMENT_n, pumpIndex));
      }
    }
    Message[] controllerRepairs = extractAllMatches(MessageKind.PUMP_CONTROL_REPAIRED_n, incoming);
    for (int i = 0; i < controllerRepairs.length; i++) {
      int controllerIndex = controllerRepairs[i].getIntegerParameter();
      // Validate the index before using it
      if (controllerIndex >= 0 && controllerIndex < this.configuration.getNumberOfPumps()
          && this.controllerFailures[controllerIndex]) {
        this.controllerFailures[controllerIndex] = false;
        outgoing.send(
            new Message(MessageKind.PUMP_CONTROL_REPAIRED_ACKNOWLEDGEMENT_n, controllerIndex));
      }
    }
  }

  /**
   * Sets the program mode by running a failure check and using the result
   * appropriately (as in, it respects existing failures). If a failure is found,
   * the detection message is also placed into the outgoing Mailbox here.
   *
   * @param levelMessage Extracted LEVEL_v message.
   * @param steamMessage Extracted STEAM_v message.
   * @param incoming     The incoming Mailbox from hardware.
   * @param outgoing     The outgoing Mailbox to send messages in.
   */
  private void runSystemCheck(@Nullable Message levelMessage, @Nullable Message steamMessage,
      Mailbox incoming, Mailbox outgoing) {
    if (this.mode != State.EMERGENCY_STOP) { // Respect existing emergency stop!
      // When STOP is received three times in a row, go into emergency stop.
      if (extractAllMatches(MessageKind.STOP, incoming).length > 0) {
        this.stopCount++;
        if (this.stopCount >= 3) {
          this.mode = State.EMERGENCY_STOP;
          return;
        }
      } else {
        this.stopCount = 0;
      }
      Message failureMessage = runFailureCheck(levelMessage, steamMessage, incoming);
      if (failureMessage != null) {
        switch (failureMessage.getKind()) {
          case LEVEL_FAILURE_DETECTION:
            this.unexpectedLevel = true;
            // Do not immediately assume an unexpected level is a level failure!
            break;
          case STEAM_FAILURE_DETECTION:
            this.steamSensorFailure = true;
            outgoing.send(failureMessage);
            break;
          case PUMP_CONTROL_FAILURE_DETECTION_n:
            this.controllerFailures[failureMessage.getIntegerParameter()] = true;
            outgoing.send(failureMessage);
            break;
          case PUMP_FAILURE_DETECTION_n:
            this.pumpFailures[failureMessage.getIntegerParameter()] = true;
            outgoing.send(failureMessage);
            break;
          default:
            // A transmission failure is suspected!
            this.mode = State.EMERGENCY_STOP;
            return;
        }
      }
      if (this.mode != State.EMERGENCY_STOP && this.unexpectedLevel) {
        assert levelMessage != null;
        // Further investigate the level failure detection over time...
        watchLevelReading(levelMessage.getDoubleParameter(), outgoing);
      }
      // Finally, set the program mode using new failure knowledge
      updateMode();
    }
  }

  /**
   * Watch the reported water level to protect against valve or pump capacity
   * (reduced output) failures. If a level failure is detected,
   * <code>levelFailure</code> will be set to <code>true</code> and the failure
   * message will be sent in the outgoing Mailbox. Note: Must be called before the
   * <code>pumpActivations</code> array is updated (requires values from the
   * previous clock cycle). Also requires that <code>previousLevelReading</code>
   * be kept updated.
   *
   * @param levelReading The actual level reading given by the sensor this clock.
   * @param outgoing     The outgoing Mailbox to send messages in.
   */
  private void watchLevelReading(double levelReading, Mailbox outgoing) {
    // Both previousLevelReading and pumpActivations are from the previous clock...
    if (!this.levelFailure && this.previousLevelReading >= 0) {
      double pumpsContribution = 0.0;
      double maxPumpContribution = 0.0;
      for (int i = 0; i < this.configuration.getNumberOfPumps(); i++) {
        if (this.pumpActivations[i]) {
          double pumpContribution = 5 * this.configuration.getPumpCapacity(i);
          pumpsContribution += pumpContribution;
          if (pumpContribution > maxPumpContribution) {
            maxPumpContribution = pumpContribution;
          }
        }
      }
      double expectedMax = this.previousLevelReading + pumpsContribution
          - (5 * this.expectedMinSteamOutput);
      double expectedMin = this.previousLevelReading + pumpsContribution
          - (5 * this.configuration.getMaximualSteamRate());
      double absoluteMin = expectedMin
          - Math.max((5 * this.configuration.getEvacuationRate()) + 0.01, maxPumpContribution);
      double levelDelta = Math.abs(expectedMin - levelReading);
      if (levelReading < expectedMax && levelReading > absoluteMin) {
        // Do nothing if expectedMin <= levelReading < expectedMax, wait for stability.
        if (levelReading < expectedMin) {
          if (this.unexpectedLevelConsistent) {
            // Level reading could still be valid, compare delta to a valve failure:
            if (Math.abs(levelDelta - (5 * this.configuration.getEvacuationRate())) < 0.01) {
              // Valve failure suspected!
              this.mode = State.EMERGENCY_STOP;
            } else if (levelDelta < maxPumpContribution) {
              // If it is not a valve failure, it is likely a reduced capacity pump!
              this.reducedPumpSuspected = true;
            } else {
              this.reducedPumpSuspected = false;
            }
          }
          this.unexpectedLevelConsistent = true;
        }
      } else {
        if (!this.reducedPumpSuspected) {
          // Obvious level failure!
          this.levelFailure = true;
          outgoing.send(new Message(MessageKind.LEVEL_FAILURE_DETECTION));
        } else {
          if (levelDelta < 0.01) { // Reduced pump suspected but level returned to normal
            // It is likely that a reduced capacity pump was fixed!
            this.unexpectedLevel = false;
          }
          // Only provide one chance for a reduced pump fix to be considered!
          this.reducedPumpSuspected = false;
        }
        this.unexpectedLevelConsistent = false;
      }
    }
  }

  /**
   * Update the program mode based on known failures according to the
   * specification.
   */
  private void updateMode() {
    if (this.mode != State.EMERGENCY_STOP) { // Respect existing emergency stop!
      // Guard against overriding specialized modes unnecessarily:
      if (this.mode == State.WAITING || this.mode == State.READY) {
        if (this.unexpectedLevel) {
          this.mode = State.EMERGENCY_STOP;
        }
        return;
      }
      boolean pumpFailure = false;
      boolean controllerFailure = false;
      for (int i = 0; i < this.configuration.getNumberOfPumps()
          && !(pumpFailure && controllerFailure); i++) {
        if (this.pumpFailures[i]) {
          pumpFailure = true;
        }
        if (this.controllerFailures[i]) {
          controllerFailure = true;
        }
      }
      if (this.levelFailure) {
        if (this.steamSensorFailure || controllerFailure) {
          // Water approximation can no longer be trusted...
          this.mode = State.EMERGENCY_STOP;
        } else {
          this.mode = State.RESCUE;
        }
      } else if (this.steamSensorFailure || controllerFailure || pumpFailure
          || this.unexpectedLevel) {
        this.mode = State.DEGRADED;
      } else {
        this.mode = State.NORMAL;
      }
    }
  }

  /**
   * Check for hardware failures by validating and comparing the values we
   * actually received with the values we expected to receive. Assumes that only
   * one failure can occur per clock.
   *
   * @param levelMessage Extracted LEVEL_v message.
   * @param steamMessage Extracted STEAM_v message.
   * @param incoming     The incoming Mailbox from hardware.
   * @return Message where the MessageKind corresponds to a failed hardware unit
   *         (LEVEL_FAILURE_DETECTION, STEAM_FAILURE_DETECTION,
   *         PUMP_CONTROL_FAILURE_DETECTION_n, or PUMP_FAILURE_DETECTION_n) or
   *         STOP if a transmission failure is suspected, or <code>null</code> if
   *         no failure was yet detected or a multiple component failure is
   *         suspected.
   */
  private @Nullable Message runFailureCheck(@Nullable Message levelMessage,
      @Nullable Message steamMessage, Mailbox incoming) {
    boolean[] currPumpStates = sort_n_b(extractAllMatches(MessageKind.PUMP_STATE_n_b, incoming));
    boolean[] currPumpControlStates = sort_n_b(
        extractAllMatches(MessageKind.PUMP_CONTROL_STATE_n_b, incoming));
    // Always check for transmission failures
    if (transmissionFailure(levelMessage, steamMessage, currPumpStates, currPumpControlStates,
        incoming)) {
      return new Message(MessageKind.STOP); // Arbitrary MessageKind to trigger emergency stop
    }
    assert steamMessage != null;
    assert levelMessage != null;
    // Check the steam sensor reading for obvious failure
    if (!this.steamSensorFailure && (steamMessage.getDoubleParameter() < this.expectedMinSteamOutput
        || steamMessage.getDoubleParameter() < 0
        || steamMessage.getDoubleParameter() > this.configuration.getMaximualSteamRate())) {
      // Steam sensor failure! (Boiler failure not accounted for)
      return new Message(MessageKind.STEAM_FAILURE_DETECTION);
    }
    // Check the water level sensor reading for obvious failure
    if (!this.unexpectedLevel && (levelMessage.getDoubleParameter() < 0
        || levelMessage.getDoubleParameter() > this.configuration.getCapacity())) {
      return new Message(MessageKind.LEVEL_FAILURE_DETECTION);
    }
    // Guard against using inaccurate predictions during initialization...
    if (this.mode != State.WAITING && this.mode != State.READY) {
      // Compare current water reading with expectation
      int waterComparison = 0; // 0 = within expected range
      double currWaterLevel = levelMessage.getDoubleParameter();
      if (currWaterLevel < this.expectedMinWaterLevel - 0.1) {
        waterComparison = -1;
      } else if (currWaterLevel > this.expectedMaxWaterLevel + 0.1) {
        waterComparison = 1;
      }
      for (int i = 0; i < this.configuration.getNumberOfPumps(); i++) {
        if (!this.pumpFailures[i] && !this.controllerFailures[i]) {
          // Compare current pump state with expectation
          int pumpStateComparison = 0; // 0 = expected result
          if (currPumpStates[i] != this.pumpActivations[i]) {
            pumpStateComparison = this.pumpActivations[i] ? -1 : 1;
          }
          // Compare current controller state with expectation
          int controllerStateComparison = 0; // 0 = expected result
          if (currPumpControlStates[i] != this.pumpActivations[i]) {
            controllerStateComparison = this.pumpActivations[i] ? -1 : 1;
          }
          // Perform failure analysis on each pump if necessary
          if (waterComparison != 0 || pumpStateComparison != 0 || controllerStateComparison != 0) {
            // Something has deviated from expectations, conduct failure analysis
            MessageKind failureMessage = conductFailureAnalysis(i, waterComparison,
                pumpStateComparison, controllerStateComparison);
            // Check every pump until something is found (or we are done)
            if (failureMessage != null) {
              if (failureMessage == MessageKind.PUMP_FAILURE_DETECTION_n
                  || failureMessage == MessageKind.PUMP_CONTROL_FAILURE_DETECTION_n) {
                // Attach the pump index to pump-related failures
                return new Message(failureMessage, i);
              }
              return new Message(failureMessage);
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Determine which physical hardware unit likely caused the discrepancy.
   *
   * @param pumpIndex                 The index of the pump currently in analysis.
   *
   * @param waterComparison           The comparison between water and expected
   *                                  water. 0 indicates expected, -1 indicates
   *                                  less than expected, and 1 indicates more
   *                                  than expected.
   * @param pumpStateComparison       The comparison between the pump state and
   *                                  the expected pump state. -1 indicates off
   *                                  instead of on and 1 indicates on instead of
   *                                  off.
   * @param controllerStateComparison The comparison between the controller state
   *                                  and the expected controller state. -1
   *                                  indicates off instead of on and 1 indicates
   *                                  on instead of off.
   * @return MessageKind corresponding to a failed hardware unit
   *         (LEVEL_FAILURE_DETECTION, PUMP_CONTROL_FAILURE_DETECTION_n, or
   *         PUMP_FAILURE_DETECTION_n), or <code>null</code> if no failure was yet
   *         detected or a multiple component failure is suspected.
   */
  private @Nullable MessageKind conductFailureAnalysis(int pumpIndex, int waterComparison,
      int pumpStateComparison, int controllerStateComparison) {
    // Stored booleans reduce the amount of comparisons (false = unexpected)
    boolean water = waterComparison == 0; // Was water level expected?
    boolean pump = pumpStateComparison == 0; // Was pump state expected?
    boolean controller = controllerStateComparison == 0; // Was controller state expected?
    if (pump && controller && !water) {
      // Water level sensor failure!
      if (!this.unexpectedLevel) {
        return MessageKind.LEVEL_FAILURE_DETECTION;
      }
    } else if (pump && !controller && water) {
      // Likely a controller failure, but could be a pump failure...
      if (this.expectedMaxWaterLevel - this.expectedMinWaterLevel < this.configuration
          .getPumpCapacity(pumpIndex)) {
        // Water level range < pump capacity, it could not have been a pump failure.
        return MessageKind.PUMP_CONTROL_FAILURE_DETECTION_n;
      } // ...else we can not be certain yet!
    } else if (pump && !controller && !water) {
      if (controllerStateComparison == waterComparison) {
        // Pump failure!
        return MessageKind.PUMP_FAILURE_DETECTION_n;
      } // ...else a multiple component failure suspected.
    } else if (!pump && controller && water) {
      // Pump is likely working but not responding correctly...
      return MessageKind.PUMP_FAILURE_DETECTION_n;
    } else if (!pump && controller && !water) {
      if (pumpStateComparison == waterComparison) {
        // Pump failure!
        return MessageKind.PUMP_FAILURE_DETECTION_n;
      } // ...else a multiple component failure suspected.
    } else if (!pump && !controller && water) {
      // Pump failure!
      return MessageKind.PUMP_FAILURE_DETECTION_n;
    } else if (!pump && !controller && !water) {
      if (pumpStateComparison == controllerStateComparison
          && pumpStateComparison == waterComparison) {
        // Pump failure!
        return MessageKind.PUMP_FAILURE_DETECTION_n;
      } // ...else a multiple component failure suspected.
    }
    return null;
  }

  /**
   * Check whether there was a transmission failure. This is indicated in several
   * ways. Firstly, when one of the required messages is missing. Secondly, when
   * the values returned in the messages are nonsensical.
   *
   * @param levelMessage      Extracted LEVEL_v message.
   * @param steamMessage      Extracted STEAM_v message.
   * @param pumpStates        Extracted PUMP_STATE_n_b messages.
   * @param pumpControlStates Extracted PUMP_CONTROL_STATE_n_b messages.
   * @param incoming          The incoming Mailbox from hardware.
   * @return True if a transmission failure was detected.
   */
  private boolean transmissionFailure(@Nullable Message levelMessage,
      @Nullable Message steamMessage, boolean[] pumpStates, boolean[] pumpControlStates,
      Mailbox incoming) {
    // Inspect incoming Mailbox for any missing or nonsensical values:
    if (levelMessage == null) {
      return true; // Nonsense or missing level reading
    }
    if (steamMessage == null) {
      return true; // Nonsense or missing steam reading
    }
    if (pumpStates.length != this.configuration.getNumberOfPumps()) {
      return true; // Nonsense pump state readings
    }
    if (pumpControlStates.length != this.configuration.getNumberOfPumps()) {
      return true; // Nonsense pump control state readings
    }
    // Inspect incoming Mailbox for any expected failure acknowledgments:
    if (this.levelFailure != (extractOnlyMatch(MessageKind.LEVEL_FAILURE_ACKNOWLEDGEMENT,
        incoming) != null)) {
      return true; // Missing or unexpected water sensor failure acknowledgement!
    }
    if (this.steamSensorFailure != (extractOnlyMatch(
        MessageKind.STEAM_OUTCOME_FAILURE_ACKNOWLEDGEMENT, incoming) != null)) {
      return true; // Missing or unexpected steam sensor failure acknowledgement!
    }
    boolean[] pumpFailAcknowledgements = place_n(
        extractAllMatches(MessageKind.PUMP_FAILURE_ACKNOWLEDGEMENT_n, incoming),
        this.configuration.getNumberOfPumps());
    boolean[] controllerFailAcknowledgements = place_n(
        extractAllMatches(MessageKind.PUMP_CONTROL_FAILURE_ACKNOWLEDGEMENT_n, incoming),
        this.configuration.getNumberOfPumps());
    for (int i = 0; i < this.configuration.getNumberOfPumps(); i++) {
      if (this.pumpFailures[i] != pumpFailAcknowledgements[i]
          || this.controllerFailures[i] != controllerFailAcknowledgements[i]) {
        return true; // Missing or unexpected a pump/controller failure acknowledgement!
      }
    }
    return false;
  }

  /**
   * Place an array of Messages of type n (those with an integer parameter),
   * specifically PUMP_FAILURE_ACKNOWLEDGEMENT_n and
   * PUMP_CONTROL_FAILURE_ACKNOWLEDGEMENT_n messages, into another array based on
   * their <code>n</code> values. This means each index represents whether or not
   * a message with that <code>n</code> value exists in the input array.
   *
   * @param messages An array of Messages of type n (those with an integer
   *                 parameter), specifically PUMP_FAILURE_ACKNOWLEDGEMENT_n and
   *                 PUMP_CONTROL_FAILURE_ACKNOWLEDGEMENT_n messages.
   * @param size     The size of the array to return. This should probably include
   *                 indexes for every expected <code>n</code> value.
   * @return A boolean array of size <code>size</code> where each index represents
   *         whether or not a message with that <code>n</code> value exists in the
   *         input array.
   */
  private static boolean[] place_n(Message[] messages, int size) {
    boolean[] sorted = new boolean[size]; // Heap memory!
    for (int i = 0; i < messages.length; i++) {
      int n = messages[i].getIntegerParameter();
      if (n >= 0 && n < size) {
        sorted[n] = true;
      }
    }
    return sorted;
  }

  /**
   * Sort an array of Messages of type n_b (those with integer and boolean
   * parameters), specifically PUMP_STATE_n_b and PUMP_CONTROL_STATE_n_b messages.
   * This means each <code>b</code> value is placed at its index <code>n</code>.
   * In the context of pump/control state messages, this means the pump/controller
   * identifier becomes the index.
   *
   * @param messages An array of Messages of type n_b (those with integer and
   *                 boolean parameters), specifically PUMP_STATE_n_b and
   *                 PUMP_CONTROL_STATE_n_b messages.
   * @return A boolean array where each <code>b</code> value is placed at index
   *         <code>n</code>. Note: if an issue is encountered, the returned array
   *         is incomplete (the program mode will be set to EMERGENCY_STOP).
   */
  private boolean[] sort_n_b(Message[] messages) {
    boolean[] sorted = new boolean[messages.length]; // Heap memory!
    for (int i = 0; i < messages.length; i++) {
      int n = messages[i].getIntegerParameter();
      if (n < 0 || n >= messages.length) {
        // Invalid n value: transmission failure!
        this.mode = State.EMERGENCY_STOP;
        return sorted; // Stop here!
      }
      sorted[n] = messages[i].getBooleanParameter();
    }
    return sorted;
  }

  /**
   * Find and extract a message of a given kind in a mailbox. This must the only
   * match in the mailbox, else <code>null</code> is returned.
   *
   * @param kind     The kind of message to look for.
   * @param incoming The mailbox to search through.
   * @return The matching message, or <code>null</code> if there was not exactly
   *         one match.
   */
  private static @Nullable Message extractOnlyMatch(MessageKind kind, Mailbox incoming) {
    Message match = null;
    for (int i = 0; i != incoming.size(); ++i) {
      Message ith = incoming.read(i);
      if (ith.getKind() == kind) {
        if (match == null) {
          match = ith;
        } else {
          // This indicates that we matched more than one message of the given kind.
          return null;
        }
      }
    }
    return match;
  }

  /**
   * Find and extract all messages of a given kind.
   *
   * @param kind     The kind of message to look for.
   * @param incoming The mailbox to search through.
   * @return The array of matches, which can empty if there were none.
   */
  private static Message[] extractAllMatches(MessageKind kind, Mailbox incoming) {
    int count = 0;
    // Count the number of matches
    for (int i = 0; i != incoming.size(); ++i) {
      Message ith = incoming.read(i);
      if (ith.getKind() == kind) {
        count = count + 1;
      }
    }
    // Now, construct resulting array
    Message[] matches = new Message[count];
    int index = 0;
    for (int i = 0; i != incoming.size(); ++i) {
      Message ith = incoming.read(i);
      if (ith.getKind() == kind) {
        matches[index++] = ith;
      }
    }
    return matches;
  }
}
