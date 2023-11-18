package avrmc.core;

import avrmc.core.AbstractMemory.Byte;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javr.core.AVR;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Responsible for managing the model checking process for a given
 * <i>property</i> of the AVR machine. For example, this property might be the
 * maximum stack height observed in any reachable state; or, it could be a
 * signal as to whether any assertion's were violated, etc. The key, however, is
 * that this must properly manage a potentially large number of states.
 *
 * @author David J. Pearce, completed by Jared Scholz
 *
 * @param <T> Concrete property value to be checked.
 */
public class AvrModelChecker<T> {
  /**
   * Property to use for checking concrete value.
   */
  private final Property<T> property;

  /**
   * Construct a new model checker instance to check a given property.
   *
   * @param property The property which this checker will check.
   */
  public AvrModelChecker(Property<T> property) {
    this.property = property;
  }

  /**
   * Apply this model checker to a given starting state and compute the final
   * property. For example, if we're computing the maximum stack height then this
   * would return that value.
   *
   * @param seed Machine state to start checking from.
   * @return Computed property value.
   */
  public T apply(AbstractAvr seed) {
    // Map visited program counter values to corresponding AVR states
    HashMap<Integer, @Nullable Set<AbstractAvr>> visited = new HashMap<>();
    ArrayList<AbstractAvr> worklist = new ArrayList<>();
    // Seed the work list
    worklist.add(seed);
    // Compute initial value for our starting state
    T value = this.property.map(seed);
    while (worklist.size() > 0) {
      // Get next state to process
      AbstractAvr state = pop(worklist);
      AbstractAvr fork = null;
      boolean proceed = true;
      try {
        // Execute state until fork encountered
        while (proceed && fork == null) {
          // Reset I/O port unknown value(s).
          resetIoPort(state);
          // Execute one step the state
          fork = state.clock();
          // Look ahead (as in, before adding to work list) to prevent infinite loops:
          if (fork != null && loopCheck(fork, visited)) {
            fork = null; // Discard fork!
          }
          if (loopCheck(state, visited)) {
            proceed = false; // Discard branch!
          } else {
            // Determine property for updated state
            T nvalue = this.property.map(state);
            // Join with accumulated value
            value = this.property.join(value, nvalue);
          }
        }
        if (proceed) {
          // Add state (and fork if discovered) back on work list
          worklist.add(state);
          if (fork != null) {
            worklist.add(fork);
          }
        }
      } catch (AVR.HaltedException e) {
        assert e != null;
        // Indicates current state has halted. In such case, we don't need to put it
        // back on the work list. However, we do still want to extract its property
        // value
        // (e.g. as this might tell us the exit code, etc).
        T nvalue = this.property.map(state);
        // Join with accumulated value
        value = this.property.join(value, nvalue);
      }
    }
    return value;
  }

  /**
   * Perform a check to guard against continuing on an infinite loop. Note that
   * input states will be used to update the visited map.
   *
   * @param state   The AVR state to check against.
   * @param visited A persistent map of visited program counter values to
   *                corresponding AVR states.
   * @return boolean <code>true</code> if an infinite loop is detected, after
   *         which the given state (branch) should be discarded.
   */
  private static boolean loopCheck(AbstractAvr state,
      HashMap<Integer, @Nullable Set<AbstractAvr>> visited) {
    Integer programCounter = safeIntegerCast(state.getProgramCounter());
    if (visited.containsKey(programCounter)) {
      @Nullable
      Set<AbstractAvr> pastStates = visited.get(programCounter);
      if (Objects.isNull(pastStates)) {
        // Initialize pastStates set upon second visit
        pastStates = new HashSet<>();
        visited.put(programCounter, pastStates);
      } else if (pastStates.contains(state)) {
        // Infinite loop detected!
        return true;
      }
      pastStates.add(state);
    } else {
      // Mark the current instruction as visited once
      visited.put(programCounter, null);
    }
    return false;
  }

  /**
   * Represents a property which is checked over all states encountered during
   * model checking. This could be a simply safety property (e.g. no assertions
   * fail) or some other property of the state (e.g. maximum stack height).
   *
   * @author David J. Pearce
   * @param <T> Value which this property represents.
   *
   */
  public interface Property<T> {
    /**
     * Compute the given property for a single AVR state. For example, the current
     * stack height of the machine.
     *
     * @param state AVR state over which to compute the given property.
     * @return The computed property value
     */
    public T map(AbstractAvr state);

    /**
     * Join two property values together to form a single property value. For
     * example, if we're computing the maximum stack height then we would keep the
     * largest value.
     *
     * @param left  Leftmost value to join.
     * @param right Rightmost value to join.
     * @return Result of join.
     */
    public T join(T left, T right);
  }

  // ===============================================================
  // Helpers
  // ===============================================================

  /**
   * This resets the IO port value to unknown, which may be necessary if the
   * AbstractAVR writes concrete values to the IO port.
   *
   * @param state Abstract AVR state.
   */
  private static void resetIoPort(AbstractAvr state) {
    state.getData().write(32 + 0x16, Byte.UNKNOWN);
  }

  /**
   * Simple helper function for pulling things off the work list efficiently.
   *
   * @param states Work list of states to pop from.
   * @return Abstract state popped from list.
   */
  private static AbstractAvr pop(List<AbstractAvr> states) {
    int last = states.size() - 1;
    @Nullable
    AbstractAvr st = states.get(last);
    states.remove(last);
    return st;
  }

  /**
   * Cast a primitive int to an Integer object in a way that makes Eclipse null
   * annotations happy...
   *
   * @param primitiveInt int to cast to an Integer.
   * @return primitiveInt boxed into an Integer.
   */
  private static Integer safeIntegerCast(int primitiveInt) {
    @Nullable
    Integer intBoxed = Integer.valueOf(primitiveInt);
    assert intBoxed != null;
    return intBoxed;
  }
}
