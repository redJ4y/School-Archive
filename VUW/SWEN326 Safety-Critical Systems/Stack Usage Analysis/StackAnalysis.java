package avranalysis.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javr.core.AvrDecoder;
import javr.core.AvrInstruction;
import javr.core.AvrInstruction.AbsoluteAddress;
import javr.core.AvrInstruction.RelativeAddress;
import javr.io.HexFile;
import javr.memory.ElasticByteMemory;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Responsible for determining the worst-case stack analysis for a given AVR
 * program.
 *
 * @author Jared Scholz, scaffolding provided by David J. Pearce
 *
 */
public class StackAnalysis {
  /**
   * Contains the raw bytes of the given firmware image being analyzed.
   */
  private ElasticByteMemory firmware;
  /**
   * The decoder is used for actually decoding an instruction.
   */
  private AvrDecoder decoder = new AvrDecoder();
  /**
   * Map that keeps visited instructions and their corresponding stack heights
   * from last visit.
   */
  private Map<Integer, Integer> visited;
  /**
   * Records the maximum height seen so far.
   */
  private int maxHeight;

  /**
   * Construct a new analysis instance for a given hex file.
   *
   * @param hf Hexfile on which the analysis will be run.
   */
  public StackAnalysis(HexFile hf) {
    // Create firmware memory
    this.firmware = new ElasticByteMemory();
    // Upload image to firmware memory
    hf.uploadTo(this.firmware);
    this.visited = new HashMap<>();
  }

  /**
   * Apply the stack analysis to the given firmware image producing a maximum
   * stack usage (in bytes).
   *
   * @return The maximum height observed thus far.
   */
  public int apply() {
    // Reset analysis fields
    this.maxHeight = 0;
    this.visited.clear();
    // Traverse instructions starting at beginning
    traverse(0, 0);
    // Return the maximum height observed
    return this.maxHeight;
  }

  /**
   * Traverse the instruction at a given pc address, assuming the stack has a
   * given height on entry.
   *
   * @param pc            Program Counter of instruction to traverse
   * @param currentHeight Current height of the stack at this point (in bytes)
   */
  private void traverse(int pc, int currentHeight) {
    // Allow for early stop:
    if (this.maxHeight == Integer.MAX_VALUE) {
      // No need to continue traversing if worst case is already assumed...
      return;
    }
    // Check whether current stack height is maximum
    this.maxHeight = Math.max(this.maxHeight, currentHeight);
    // Check whether we have terminated or not:
    if ((pc * 2) >= this.firmware.size()) {
      // We've gone over end of instruction sequence, so stop.
      return;
    }
    Integer pcBoxed = safeIntegerCast(pc);
    Integer currentHeightBoxed = safeIntegerCast(currentHeight);
    @Nullable
    Integer previousHeight = this.visited.get(pcBoxed);
    // Check for recursion/looping:
    if (Objects.nonNull(previousHeight)) {
      if (!previousHeight.equals(currentHeightBoxed)) {
        // Unstable stack height!
        this.maxHeight = Integer.MAX_VALUE;
      } // Else stable stack height...
      return; // Terminate current traversal
    }
    this.visited.put(pcBoxed, currentHeightBoxed);
    // Process instruction at this address
    AvrInstruction instruction = decodeInstructionAt(pc);
    // Move to the next logical instruction as this is always the starting point.
    int next = pc + instruction.getWidth();
    process(instruction, next, currentHeight);
    // Allow different branches to cover the same instructions...
    this.visited.remove(pcBoxed);
  }

  /**
   * Process the effect of a given instruction.
   *
   * @param instruction   Instruction to process
   * @param pc            Program counter of following instruction
   * @param currentHeight Current height of the stack at this point (in bytes)
   */
  private void process(AvrInstruction instruction, int pc, int currentHeight) {
    switch (instruction.getOpcode()) {
      // Branching instructions:
      case BRTS: // Fall through...
      case BRVC:
      case BRVS:
      case BRBC:
      case BRBS:
      case BRHC:
      case BRHS:
      case BRID:
      case BRIE:
      case BRLO:
      case BRMI:
      case BRNE:
      case BRPL:
      case BRSH:
      case BREQ:
      case BRGE:
      case BRLT: { // Explore both possible branches:
        RelativeAddress branch = (RelativeAddress) instruction;
        traverse(pc, currentHeight);
        traverse(pc + branch.k, currentHeight);
        break;
      }
      // Skipping instructions:
      case CPSE: // Fall through...
      case SBIC:
      case SBIS:
      case SBRC:
      case SBRS: { // Explore both possible branches:
        int skipWidth = decodeInstructionAt(pc).getWidth();
        traverse(pc, currentHeight);
        traverse(pc + skipWidth, currentHeight);
        break;
      }
      // Instructions related to method invocation:
      case CALL: {
        AbsoluteAddress branch = (AbsoluteAddress) instruction;
        traverse(branch.k, currentHeight + 2); // Explore the branch target
        traverse(pc, currentHeight); // Resume when RET is called
        break;
      }
      case RCALL: {
        RelativeAddress branch = (RelativeAddress) instruction;
        traverse(pc + branch.k, currentHeight + 2); // Explore the branch target
        traverse(pc, currentHeight); // Resume when RET is called
        break;
      }
      case RET: // Fall through...
      case RETI: {
        return; // Terminate current traversal
      }
      // Instructions that do not fork control-flow:
      case JMP: {
        AbsoluteAddress branch = (AbsoluteAddress) instruction;
        if (branch.k != -1) { // Check whether infinite loop; if so, terminate.
          traverse(branch.k, currentHeight); // Explore the branch target
        }
        break;
      }
      case RJMP: {
        RelativeAddress branch = (RelativeAddress) instruction;
        if (branch.k != -1) { // Check whether infinite loop; if so, terminate.
          traverse(pc + branch.k, currentHeight); // Explore the branch target
        }
        break;
      }
      case PUSH: {
        traverse(pc, currentHeight + 1);
        break;
      }
      case POP: {
        traverse(pc, currentHeight - 1);
        break;
      }
      default: {
        // Control is transferred to the following instruction...
        traverse(pc, currentHeight);
        break;
      }
    }
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

  /**
   * Decode the instruction at a given PC location.
   *
   * @param pc Address of instruction to decode.
   * @return Instruction which has been decoded.
   */
  private AvrInstruction decodeInstructionAt(int pc) {
    AvrInstruction insn = this.decoder.decode(this.firmware, pc);
    assert insn != null;
    return insn;
  }
}
