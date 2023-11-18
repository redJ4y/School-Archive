package tinyboycov.core;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.eclipse.jdt.annotation.Nullable;
import tinyboy.core.ControlPad;
import tinyboy.core.TinyBoyInputSequence;
import tinyboy.util.AutomatedTester;

/**
 * The TinyBoy Input Generator is responsible for generating and refining inputs
 * to try and ensure that sufficient branch coverage is obtained. This
 * implementation is designed to be fully concurrent. Instead of processing
 * batches one at a time, a continuous stream of sequences should be available
 * to the fuzzer (assuming record is called frequently and concurrently).
 *
 * @author Jared Scholz
 *
 */
public class TinyBoyInputGenerator implements AutomatedTester.InputGenerator<TinyBoyInputSequence> {

  /**
   * A ScoredSequence wraps a TinyBoyInputSequence, providing a score that can be
   * used for ScoredSequence comparison via the Comparable interface.
   *
   * @author Jared Scholz
   *
   */
  private static class ScoredSequence extends TinyBoyInputSequence
      implements Comparable<ScoredSequence> {

    /**
     * The score of the sequence being wrapped.
     */
    public int score;

    /**
     * Create a ScoredSequence that wraps a given TinyBoyInputSequence along with a
     * given score.
     *
     * @param sequence The TinyBoyInputSequence to wrap.
     * @param score    The score of the TinyBoyInputSequence.
     */
    public ScoredSequence(TinyBoyInputSequence sequence, int score) {
      super(sequence);
      this.score = score;
    }

    /**
     * Create a ScoredSequence that wraps a TinyBoyInputSequence given an input
     * sequence. Score defaults to maximum to ensure usage.
     *
     * @param pulses The input sequence for the TinyBoyInputSequence.
     */
    public ScoredSequence(ControlPad.@Nullable Button... pulses) {
      super(pulses);
      // Maximum default score to ensure usage
      this.score = Integer.MAX_VALUE;
    }

    @Override
    public int compareTo(ScoredSequence o) {
      return this.score - o.score;
    }
  }

  /**
   * SequenceData relates multiple sequence attributes into a single object for
   * storage.
   *
   * @author Jared Scholz
   *
   */
  private class SequenceData {
    /**
     * The recorded coverage of a sequence.
     */
    public BitSet coverage;
    /**
     * The number of children that must be processed before this sequence can be
     * forgotten.
     */
    public int numChildren;

    /**
     * Create a SequenceData box with recorded coverage. numChildren is initialized
     * to 0.
     *
     * @param coverage The recorded coverage of a sequence.
     */
    public SequenceData(BitSet coverage) {
      this.coverage = coverage;
      this.numChildren = 0;
    }
  }

  /**
   * The length of starting sequences (sequences that are guaranteed to be
   * attempted first). Note: these are generated in the constructor!
   */
  private static final int INITIAL_BATCH_LENGTH = 4;
  /**
   * Represents the number of buttons on the control pad.
   */
  private static final int NUM_BUTTONS = ControlPad.Button.values().length;

  /**
   * Heap of sequences to be processed. The best will be processed first.
   */
  private final PriorityBlockingQueue<ScoredSequence> worklist;
  /**
   * Data kept for sequences that still have children in the work list. This
   * allows for new records to be compared against parents.
   */
  private final ConcurrentHashMap<String, SequenceData> sequenceData;
  /**
   * Store all states recorded for redundancy checking.
   */
  private final Set<byte[]> states;
  /**
   * Lock for synchronizing read and write access to states.
   */
  private final ReadWriteLock statesLock;
  /**
   * The next sequence to be provided by generate().
   */
  private volatile @Nullable TinyBoyInputSequence next;

  /**
   * Create new input generator for the TinyBoy simulation.
   */
  public TinyBoyInputGenerator() {
    this.worklist = generateAllSequences(INITIAL_BATCH_LENGTH);
    this.sequenceData = new ConcurrentHashMap<>();
    this.states = new HashSet<>();
    this.statesLock = new ReentrantReadWriteLock(true);
    this.next = this.worklist.poll();
  }

  @Override
  public synchronized boolean hasMore() {
    // Ensure hasMore() and generate() never disagree...
    if (this.next != null) {
      return true;
    }
    this.next = this.worklist.poll();
    if (this.next != null) {
      return true;
    }
    return false;
  }

  @Override
  public synchronized @Nullable TinyBoyInputSequence generate() {
    // Ensure hasMore() and generate() never disagree...
    TinyBoyInputSequence sequence = this.next;
    this.next = this.worklist.poll();
    return sequence;
  }

  /**
   * Process a record returned from the fuzzer indicating the instruction coverage
   * and final RAM state obtained for a given input sequence. The record will be
   * scored, and if it meets certain conditions, children of the sequence will be
   * added to the work list for further processing.
   *
   * @param input    The TinyBoyInputSequence used.
   * @param coverage BitSet representing instruction coverage (1 indicates an
   *                 instruction is covered) from fuzzing.
   * @param state    RAM dump after fuzzing has occurred.
   */
  @Override
  public synchronized void record(TinyBoyInputSequence input, BitSet coverage, byte[] state) {
    // Ensure state is unique:
    this.statesLock.readLock().lock();
    boolean isUnique = !this.states.contains(state);
    this.statesLock.readLock().unlock();
    if (isUnique) {
      this.statesLock.writeLock().lock();
      this.states.add(state);
      this.statesLock.writeLock().unlock();
      // Add children of this sequence to the work list if beneficial:
      String inputHashable = input.toString();
      String parentHashable = inputHashable.substring(0, inputHashable.length() - 1);
      @Nullable
      SequenceData parentData = this.sequenceData.get(parentHashable);
      if (Objects.nonNull(parentData)) {
        // Comparison with parent is possible...
        int coverageBonus = calculateCoverageBonus(coverage, parentData.coverage);
        if (coverageBonus > 0) {
          // Something has changed for the better from the parent...
          int score = coverage.cardinality()
              + coverageBonus * (int) Math.pow(inputHashable.length(), 2);
          SequenceData inputData = new SequenceData(coverage);
          this.sequenceData.put(inputHashable, inputData);
          inputData.numChildren = advanceSequence(input, score);
        }
        // Age parent:
        if (--parentData.numChildren <= 0) {
          this.sequenceData.remove(parentHashable);
        }
      } else {
        // No parent yet exists, this will become a parent...
        SequenceData inputData = new SequenceData(coverage);
        this.sequenceData.put(inputHashable, inputData);
        inputData.numChildren = advanceSequence(input, Integer.MAX_VALUE);
      }
    }
  }

  /**
   * Add the children of a given sequence to the work list with a given score.
   *
   * @param input The parent sequence to extend.
   * @param score The score of the parent sequence.
   * @return The number of children that were added to the work list. Though
   *         unused in the current implementation, this can be used to reduce the
   *         size of the work list.
   */
  private int advanceSequence(TinyBoyInputSequence input, int score) {
    List<ControlPad.Button> buttons = Arrays.asList(ControlPad.Button.values());
    // Randomize order to stop patterns from emerging
    Collections.shuffle(buttons);
    for (int i = 0; i < NUM_BUTTONS; i++) {
      this.worklist.add(new ScoredSequence(input.append(buttons.get(i)), score));
    }
    // Include no input (null)
    this.worklist.add(new ScoredSequence(input.append((ControlPad.Button) null), score));
    return NUM_BUTTONS + 1;
  }

  /**
   * Calculate a bonus by comparing the coverage of a sequence with the coverage
   * of its parent. Increases are rewarded.
   *
   * @param coverage       The recorded coverage of a sequence.
   * @param parentCoverage The coverage of the sequences parent.
   * @return The score bonus for coverage increases. Note: if 0 is returned,
   *         parentCoverage subsumes coverage.
   */
  private static int calculateCoverageBonus(BitSet coverage, BitSet parentCoverage) {
    int bonus = 0;
    for (int i = coverage.nextSetBit(0); i >= 0; i = coverage.nextSetBit(i + 1)) {
      if (!parentCoverage.get(i)) {
        bonus++;
      }
    }
    return bonus;
  }

  /**
   * Generates a list of all possible sequences of the given length.
   *
   * @param length The length of the sequences to generate.
   * @return A list of all possible sequences of the specified length.
   */
  private static PriorityBlockingQueue<ScoredSequence> generateAllSequences(int length) {
    // Account for no input in a pulse by considering NUM_BUTTONS one greater...
    int totalSequences = (int) Math.pow(NUM_BUTTONS + 1, length);
    PriorityBlockingQueue<ScoredSequence> allSequences = new PriorityBlockingQueue<>(
        totalSequences);
    ControlPad.Button[] buttons = ControlPad.Button.values();
    ControlPad.Button[] sequence = new ControlPad.Button[length];
    // Avoid recursion overhead, iterate through all possible sequences:
    for (int s = 0; s < totalSequences; s++) {
      int sequenceIndex = s;
      for (int i = 0; i < length; i++) {
        // Use a base conversion algorithm with base NUM_BUTTONS + 1
        int buttonIndex = sequenceIndex % (NUM_BUTTONS + 1);
        sequence[i] = buttonIndex == NUM_BUTTONS ? null : buttons[buttonIndex];
        sequenceIndex /= (NUM_BUTTONS + 1);
      }
      ControlPad.Button[] sequenceClone = new ControlPad.Button[length];
      System.arraycopy(sequence, 0, sequenceClone, 0, length);
      allSequences.add(new ScoredSequence(sequenceClone));
    }
    return allSequences;
  }
}
