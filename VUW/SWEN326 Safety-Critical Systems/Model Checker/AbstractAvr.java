package avrmc.core;

import static avrmc.core.AbstractMemory.Bit.FALSE;
import static avrmc.core.AbstractMemory.Bit.TRUE;
import static avrmc.core.AbstractMemory.Bit.UNKNOWN;

import avrmc.core.AbstractMemory.Bit;
import avrmc.core.AbstractMemory.Byte;
import avrmc.core.AbstractMemory.Word;
import java.io.PrintStream;
import java.util.Arrays;
import javr.core.AVR;
import javr.core.AVR.Memory;
import javr.core.AvrDecoder;
import javr.core.AvrInstruction;
import javr.memory.ByteMemory;
import org.eclipse.jdt.annotation.Nullable;

/**
 * represents an abstract AVR machine state which can hold <i>unknown</i>
 * values. For example, bits which are neither <code>true</code> or
 * <code>false</code>. Likewise, bytes which do not have a known integer value.
 *
 * @author David J. Pearce, completed by Jared Scholz
 *
 */
public class AbstractAvr implements Cloneable {
  /**
   * responsible for decoding instructions. This has to be done lazily as we
   * cannot otherwise tell what is code, versus what is data in the flash memory.
   */
  private static final AvrDecoder decoder = new AvrDecoder();
  /**
   * Flash memory in the abstract AVR. This is concrete because it represents the
   * firmware being executed (i.e. whose contents is concrete).
   */
  private final Memory code;
  /**
   * Model of data Memory in the abstract AVR (i.e. including registers and SrAM).
   * This is abstract because it may contain locations within unknown values.
   */
  private final AbstractMemory data;
  /**
   * Interrupt table.
   */
  private final AVR.Interrupt[] interrupts;
  /**
   * Cache of decoded instructions. This just means we don't have to decode
   * everything everytime.
   */
  private final AvrInstruction[] decoded;
  /**
   * represents the Program Counter register. This is concrete because we always
   * know where the AVR is.
   */
  private int programCounter;
  /**
   * represents the <i>carry flag</i> in the <code>SREG</code> status register.
   */
  private Bit carryFlag;
  /**
   * represents the <i>zero flag</i> in the <code>SREG</code> status register.
   */
  private Bit zeroFlag;
  /**
   * represents the <i>negative flag</i> in the <code>SREG</code> status register.
   */
  private Bit negativeFlag;
  /**
   * represents the <i>overflow flag</i> in the <code>SREG</code> status register.
   */
  private Bit overflowFlag;
  /**
   * represents the <i>sign flag</i> in the <code>SREG</code> status register.
   */
  private Bit signFlag;
  /**
   * represents the <i>halfcarry flag</i> in the <code>SREG</code> status
   * register.
   */
  private Bit halfCarryFlag;
  /**
   * represents the <i>bitcopy flag</i> in the <code>SREG</code> status register.
   */
  private Bit bitcopyFlag;
  /**
   * represents the <i>interrupt flag</i> in the <code>SREG</code> status
   * register.
   */
  private Bit interruptFlag;

  /**
   * Construct an abstract AVR with a given <code>code</code> and
   * <code>data</code> size.
   *
   * @param code       Size (in bytes) of the FLASH memory where the code
   *                   instructions are stored.
   * @param data       Size (in bytes) of the DATA memory where registers, I/O
   *                   ports and SrAM are stored.
   * @param interrupts Interrupt triggers.
   */
  public AbstractAvr(int code, int data, AVR.Interrupt... interrupts) {
    this.code = new ByteMemory(code);
    this.data = new AbstractMemory(data);
    this.interrupts = interrupts;
    this.decoded = new AvrInstruction[code];
    //
    this.carryFlag = Bit.FALSE;
    this.zeroFlag = Bit.FALSE;
    this.negativeFlag = Bit.FALSE;
    this.overflowFlag = Bit.FALSE;
    this.signFlag = Bit.FALSE;
    this.halfCarryFlag = Bit.FALSE;
    this.bitcopyFlag = Bit.FALSE;
    this.interruptFlag = Bit.FALSE;
  }

  /**
   * Make a (deep) copy of a given AVR state. Since only the data segments are
   * mutated during execution, this is the only part that needs to be actually
   * cloned.
   *
   * @param state Abstract machine state to copy.
   */
  private AbstractAvr(AbstractAvr state) {
    this.code = state.code;
    this.data = new AbstractMemory(state.data);
    this.interrupts = state.interrupts;
    this.decoded = state.decoded;
    //
    this.programCounter = state.programCounter;
    this.carryFlag = state.carryFlag;
    this.zeroFlag = state.zeroFlag;
    this.negativeFlag = state.negativeFlag;
    this.overflowFlag = state.overflowFlag;
    this.signFlag = state.signFlag;
    this.halfCarryFlag = state.halfCarryFlag;
    this.bitcopyFlag = state.bitcopyFlag;
    this.interruptFlag = state.interruptFlag;
  }

  @Override
  public int hashCode() {
    int sregHashCode = Arrays.hashCode(this.getStatusRegister());
    int dataHashCode = this.data.hashCode();
    // Combine hashCodes:
    int result = 17; // Primes for distribution...
    result = 31 * result + sregHashCode;
    result = 31 * result + dataHashCode;
    return result;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == null || !(o instanceof AbstractAvr)) {
      return false;
    }
    AbstractAvr compareTo = (AbstractAvr) o;
    Bit[] sreg = this.getStatusRegister();
    Bit[] compareToSreg = compareTo.getStatusRegister();
    for (int i = 0; i < 8; i++) {
      // Consider UNKNOWN Bits to check for "exact" equality
      if (sreg[i] != compareToSreg[i]) {
        return false;
      }
    }
    return this.data.equals(compareTo.getData());
  }

  /**
   * Get the contents of the <code>SREG</code> status register.
   *
   * @return An array of 8 Bits representing every flag in the <code>SREG</code>
   *         status register.
   */
  public Bit[] getStatusRegister() {
    Bit[] sreg = new Bit[8];
    sreg[0] = this.carryFlag;
    sreg[1] = this.zeroFlag;
    sreg[2] = this.negativeFlag;
    sreg[3] = this.overflowFlag;
    sreg[4] = this.signFlag;
    sreg[5] = this.halfCarryFlag;
    sreg[6] = this.bitcopyFlag;
    sreg[7] = this.interruptFlag;
    return sreg;
  }

  /**
   * Get the code (i.e. FLASH) memory associated with this (abstract) AVR.
   *
   * @return FLASH memory.
   */
  public Memory getCode() {
    return this.code;
  }

  /**
   * Get the abstract data memory associated with this (abstract) AVR.
   *
   * @return Abstract memory.
   */

  public AbstractMemory getData() {
    return this.data;
  }

  /**
   * Check whether this machine has halted or not.
   *
   * @return True if the machine has halted, false otherwise.
   */
  public boolean isHalted() {
    return this.programCounter < 0;
  }

  /**
   * Get the current program counter for this (abstract) AVR.
   *
   * @return Program Counter
   */
  public int getProgramCounter() {
    return this.programCounter;
  }

  /**
   * Clock the abstract machine to execute one instruction. This will update this
   * state accordingly, and may produce a forked state as well (e.g. if a
   * <i>choice point</i> was encountered).
   *
   * @return <code>null</code> (if no forked state produced); otherwise, returns a
   *         forked state.
   * @throws AVR.HaltedException Halted exception is thrown if this machine halts.
   */
  public @Nullable AbstractAvr clock() throws AVR.HaltedException {
    // First check for interrupts
    handleInterrupts();
    // Second decode relevant instruction
    AvrInstruction insn = decode(this.programCounter);
    // Dispatch on instruction
    switch (insn.getOpcode()) {
      case ADC:
        return execute((AvrInstruction.ADC) insn);
      case ADD:
        return execute((AvrInstruction.ADD) insn);
      case ADIW:
        return execute((AvrInstruction.ADIW) insn);
      case AND:
        return execute((AvrInstruction.AND) insn);
      case ANDI:
        return execute((AvrInstruction.ANDI) insn);
      case ASR:
        return execute((AvrInstruction.ASR) insn);
      case BCLR:
        return execute((AvrInstruction.BCLR) insn);
      case BLD:
        return execute((AvrInstruction.BLD) insn);
      case BRBC:
        return execute((AvrInstruction.BRBC) insn);
      case BRBS:
        return execute((AvrInstruction.BRBS) insn);
      case BREQ:
        return execute((AvrInstruction.BREQ) insn);
      case BRGE:
        return execute((AvrInstruction.BRGE) insn);
      case BRHC:
        return execute((AvrInstruction.BRHC) insn);
      case BRHS:
        return execute((AvrInstruction.BRHS) insn);
      case BRID:
        return execute((AvrInstruction.BRID) insn);
      case BRIE:
        return execute((AvrInstruction.BRIE) insn);
      case BRLO:
        return execute((AvrInstruction.BRLO) insn);
      case BRLT:
        return execute((AvrInstruction.BRLT) insn);
      case BRMI:
        return execute((AvrInstruction.BRMI) insn);
      case BRNE:
        return execute((AvrInstruction.BRNE) insn);
      case BRPL:
        return execute((AvrInstruction.BRPL) insn);
      case BRSH:
        return execute((AvrInstruction.BRSH) insn);
      case BRTC:
        return execute((AvrInstruction.BRTC) insn);
      case BRTS:
        return execute((AvrInstruction.BRTS) insn);
      case BRVC:
        return execute((AvrInstruction.BRVC) insn);
      case BRVS:
        return execute((AvrInstruction.BRVS) insn);
      case BSET:
        return execute((AvrInstruction.BSET) insn);
      case BST:
        return execute((AvrInstruction.BST) insn);
      case CALL:
        return execute((AvrInstruction.CALL) insn);
      case CBI:
        return execute((AvrInstruction.CBI) insn);
      case CLC:
        return execute((AvrInstruction.CLC) insn);
      case CLH:
        return execute((AvrInstruction.CLH) insn);
      case CLI:
        return execute((AvrInstruction.CLI) insn);
      case CLN:
        return execute((AvrInstruction.CLN) insn);
      case CLS:
        return execute((AvrInstruction.CLS) insn);
      case CLT:
        return execute((AvrInstruction.CLT) insn);
      case CLV:
        return execute((AvrInstruction.CLV) insn);
      case CLZ:
        return execute((AvrInstruction.CLZ) insn);
      case COM:
        return execute((AvrInstruction.COM) insn);
      case CP:
        return execute((AvrInstruction.CP) insn);
      case CPC:
        return execute((AvrInstruction.CPC) insn);
      case CPI:
        return execute((AvrInstruction.CPI) insn);
      case CPSE:
        return execute((AvrInstruction.CPSE) insn);
      case DEC:
        return execute((AvrInstruction.DEC) insn);
      case EOR:
        return execute((AvrInstruction.EOR) insn);
      case ICALL:
        return execute((AvrInstruction.ICALL) insn);
      case IJMP:
        return execute((AvrInstruction.IJMP) insn);
      case IN:
        return execute((AvrInstruction.IN) insn);
      case INC:
        return execute((AvrInstruction.INC) insn);
      case JMP:
        return execute((AvrInstruction.JMP) insn);
      case LD_X:
        return execute((AvrInstruction.LD_X) insn);
      case LD_X_INC:
        return execute((AvrInstruction.LD_X_INC) insn);
      case LD_X_DEC:
        return execute((AvrInstruction.LD_X_DEC) insn);
      case LD_Y:
        return execute((AvrInstruction.LD_Y) insn);
      case LD_Y_INC:
        return execute((AvrInstruction.LD_Y_INC) insn);
      case LD_Y_DEC:
        return execute((AvrInstruction.LD_Y_DEC) insn);
      case LDD_Y_Q:
        return execute((AvrInstruction.LDD_Y_Q) insn);
      case LD_Z:
        return execute((AvrInstruction.LD_Z) insn);
      case LD_Z_INC:
        return execute((AvrInstruction.LD_Z_INC) insn);
      case LD_Z_DEC:
        return execute((AvrInstruction.LD_Z_DEC) insn);
      case LDD_Z_Q:
        return execute((AvrInstruction.LDD_Z_Q) insn);
      case LDI:
        return execute((AvrInstruction.LDI) insn);
      case LDS:
        return execute((AvrInstruction.LDS) insn);
      case LPM_Z:
        return execute((AvrInstruction.LPM_Z) insn);
      case LPM_Z_INC:
        return execute((AvrInstruction.LPM_Z_INC) insn);
      case LSR:
        return execute((AvrInstruction.LSR) insn);
      case MOV:
        return execute((AvrInstruction.MOV) insn);
      case MOVW:
        return execute((AvrInstruction.MOVW) insn);
      case NEG:
        return execute((AvrInstruction.NEG) insn);
      case NOP:
        return execute((AvrInstruction.NOP) insn);
      case OR:
        return execute((AvrInstruction.OR) insn);
      case ORI:
        return execute((AvrInstruction.ORI) insn);
      case OUT:
        return execute((AvrInstruction.OUT) insn);
      case POP:
        return execute((AvrInstruction.POP) insn);
      case PUSH:
        return execute((AvrInstruction.PUSH) insn);
      case RCALL:
        return execute((AvrInstruction.RCALL) insn);
      case RET:
        return execute((AvrInstruction.RET) insn);
      case RJMP:
        return execute((AvrInstruction.RJMP) insn);
      case ROR:
        return execute((AvrInstruction.ROR) insn);
      case SBC:
        return execute((AvrInstruction.SBC) insn);
      case SBCI:
        return execute((AvrInstruction.SBCI) insn);
      case SBI:
        return execute((AvrInstruction.SBI) insn);
      case SBIC:
        return execute((AvrInstruction.SBIC) insn);
      case SBIS:
        return execute((AvrInstruction.SBIS) insn);
      case SBIW:
        return execute((AvrInstruction.SBIW) insn);
      case SBR:
        return execute((AvrInstruction.SBR) insn);
      case SBRC:
        return execute((AvrInstruction.SBRC) insn);
      case SBRS:
        return execute((AvrInstruction.SBRS) insn);
      case SEC:
        return execute((AvrInstruction.SEC) insn);
      case SEH:
        return execute((AvrInstruction.SEH) insn);
      case SEI:
        return execute((AvrInstruction.SEI) insn);
      case SEN:
        return execute((AvrInstruction.SEN) insn);
      case SER:
        return execute((AvrInstruction.SER) insn);
      case SES:
        return execute((AvrInstruction.SES) insn);
      case SET:
        return execute((AvrInstruction.SET) insn);
      case SEV:
        return execute((AvrInstruction.SEV) insn);
      case SEZ:
        return execute((AvrInstruction.SEZ) insn);
      case ST_X:
        return execute((AvrInstruction.ST_X) insn);
      case ST_X_INC:
        return execute((AvrInstruction.ST_X_INC) insn);
      case ST_X_DEC:
        return execute((AvrInstruction.ST_X_DEC) insn);
      case ST_Y:
        return execute((AvrInstruction.ST_Y) insn);
      case ST_Y_INC:
        return execute((AvrInstruction.ST_Y_INC) insn);
      case ST_Y_DEC:
        return execute((AvrInstruction.ST_Y_DEC) insn);
      case STD_Y_Q:
        return execute((AvrInstruction.STD_Y_Q) insn);
      case ST_Z:
        return execute((AvrInstruction.ST_Z) insn);
      case ST_Z_INC:
        return execute((AvrInstruction.ST_Z_INC) insn);
      case ST_Z_DEC:
        return execute((AvrInstruction.ST_Z_DEC) insn);
      case STD_Z_Q:
        return execute((AvrInstruction.STD_Z_Q) insn);
      case STS_DATA_WIDE:
        return execute((AvrInstruction.STS_DATA_WIDE) insn);
      case SUB:
        return execute((AvrInstruction.SUB) insn);
      case SUBI:
        return execute((AvrInstruction.SUBI) insn);
      case SWAP:
        return execute((AvrInstruction.SWAP) insn);
      case XCH:
        return execute((AvrInstruction.XCH) insn);
      default:
        throw new IllegalArgumentException();
    }
  }

  @Override
  public AbstractAvr clone() {
    return new AbstractAvr(this);
  }

  @Override
  public String toString() {
    return LANGLE + this.programCounter + SEMICOLON + this.carryFlag + this.zeroFlag
        + this.negativeFlag + this.overflowFlag + this.signFlag + this.halfCarryFlag
        + this.bitcopyFlag + this.interruptFlag + SEMICOLON
        + Integer.toHexString(this.data.hashCode()) + RANGLE;
  }

  /**
   * Print a formatted string representing the entire state. This is primarily
   * useful for debugging.
   *
   * @param out The output stream where the debug information is printed
   */
  public void print(PrintStream out) {
    int n = this.data.size() / 16;
    int address = 0;
    out.println(DBG_BEG + Integer.toHexString(this.programCounter) + DBG_MID + this.carryFlag
        + this.zeroFlag + this.negativeFlag + this.overflowFlag + this.signFlag + this.halfCarryFlag
        + this.bitcopyFlag + this.interruptFlag);
    for (int i = 0; i != n; ++i) {
      out.print(String.format(FMT_32, Integer.valueOf(i * 16)));
      out.print(BAR);
      for (int j = 0; j < 16; ++j) {
        Byte b = this.data.read(address);
        out.print(b.toString() + COLON);
        address = address + 1;
      }
      out.println();
    }
  }

  /**
   * Decode an instruction at a given address, whilst updating the cache of
   * previously decoded instructions correctly. If the instruction was not
   * previously decoded, then it will be now.
   *
   * @param address Location in FLASH memory of instruction to be decoded.
   *
   * @return Instruction at the given address.
   */
  private AvrInstruction decode(int address) {
    @Nullable
    AvrInstruction insn = this.decoded[address];
    if (insn == null) {
      // Instruction not previously decoded. Therefore, decode and cache for later.
      insn = decoder.decode(this.code, address);
      this.decoded[address] = insn;
      assert insn != null;
    }
    return insn;
  }

  /**
   * responsible for handling interrupts which are raised. There are various ways
   * that interrupts can be raised, such as via the internal watchdown timer, etc.
   *
   */
  private void handleInterrupts() {
    // Check whether interrupts are enabled or not.
    if (this.interruptFlag == TRUE) {
      // Check whether an interrupt is triggered or not
      int vector = determineInterruptVector();
      //
      if (vector >= 0) {
        // yes, interrupt triggered so disable interrupts
        this.interruptFlag = FALSE;
        // push PC
        pushWord(Word.from(this.programCounter));
        // jump to interrupt vector
        this.programCounter = vector;
      }
    } else if (this.interruptFlag == UNKNOWN) {
      // Sanity test for now.
      throw new IllegalArgumentException();
    }
  }

  /**
   * Determine whether an interrupt is signaled or not. This is done by checking
   * the relevant ways in which an interrupt can be signaled.
   *
   * @return The interrupt vector being signaled, or negative one if none
   *         signaled.
   */
  private int determineInterruptVector() {
    for (int i = 0; i != this.interrupts.length; ++i) {
      if (this.interrupts[i].get()) {
        this.interrupts[i].clear();
        return i;
      }
    }
    return -1;
  }

  /**
   * Adds two registers and the contents of the C Flag and places the result in
   * the destination register rd.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ADC insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Byte rr = this.data.read(insn.Rr);
    Byte cf = Byte.from(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, this.carryFlag);
    // Perform operation
    Byte r = rd.add(rr.add(cf));
    // Update register file
    this.data.write(insn.Rd, r);
    // Set Flags
    Bit rd7 = rd.get(7);
    Bit rr7 = rr.get(7);
    Bit r7 = r.get(7);
    //
    this.carryFlag = or(and(rd7, rr7), and(rr7, not(r7)), and(not(r7), rd7));
    this.zeroFlag = r.isZero();
    this.negativeFlag = r7;
    this.overflowFlag = or(and(rd7, rr7, not(r7)), and(not(rd7), not(rr7), r7));
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    //
    Bit rd3 = rd.get(3);
    Bit rr3 = rr.get(3);
    Bit r3 = r.get(3);
    this.halfCarryFlag = or(and(rd3, rr3), and(rr3, not(r3)), and(not(r3), rd3));
    //
    return null;
  }

  /**
   * Adds two registers without the C Flag and places the result in the
   * destination register rd.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ADD insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Byte rr = this.data.read(insn.Rr);
    // Perform operation
    Byte r = rd.add(rr);
    // Update register file
    this.data.write(insn.Rd, r);
    // Set Flags
    Bit rd7 = rd.get(7);
    Bit rr7 = rr.get(7);
    Bit r7 = r.get(7);
    //
    this.carryFlag = or(and(rd7, rr7), and(rr7, not(r7)), and(not(r7), rd7));
    this.zeroFlag = r.isZero();
    this.negativeFlag = r7;
    this.overflowFlag = or(and(rd7, rr7, not(r7)), and(not(rd7), not(rr7), r7));
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    Bit rd3 = rd.get(3);
    Bit rr3 = rr.get(3);
    Bit r3 = r.get(3);
    //
    this.halfCarryFlag = or(and(rd3, rr3), and(rr3, not(r3)), and(not(r3), rd3));
    //
    return null;
  }

  /**
   * Adds an immediate value (0 - 63) to a register pair and places the result in
   * the register pair. This instruction operates on the upper four register
   * pairs, and is well suited for operations on the pointer registers.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ADIW insn) {
    this.programCounter = this.programCounter + 1;
    Word rd = readWord(insn.Rd);
    // Perform operation
    Word r = rd.add((byte) insn.K);
    // Update register file
    writeWord(insn.Rd, r);
    // Set Flags
    Bit rdh7 = rd.get(15);
    Bit r15 = r.get(15);
    //
    this.carryFlag = and(not(r15), rdh7);
    this.zeroFlag = r.isZero();
    this.negativeFlag = r15;
    this.overflowFlag = and(not(rdh7), r15);
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    //
    return null;
  }

  /**
   * Performs the logical AND between the contents of register rd and register rr,
   * and places the result in the destination register rd.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.AND insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Byte rr = this.data.read(insn.Rr);
    // Perform operation
    Byte r = rd.and(rr);
    // Update register file
    this.data.write(insn.Rd, r);
    // Set Flags
    Bit r7 = r.get(7);
    //
    this.zeroFlag = r.isZero();
    this.negativeFlag = r7;
    this.overflowFlag = FALSE;
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    //
    return null;
  }

  /**
   * Performs the logical AND between the contents of register rd and a constant,
   * and places the result in the destination register rd.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ANDI insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    byte rr = (byte) insn.K;
    // Perform operation
    Byte r = rd.and(Byte.from(rr));
    // Update register file
    this.data.write(insn.Rd, r);
    // Set Flags
    Bit r7 = r.get(7);
    //
    this.zeroFlag = r.isZero();
    this.negativeFlag = r7;
    this.overflowFlag = FALSE;
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    //
    return null;
  }

  /**
   * Shifts all bits in rd one place to the right. Bit 7 is held constant. Bit 0
   * is loaded into the C Flag of the SREG. This operation effectively divides a
   * signed value by two without changing its sign. The Carry Flag can be used to
   * round the result.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ASR insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    // Perform operation
    Byte r = rd.shr(1);
    // Update register file
    this.data.write(insn.Rd, r);
    // Set Flags
    this.carryFlag = or(rd.get(7), rd.get(0));
    this.zeroFlag = r.isZero();
    this.negativeFlag = r.get(7);
    this.overflowFlag = xor(this.negativeFlag, this.carryFlag);
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    //
    return null;
  }

  /**
   * Clears a single Flag in SREG.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BCLR insn) {
    this.programCounter = this.programCounter + 1;
    setStatusBit(insn.s, FALSE);
    //
    return null;
  }

  /**
   * Copies the T Flag in the SREG (Status register) to bit b in register rd.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BLD insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    rd = rd.set(insn.b, this.bitcopyFlag);
    this.data.write(insn.Rd, rd);
    //
    return null;
  }

  /**
   * Conditional relative branch. Tests a single bit in SREG and branches
   * relatively to PC if the bit is cleared. This instruction branches relatively
   * to PC in either direction (PC - 63 ≤ destination ≤ PC + 64). Parameter k is
   * the offset from PC and is represented in two’s complement form.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRBC insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    Bit bit = getStatusBit(insn.s);
    if (bit == UNKNOWN) {
      setStatusBit(insn.s, TRUE);
      fork = clone();
      bit = FALSE;
      setStatusBit(insn.s, bit);
    }
    if (bit == FALSE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests a single bit in SREG and branches
   * relatively to PC if the bit is set. This instruction branches relatively to
   * PC in either direction (PC - 63 ≤ destination ≤ PC + 64). Parameter k is the
   * offset from PC and is represented in two’s complement form.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRBS insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    Bit bit = getStatusBit(insn.s);
    if (bit == UNKNOWN) {
      setStatusBit(insn.s, FALSE);
      fork = clone();
      bit = TRUE;
      setStatusBit(insn.s, bit);
    }
    if (bit == TRUE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests the Zero Flag (Z) and branches relatively
   * to PC if Z is set. If the instruction is executed immediately after any of
   * the instructions CP, CPI, SUB, or SUBI, the branch will occur if and only if
   * the unsigned or signed binary number represented in rd was equal to the
   * unsigned or signed binary number represented in rr. This instruction branches
   * relatively to PC in either direction (PC - 63 ≤ destination ≤ PC + 64).
   * Parameter k is the offset from PC and is represented in two’s complement
   * form. (Equivalent to instruction BRBS 1,k.)
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BREQ insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    if (this.zeroFlag == UNKNOWN) {
      this.zeroFlag = FALSE;
      fork = clone();
      this.zeroFlag = TRUE;
    }
    if (this.zeroFlag == TRUE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests the Signed Flag (S) and branches
   * relatively to PC if S is cleared. If the instruction is executed immediately
   * after any of the instructions CP, CPI, SUB, or SUBI, the branch will occur if
   * and only if the signed binary number represented in rd was greater than or
   * equal to the signed binary number represented in rr. This instruction
   * branches relatively to PC in either direction (PC - 63 ≤ destination ≤ PC +
   * 64). Parameter k is the offset from PC and is represented in two’s complement
   * form. (Equivalent to instruction BRBC 4,k.)
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRGE insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    if (this.signFlag == UNKNOWN) {
      this.signFlag = TRUE;
      fork = clone();
      this.signFlag = FALSE;
    }
    if (this.signFlag == FALSE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests the Half Carry Flag (H) and branches
   * relatively to PC if H is cleared. This instruction branches relatively to PC
   * in either direction (PC - 63 ≤ destination ≤ PC + 64). Parameter k is the
   * offset from PC and is represented in two’s complement form. (Equivalent to
   * instruction BRBC 5,k.)
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRHC insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    if (this.halfCarryFlag == UNKNOWN) {
      this.halfCarryFlag = TRUE;
      fork = clone();
      this.halfCarryFlag = FALSE;
    }
    if (this.halfCarryFlag == FALSE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests the Half Carry Flag (H) and branches
   * relatively to PC if H is set. This instruction branches relatively to PC in
   * either direction (PC - 63 ≤ destination ≤ PC + 64). Parameter k is the offset
   * from PC and is represented in two’s complement form. (Equivalent to
   * instruction BRBS 5,k.)
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRHS insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    if (this.halfCarryFlag == UNKNOWN) {
      this.halfCarryFlag = FALSE;
      fork = clone();
      this.halfCarryFlag = TRUE;
    }
    if (this.halfCarryFlag == TRUE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests the Global Interrupt Flag (I) and branches
   * relatively to PC if I is cleared. This instruction branches relatively to PC
   * in either direction (PC - 63 ≤ destination ≤ PC + 64). Parameter k is the
   * offset from PC and is represented in two’s complement form. (Equivalent to
   * instruction BRBC 7,k.)
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRID insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    if (this.interruptFlag == UNKNOWN) {
      this.interruptFlag = TRUE;
      fork = clone();
      this.interruptFlag = FALSE;
    }
    if (this.interruptFlag == FALSE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests the Global Interrupt Flag (I) and branches
   * relatively to PC if I is set. This instruction branches relatively to PC in
   * either direction (PC - 63 ≤ destination ≤ PC + 64). Parameter k is the offset
   * from PC and is represented in two’s complement form. (Equivalent to
   * instruction BRBS 7,k.)
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRIE insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    if (this.interruptFlag == UNKNOWN) {
      this.interruptFlag = FALSE;
      fork = clone();
      this.interruptFlag = TRUE;
    }
    if (this.interruptFlag == TRUE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests the Carry Flag (C) and branches relatively
   * to PC if C is set. If the instruction is executed immediately after any of
   * the instructions CP, CPI, SUB, or SUBI, the branch will occur if and only if,
   * the unsigned binary number represented in rd was smaller than the unsigned
   * binary number represented in rr. This instruction branches relatively to PC
   * in either direction (PC - 63 ≤ destination ≤ PC + 64). Parameter k is the
   * offset from PC and is represented in two’s complement form. (Equivalent to
   * instruction BRBS 0,k.)
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRLO insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    if (this.carryFlag == UNKNOWN) {
      this.carryFlag = FALSE;
      fork = clone();
      this.carryFlag = TRUE;
    }
    if (this.carryFlag == TRUE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests the Signed Flag (S) and branches
   * relatively to PC if S is set. If the instruction is executed immediately
   * after any of the instructions CP, CPI, SUB, or SUBI, the branch will occur if
   * and only if, the signed binary number represented in rd was less than the
   * signed binary number represented in rr. This instruction branches relatively
   * to PC in either direction (PC - 63 ≤ destination ≤ PC + 64). Parameter k is
   * the offset from PC and is represented in two’s complement form. (Equivalent
   * to instruction BRBS 4,k.)
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRLT insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    if (this.signFlag == UNKNOWN) {
      this.signFlag = FALSE;
      fork = clone();
      this.signFlag = TRUE;
    }
    if (this.signFlag == TRUE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests the Negative Flag (N) and branches
   * relatively to PC if N is set. This instruction branches relatively to PC in
   * either direction (PC - 63 ≤ destination ≤ PC + 64). Parameter k is the offset
   * from PC and is represented in two’s complement form. (Equivalent to
   * instruction BRBS 2,k.)
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRMI insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    if (this.negativeFlag == UNKNOWN) {
      this.negativeFlag = FALSE;
      fork = clone();
      this.negativeFlag = TRUE;
    }
    if (this.negativeFlag == TRUE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests the Zero Flag (Z) and branches relatively
   * to PC if Z is cleared. If the instruction is executed immediately after any
   * of the instructions CP, CPI, SUB, or SUBI, the branch will occur if and only
   * if, the unsigned or signed binary number represented in rd was not equal to
   * the unsigned or signed binary number represented in rr. This instruction
   * branches relatively to PC in either direction (PC - 63 ≤ destination ≤ PC +
   * 64). Parameter k is the offset from PC and is represented in two’s complement
   * form. (Equivalent to instruction BRBC 1,k.)
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRNE insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    if (this.zeroFlag == UNKNOWN) {
      this.zeroFlag = TRUE;
      fork = clone();
      this.zeroFlag = FALSE;
    }
    if (this.zeroFlag == FALSE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests the Negative Flag (N) and branches
   * relatively to PC if N is cleared. This instruction branches relatively to PC
   * in either direction (PC - 63 ≤ destination ≤ PC + 64). Parameter k is the
   * offset from PC and is represented in two’s complement form. (Equivalent to
   * instruction BRBC 2,k.)
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRPL insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    if (this.negativeFlag == UNKNOWN) {
      this.negativeFlag = TRUE;
      fork = clone();
      this.negativeFlag = FALSE;
    }
    if (this.negativeFlag == FALSE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests the Carry Flag (C) and branches relatively
   * to PC if C is cleared. If the instruction is executed immediately after
   * execution of any of the instructions CP, CPI, SUB, or SUBI, the branch will
   * occur if and only if, the unsigned binary number represented in rd was
   * greater than or equal to the unsigned binary number represented in rr. This
   * instruction branches relatively to PC in either direction (PC - 63 ≤
   * destination ≤ PC + 64). Parameter k is the offset from PC and is represented
   * in two’s complement form. (Equivalent to instruction BRBC 0,k.)
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRSH insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    if (this.carryFlag == UNKNOWN) {
      this.carryFlag = TRUE;
      fork = clone();
      this.carryFlag = FALSE;
    }
    if (this.carryFlag == FALSE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests the T Flag and branches relatively to PC
   * if T is cleared. This instruction branches relatively to PC in either
   * direction (PC - 63 ≤ destination ≤ PC + 64). Parameter k is the offset from
   * PC and is represented in two’s complement form. (Equivalent to instruction
   * BRBC 6,k.)
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRTC insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    if (this.bitcopyFlag == UNKNOWN) {
      this.bitcopyFlag = TRUE;
      fork = clone();
      this.bitcopyFlag = FALSE;
    }
    if (this.bitcopyFlag == FALSE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests the T Flag and branches relatively to PC
   * if T is set. This instruction branches relatively to PC in either direction
   * (PC - 63 ≤ destination ≤ PC + 64). Parameter k is the offset from PC and is
   * represented in two’s complement form. (Equivalent to instruction BRBS 6,k.)
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRTS insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    if (this.bitcopyFlag == UNKNOWN) {
      this.bitcopyFlag = FALSE;
      fork = clone();
      this.bitcopyFlag = TRUE;
    }
    if (this.bitcopyFlag == TRUE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests the Overflow Flag (V) and branches
   * relatively to PC if V is cleared. This instruction branches relatively to PC
   * in either direction (PC - 63 ≤ destination ≤ PC + 64). Parameter k is the
   * offset from PC and is represented in two’s complement form. (Equivalent to
   * instruction BRBC 3,k.)
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRVC insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    if (this.overflowFlag == UNKNOWN) {
      this.overflowFlag = TRUE;
      fork = clone();
      this.overflowFlag = FALSE;
    }
    if (this.overflowFlag == FALSE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Conditional relative branch. Tests the Overflow Flag (V) and branches
   * relatively to PC if V is set. This instruction branches relatively to PC in
   * either direction (PC - 63 ≤ destination ≤ PC + 64). Parameter k is the offset
   * from PC and is represented in two’s complement form. (Equivalent to
   * instruction BRBS 3,k.)
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BRVS insn) {
    this.programCounter = this.programCounter + 1;
    AbstractAvr fork = null;
    if (this.overflowFlag == UNKNOWN) {
      this.overflowFlag = FALSE;
      fork = clone();
      this.overflowFlag = TRUE;
    }
    if (this.overflowFlag == TRUE) {
      this.programCounter = this.programCounter + insn.k;
    }
    //
    return fork;
  }

  /**
   * Sets a single Flag or bit in SREG.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BSET insn) {
    this.programCounter = (this.programCounter + 1);
    setStatusBit(insn.s, TRUE);
    return null;
  }

  /**
   * Stores bit b from rd to the T Flag in SREG (Status register).
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.BST insn) {
    this.programCounter = (this.programCounter + 1);
    Byte rd = this.data.read(insn.Rd);
    this.bitcopyFlag = rd.get(insn.b);
    return null;
  }

  /**
   * Calls to a subroutine within the entire Program memory. The return address
   * (to the instruction after the CALL) will be stored onto the Stack. (See also
   * RCALL). The Stack Pointer uses a post-decrement scheme during CALL.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.CALL insn) {
    this.programCounter = (this.programCounter + 2);
    pushWord(Word.from(this.programCounter));
    this.programCounter = insn.k;
    return null;
  }

  /**
   * Clears a specified bit in an I/O register. This instruction operates on the
   * lower 32 I/O registers – addresses 0-31.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.CBI insn) {
    this.programCounter = this.programCounter + 1;
    Byte a = this.data.read(insn.A + 32);
    a.clear(insn.b);
    this.data.write(insn.A + 32, a);
    return null;
  }

  /**
   * Clears the Carry Flag (C) in SREG (Status register).
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.CLC insn) {
    this.programCounter = this.programCounter + 1;
    this.carryFlag = FALSE;
    return null;
  }

  /**
   * Clears the Half Carry Flag (H) in SREG (Status register).
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.CLH insn) {
    this.programCounter = this.programCounter + 1;
    this.halfCarryFlag = FALSE;
    return null;
  }

  /**
   * Clears the Global Interrupt Flag (I) in SREG (Status register). The
   * interrupts will be immediately disabled. No interrupt will be executed after
   * the CLI instruction, even if it occurs simultaneously with the CLI
   * instruction.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.CLI insn) {
    this.programCounter = this.programCounter + 1;
    this.interruptFlag = FALSE;
    return null;
  }

  /**
   * Clears the Negative Flag (N) in SREG (Status register).
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.CLN insn) {
    this.programCounter = this.programCounter + 1;
    this.negativeFlag = FALSE;
    return null;
  }

  /**
   * Clears the Signed Flag (S) in SREG (Status register).
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.CLS insn) {
    this.programCounter = this.programCounter + 1;
    this.signFlag = FALSE;
    return null;
  }

  /**
   * Clears the T (or BitCopy) Flag in SREG (Status register).
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.CLT insn) {
    this.programCounter = this.programCounter + 1;
    this.bitcopyFlag = FALSE;
    return null;
  }

  /**
   * Clears the Overflow Flag (V) in SREG (Status register).
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.CLV insn) {
    this.programCounter = this.programCounter + 1;
    this.overflowFlag = FALSE;
    return null;
  }

  /**
   * Clears the Zero Flag (Z) in SREG (Status register).
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.CLZ insn) {
    this.programCounter = this.programCounter + 1;
    this.zeroFlag = FALSE;
    return null;
  }

  /**
   * This instruction performs a One’s Complement of register rd.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.COM insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    // Perform operation
    Byte r = rd.inv();
    // Update register file
    this.data.write(insn.Rd, r);
    // Set Flags
    Bit r7 = r.get(7);
    //
    this.carryFlag = TRUE;
    this.zeroFlag = r.isZero();
    this.negativeFlag = r7;
    this.overflowFlag = FALSE;
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    //
    return null;
  }

  /**
   * This instruction performs a compare between two registers rd and rr. None of
   * the registers are changed. All conditional branches can be used after this
   * instruction.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.CP insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Byte rr = this.data.read(insn.Rr);
    // Perform operation
    Byte r = rd.sub(rr);
    // Set Flags
    Bit rd7 = rd.get(7);
    Bit rr7 = rr.get(7);
    Bit r7 = r.get(7);
    //
    this.carryFlag = or(and(not(rd7), rr7), and(rr7, r7), and(r7, not(rd7)));
    this.zeroFlag = r.isZero();
    this.negativeFlag = r7;
    this.overflowFlag = or(and(rd7, not(rr7), not(r7)), and(not(rd7), rr7, r7));
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    Bit rd3 = rd.get(3);
    Bit rr3 = rr.get(3);
    Bit r3 = r.get(3);
    //
    this.halfCarryFlag = or(and(not(rd3), rr3), and(rr3, r3), and(r3, not(rd3)));
    //
    return null;
  }

  /**
   * This instruction performs a compare between two registers rd and rr and also
   * takes into account the previous carry. None of the registers are changed. All
   * conditional branches can be used after this instruction.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.CPC insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Byte rr = this.data.read(insn.Rr);
    Byte cf = Byte.from(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, this.carryFlag);
    // Perform operation
    Byte r = rd.sub(rr).sub(cf);
    // Set Flags
    Bit rd7 = rd.get(7);
    Bit rr7 = rr.get(7);
    Bit r7 = r.get(7);
    //
    this.carryFlag = or(and(not(rd7), rr7), and(rr7, r7), and(r7, not(rd7)));
    this.zeroFlag = and(r.isZero(), this.zeroFlag);
    this.negativeFlag = r7;
    this.overflowFlag = or(and(rd7, not(rr7), not(r7)), and(not(rd7), rr7, r7));
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    Bit rd3 = rd.get(3);
    Bit rr3 = rr.get(3);
    Bit r3 = r.get(3);
    //
    this.halfCarryFlag = or(and(not(rd3), rr3), and(rr3, r3), and(r3, not(rd3)));
    //
    return null;
  }

  /**
   * This instruction performs a compare between register rd and a constant. The
   * register is not changed. All conditional branches can be used after this
   * instruction.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.CPI insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Byte k = Byte.from((byte) insn.K);
    // Perform operation
    Byte r = rd.sub(k);
    // Set Flags
    Bit rd7 = rd.get(7);
    Bit k7 = k.get(7);
    Bit r7 = r.get(7);
    //
    this.carryFlag = or(and(not(rd7), k7), and(k7, r7), and(r7, not(rd7)));
    this.zeroFlag = r.isZero();
    this.negativeFlag = r7;
    this.overflowFlag = or(and(rd7, not(k7), not(r7)), and(not(rd7), k7, r7));
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    Bit rd3 = rd.get(3);
    Bit k3 = k.get(3);
    Bit r3 = r.get(3);
    //
    this.halfCarryFlag = or(and(not(rd3), k3), and(k3, r3), and(r3, not(rd3)));
    //
    return null;
  }

  /**
   * This instruction performs a compare between two registers rd and rr, and
   * skips the next instruction if rd = rr.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.CPSE insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Byte rr = this.data.read(insn.Rr);
    Bit b = rd.eq(rr);
    AbstractAvr fork = null;
    if (b == UNKNOWN) {
      fork = clone();
      b = TRUE;
    }
    if (b == TRUE) {
      AvrInstruction following = decode(this.programCounter);
      this.programCounter = (this.programCounter + following.getWidth());
    }
    //
    return fork;
  }

  /**
   * Subtracts one from the contents of register rd and places the result in the
   * destination register rd. The C Flag in SREG is not affected by the operation,
   * thus allowing the DEC instruction to be used on a loop counter in
   * multiple-precision computations. When operating on unsigned values, only BREQ
   * and BRNE branches can be expected to perform consistently. When operating on
   * two’s complement values, all signed branches are available.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.DEC insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    // Perform operation
    Byte r = rd.sub((byte) 1);
    // Update register file
    this.data.write(insn.Rd, r);
    // Set Flags
    Bit r7 = r.get(7);
    //
    this.zeroFlag = r.isZero();
    this.negativeFlag = r7;
    this.overflowFlag = rd.isLeast();
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    //
    return null;
  }

  /**
   * Performs the logical EOR between the contents of register rd and register rr
   * and places the result in the destination register rd.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.EOR insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Byte rr = this.data.read(insn.Rr);
    // Perform operation
    Byte r = rd.xor(rr);
    // Update register file
    this.data.write(insn.Rd, r);
    // Set Flags
    Bit r7 = r.get(7);
    //
    this.zeroFlag = r.isZero();
    this.negativeFlag = r7;
    this.overflowFlag = FALSE;
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    //
    return null;
  }

  /**
   * Calls to a subroutine within the entire 4M (words) Program memory. The return
   * address (to the instruction after the CALL) will be stored onto the Stack.
   * See also RCALL. The Stack Pointer uses a post-decrement scheme during CALL.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ICALL insn) {
    this.programCounter = this.programCounter + 1;
    pushWord(Word.from(this.programCounter));
    // read the Z register
    Word z = readWord(AVR.R30_ZL_ADDRESS);
    // Sanity check destination
    checkAddressKnown(z);
    // Perform operation
    this.programCounter = z.toInt();
    return null;
  }

  /**
   * Indirect jump to the address pointed to by the Z (16 bits) Pointer register
   * in the register File. The Z- pointer register is 16 bits wide and allows jump
   * within the lowest 64K words (128KB) section of Program memory.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.IJMP insn) {
    // read the Z register
    Word z = readWord(AVR.R30_ZL_ADDRESS);
    // Sanity check destination
    checkAddressKnown(z);
    // Perform operation
    this.programCounter = z.toInt();
    return null;
  }

  /**
   * Loads data from the I/O Space (Ports, Timers, Configuration registers, etc.)
   * into register rd in the register File.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.IN insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.A + 32);
    this.data.write(insn.Rd, rd);
    return null;
  }

  /**
   * Adds one -1- to the contents of register rd and places the result in the
   * destination register rd. The C Flag in SREG is not affected by the operation,
   * thus allowing the INC instruction to be used on a loop counter in
   * multiple-precision computations. When operating on unsigned numbers, only
   * BREQ and BRNE branches can be expected to perform consistently. When
   * operating on two’s complement values, all signed branches are available.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.INC insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    // Perform operation
    Byte r = rd.inc();
    // Update register file
    this.data.write(insn.Rd, r);
    // Set Flags
    Bit r0 = r.get(0);
    Bit r1 = r.get(1);
    Bit r2 = r.get(2);
    Bit r3 = r.get(3);
    Bit r4 = r.get(4);
    Bit r5 = r.get(5);
    Bit r6 = r.get(6);
    Bit r7 = r.get(7);
    //
    this.zeroFlag = r.isZero();
    this.negativeFlag = r7;
    this.overflowFlag = and(r7, not(r6), not(r5), not(r4), not(r3), not(r2), not(r1), not(r0));
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    //
    return null;
  }

  /**
   * Jump to an address within the entire 4M (words) Program memory. See also
   * rJMP. This instruction is not available in all devices. refer to the device
   * specific instruction set summary.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.JMP insn) {
    this.programCounter = insn.k;
    return null;
  }

  /**
   * Loads one byte indirect from the data space to a register. For parts with
   * SrAM, the data space consists of the register File, I/O memory, and internal
   * SrAM (and external SrAM if applicable). For parts without SrAM, the data
   * space consists of the register File only. In some parts the Flash Memory has
   * been mapped to the data space and can be read using this command. The EEPrOM
   * has a separate address space.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.LD_X insn) {
    this.programCounter = this.programCounter + 1;
    // Load X register
    Word x = readWord(AVR.R26_XL_ADDRESS);
    // Sanity check address
    checkAddressKnown(x);
    // Perform operation
    Byte rd = this.data.read(x.toInt());
    this.data.write(insn.Rd, rd);
    return null;
  }

  /**
   * Same as LD_X with post increment.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.LD_X_INC insn) {
    this.programCounter = this.programCounter + 1;
    // Load X register
    Word x = readWord(AVR.R26_XL_ADDRESS);
    // Sanity check address
    checkAddressKnown(x);
    // Perform operation
    Byte rd = this.data.read(x.toInt());
    this.data.write(insn.Rd, rd);
    // Post increment
    writeWord(AVR.R26_XL_ADDRESS, x.inc());
    //
    return null;
  }

  /**
   * Same as LD_X with pre decrement.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.LD_X_DEC insn) {
    this.programCounter = this.programCounter + 1;
    // Load X register
    Word x = readWord(AVR.R26_XL_ADDRESS);
    // Pre decrement
    x = x.dec();
    // Sanity check address
    checkAddressKnown(x);
    //
    writeWord(AVR.R26_XL_ADDRESS, x);
    // Perform operation
    Byte rd = this.data.read(x.toInt());
    this.data.write(insn.Rd, rd);
    return null;
  }

  /**
   * Loads one byte indirect with or without displacement from the data space to a
   * register. For parts with SrAM, the data space consists of the register File,
   * I/O memory, and internal SrAM (and external SrAM if applicable). For parts
   * without SrAM, the data space consists of the register File only. In some
   * parts the Flash Memory has been mapped to the data space and can be read
   * using this command. The EEPrOM has a separate address space.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.LD_Y insn) {
    this.programCounter = this.programCounter + 1;
    // Load Y register
    Word y = readWord(AVR.R28_YL_ADDRESS);
    // Sanity check address
    checkAddressKnown(y);
    // Perform operation
    Byte rd = this.data.read(y.toInt());
    this.data.write(insn.Rd, rd);
    //
    return null;
  }

  /**
   * Same as LD_Y with post increment.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.LD_Y_INC insn) {
    this.programCounter = this.programCounter + 1;
    // Load Y register
    Word y = readWord(AVR.R28_YL_ADDRESS);
    // Sanity check address
    checkAddressKnown(y);
    // Perform operation
    Byte rd = this.data.read(y.toInt());
    this.data.write(insn.Rd, rd);
    // Post increment
    writeWord(AVR.R28_YL_ADDRESS, y.inc());
    return null;
  }

  /**
   * Same as LD_Y with pre decement.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.LD_Y_DEC insn) {
    this.programCounter = this.programCounter + 1;
    // Load Y register
    Word y = readWord(AVR.R28_YL_ADDRESS);
    // Pre decrement
    y = y.dec();
    // Sanity check address
    checkAddressKnown(y);
    // Perform operation
    writeWord(AVR.R28_YL_ADDRESS, y);
    // Perform operation
    Byte rd = this.data.read(y.toInt());
    this.data.write(insn.Rd, rd);
    //
    return null;
  }

  /**
   * Same as <code>LD_Y</code> but with displacement.
   *
   * @param insn insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.LDD_Y_Q insn) {
    this.programCounter = this.programCounter + 1;
    // Load Y register
    Word y = readWord(AVR.R28_YL_ADDRESS);
    // Sanity check address
    checkAddressKnown(y);
    // Perform operation
    Byte rd = this.data.read(y.toInt() + insn.q);
    this.data.write(insn.Rd, rd);
    return null;
  }

  /**
   * Loads one byte indirect with or without displacement from the data space to a
   * register. For parts with SrAM, the data space consists of the register File,
   * I/O memory, and internal SrAM (and external SrAM if applicable). For parts
   * without SrAM, the data space consists of the register File only. In some
   * parts the Flash Memory has been mapped to the data space and can be read
   * using this command. The EEPrOM has a separate address space. The data
   * location is pointed to by the Z (16 bits) Pointer register in the register
   * File.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.LD_Z insn) {
    this.programCounter = this.programCounter + 1;
    // Load Z register
    Word z = readWord(AVR.R30_ZL_ADDRESS);
    // Sanity check address
    checkAddressKnown(z);
    // Perform operation
    Byte rd = this.data.read(z.toInt());
    this.data.write(insn.Rd, rd);
    return null;
  }

  /**
   * Same as LD_Z with post-increment.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.LD_Z_INC insn) {
    this.programCounter = this.programCounter + 1;
    // Load Z register
    Word z = readWord(AVR.R30_ZL_ADDRESS);
    // Sanity check address
    checkAddressKnown(z);
    // Perform operation
    Byte rd = this.data.read(z.toInt());
    this.data.write(insn.Rd, rd);
    // Post increment
    writeWord(AVR.R30_ZL_ADDRESS, z.inc());
    return null;
  }

  /**
   * Same as LD_Z with pre-decrement.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.LD_Z_DEC insn) {
    this.programCounter = this.programCounter + 1;
    // Load Z register
    Word z = readWord(AVR.R30_ZL_ADDRESS);
    // Sanity check address
    checkAddressKnown(z);
    // Pre decrement
    z = z.dec();
    writeWord(AVR.R30_ZL_ADDRESS, z);
    // Perform operation
    Byte rd = this.data.read(z.toInt());
    this.data.write(insn.Rd, rd);
    //
    return null;
  }

  /**
   * Same as <code>LD_Z</code> but with displacement.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.LDD_Z_Q insn) {
    this.programCounter = this.programCounter + 1;
    // Load Z register
    Word z = readWord(AVR.R30_ZL_ADDRESS);
    // Sanity check address
    checkAddressKnown(z);
    // Perform operation
    Byte rd = this.data.read(z.toInt() + insn.q);
    this.data.write(insn.Rd, rd);
    return null;
  }

  /**
   * Loads an 8-bit constant directly to register 16 to 31.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.LDI insn) {
    this.programCounter = this.programCounter + 1;
    this.data.write(insn.Rd, Byte.from((byte) insn.K));
    return null;
  }

  /**
   * Loads one byte from the data space to a register. For parts with SrAM, the
   * data space consists of the register File, I/O memory, and internal SrAM (and
   * external SrAM if applicable). For parts without SrAM, the data space consists
   * of the register file only. In some parts the Flash memory has been mapped to
   * the data space and can be read using this command. The EEPrOM has a separate
   * address space. A 7-bit address must be supplied.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.LDS insn) {
    this.programCounter = (this.programCounter + 2);
    Byte rd = this.data.read(insn.k);
    this.data.write(insn.Rd, rd);
    //
    return null;
  }

  /**
   * Load from Program (i.e. FLASH) Memory.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.LPM_Z insn) {
    this.programCounter = this.programCounter + 1;
    // Load Z register
    Word z = readWord(AVR.R30_ZL_ADDRESS);
    // Sanity check address
    checkAddressKnown(z);
    // Perform operation
    byte rd = this.code.read(z.toInt());
    this.data.write(insn.Rd, Byte.from(rd));
    return null;
  }

  /**
   * Same as <code>LD_PM</code> but with post increment.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.LPM_Z_INC insn) {
    this.programCounter = this.programCounter + 1;
    // Load Z register
    Word z = readWord(AVR.R30_ZL_ADDRESS);
    // Sanity check address
    checkAddressKnown(z);
    // Perform operation
    byte rd = this.code.read(z.toInt());
    this.data.write(insn.Rd, Byte.from(rd));
    // Post increment
    writeWord(AVR.R30_ZL_ADDRESS, z.inc());
    return null;
  }

  /**
   * Shifts all bits in rd one place to the right. Bit 7 is cleared. Bit 0 is
   * loaded into the C Flag of the SREG. This operation effectively divides an
   * unsigned value by two. The C Flag can be used to round the result.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.LSR insn) {
    this.programCounter = this.programCounter + 1;
    // read register
    Byte rd = this.data.read(insn.Rd);
    // Perform operation
    Byte r = rd.ushr(1);
    // Update register file
    this.data.write(insn.Rd, r);
    // Set flags
    Bit rd0 = rd.get(0);
    //
    this.carryFlag = rd0;
    this.zeroFlag = r.isZero();
    this.negativeFlag = FALSE;
    this.overflowFlag = xor(this.negativeFlag, this.carryFlag);
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    //
    return null;
  }

  /**
   * This instruction makes a copy of one register into another. The source
   * register rr is left unchanged, while the destination register rd is loaded
   * with a copy of rr.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.MOV insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rr);
    this.data.write(insn.Rd, rd);
    //
    return null;
  }

  /**
   * This instruction makes a copy of one register pair into another register
   * pair. The source register pair rr +1:rr is left unchanged, while the
   * destination register pair rd+1:rd is loaded with a copy of rr + 1:rr.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.MOVW insn) {
    this.programCounter = this.programCounter + 1;
    Word word = readWord(insn.Rr);
    writeWord(insn.Rd, word);
    //
    return null;
  }

  /**
   * replaces the contents of register rd with its two’s complement; the value $80
   * is left unchanged.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.NEG insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Byte r = rd.neg();
    this.data.write(insn.Rd, r);
    // Set flags
    Bit r7 = r.get(7);
    //
    this.carryFlag = r.isNotZero();
    this.zeroFlag = r.isZero();
    this.negativeFlag = r7;
    this.overflowFlag = r.isLeast();
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    Bit rd3 = rd.get(3);
    Bit r3 = r.get(3);
    //
    this.halfCarryFlag = or(r3, not(rd3));
    //
    return null;
  }

  /**
   * This instruction performs a single cycle No Operation.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.NOP insn) {
    this.programCounter = this.programCounter + 1;
    return null;
  }

  /**
   * Performs the logical OR between the contents of register rd and register rr,
   * and places the result in the destination register rd.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.OR insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Byte rr = this.data.read(insn.Rr);
    // Perform operation
    Byte r = rd.or(rr);
    // Update register file
    this.data.write(insn.Rd, r);
    // Set Flags
    Bit r7 = r.get(7);
    //
    this.zeroFlag = r.isZero();
    this.negativeFlag = r7;
    this.overflowFlag = FALSE;
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    //
    return null;
  }

  /**
   * Performs the logical OR between the contents of register rd and a constant,
   * and places the result in the destination register rd.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ORI insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Byte rr = Byte.from((byte) insn.K);
    // Perform operation
    Byte r = rd.or(rr);
    // Update register file
    this.data.write(insn.Rd, r);
    // Set Flags
    Bit r7 = r.get(7);
    //
    this.zeroFlag = r.isZero();
    this.negativeFlag = r7;
    this.overflowFlag = FALSE;
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    //
    return null;
  }

  /**
   * Stores data from register rr in the register File to I/O Space (Ports,
   * Timers, Configuration registers, etc.).
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.OUT insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rr);
    this.data.write(insn.A + 32, rd);
    //
    return null;
  }

  /**
   * This instruction loads register rd with a byte from the STACK. The Stack
   * Pointer is pre-incremented by 1 before the POP.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.POP insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = popByte();
    this.data.write(insn.Rd, rd);
    //
    return null;
  }

  /**
   * This instruction stores the contents of register rr on the STACK. The Stack
   * Pointer is post-decremented by 1 after the PUSH.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.PUSH insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    pushByte(rd);
    //
    return null;
  }

  /**
   * relative call to an address within PC - 2K + 1 and PC + 2K (words). The
   * return address (the instruction after the RCALL) is stored onto the Stack.
   * See also CALL. For AVR microcontrollers with Program memory not exceeding 4K
   * words (8KB) this instruction can address the entire memory from every address
   * location. The Stack Pointer uses a post-decrement scheme during RCALL.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.RCALL insn) {
    pushWord(Word.from(this.programCounter + 1));
    this.programCounter = this.programCounter + 1 + insn.k;
    //
    return null;
  }

  /**
   * returns from subroutine. The return address is loaded from the STACK. The
   * Stack Pointer uses a pre-increment scheme during rET.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.RET insn) {
    Word address = popWord();
    // Sanity check address
    checkAddressKnown(address);
    // Perform operation
    this.programCounter = address.toInt();
    return null;
  }

  /**
   * relative jump to an address within PC - 2K +1 and PC + 2K (words). For AVR
   * microcontrollers with Program memory not exceeding 4K words (8KB) this
   * instruction can address the entire memory from every address location. See
   * also JMP.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   * @throws AVR.HaltedException If this instruction is a "branch to self".
   */
  private @Nullable AbstractAvr execute(AvrInstruction.RJMP insn) throws AVR.HaltedException {
    int pc = this.programCounter + insn.k + 1;
    if (pc == this.programCounter) {
      Word c = readWord(AVR.R24_ADDRESS);
      // Indicate machine halted
      this.programCounter = -1;
      // Terminate
      throw new AVR.HaltedException(c.toInt());
    }
    this.programCounter = pc;
    return null;
  }

  /**
   * Shifts all bits in rd one place to the right. The C Flag is shifted into bit
   * 7 of rd. Bit 0 is shifted into the C Flag. This operation, combined with ASr,
   * effectively divides multi-byte signed values by two. Combined with LSr it
   * effectively divides multi- byte unsigned values by two. The Carry Flag can be
   * used to round the result
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ROR insn) {
    this.programCounter = this.programCounter + 1;
    // read carry flag
    Byte cf = Byte.from(this.carryFlag, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE);
    // read register
    Byte rd = this.data.read(insn.Rd);
    // Perform operation
    Byte r = cf.or(rd.ushr(1));
    // Update register file
    this.data.write(insn.Rd, r);
    // Update status register
    Bit rd0 = rd.get(0);
    Bit r7 = r.get(7);
    //
    this.carryFlag = rd0;
    this.zeroFlag = r.isZero();
    this.negativeFlag = r7;
    this.overflowFlag = xor(this.negativeFlag, this.carryFlag);
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    //
    return null;
  }

  /**
   * Subtracts two registers and subtracts with the C Flag, and places the result
   * in the destination register rd.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SBC insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Byte rr = this.data.read(insn.Rr);
    Byte cf = Byte.from(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, this.carryFlag);
    // Perform operation
    Byte r = rd.sub(rr).sub(cf);
    // Update register file
    this.data.write(insn.Rd, r);
    // Set Flags
    Bit rd7 = rd.get(7);
    Bit rr7 = rr.get(7);
    Bit r7 = r.get(7);
    //
    this.carryFlag = or(and(not(rd7), rr7), and(rr7, r7), and(r7, not(rd7)));
    this.zeroFlag = and(r.isZero(), this.zeroFlag);
    this.negativeFlag = r7;
    this.overflowFlag = or(and(rd7, not(rr7), not(r7)), and(not(rd7), rr7, r7));
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    Bit rd3 = rd.get(3);
    Bit rr3 = rr.get(3);
    Bit r3 = r.get(3);
    //
    this.halfCarryFlag = or(and(not(rd3), rr3), and(rr3, r3), and(r3, not(rd3)));
    //
    return null;
  }

  /**
   * Subtracts a constant from a register and subtracts with the C Flag, and
   * places the result in the destination register rd.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SBCI insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    byte k = (byte) insn.K;
    Byte cf = Byte.from(FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, this.carryFlag);
    // Perform operation
    Byte r = rd.sub(k).sub(cf);
    // Update register file
    this.data.write(insn.Rd, r);
    // Set Flags
    Bit rd7 = rd.get(7);
    Bit k7 = (k & 0b1000_0000) != 0 ? TRUE : FALSE;
    Bit r7 = r.get(7);
    //
    this.carryFlag = or(and(not(rd7), k7), and(k7, r7), and(r7, not(rd7)));
    this.zeroFlag = and(r.isZero(), this.zeroFlag);
    this.negativeFlag = r7;
    this.overflowFlag = or(and(rd7, not(k7), not(r7)), and(not(rd7), k7, r7));
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    Bit rd3 = rd.get(3);
    Bit k3 = (k & 0b1000) != 0 ? TRUE : FALSE;
    Bit r3 = r.get(3);
    //
    this.halfCarryFlag = or(and(not(rd3), k3), and(k3, r3), and(r3, not(rd3)));
    //
    return null;
  }

  /**
   * Sets a specified bit in an I/O register. This instruction operates on the
   * lower 32 I/O registers – addresses 0-31.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SBI insn) {
    this.programCounter = this.programCounter + 1;
    Byte a = this.data.read(insn.A + 32);
    this.data.write(insn.A + 32, a.set(insn.b));
    //
    return null;
  }

  /**
   * This instruction tests a single bit in an I/O register and skips the next
   * instruction if the bit is cleared. This instruction operates on the lower 32
   * I/O registers – addresses 0-31.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SBIC insn) {
    this.programCounter = this.programCounter + 1;
    Byte io = this.data.read(insn.A + 32);
    Bit iob = io.get(insn.b);
    AbstractAvr fork = null;
    if (iob == UNKNOWN) {
      fork = clone();
      iob = FALSE;
    }
    AvrInstruction following = decode(this.programCounter);
    int pc = this.programCounter + following.getWidth();
    if (iob == FALSE) {
      this.programCounter = pc;
    }
    //
    return fork;
  }

  /**
   * This instruction tests a single bit in an I/O register and skips the next
   * instruction if the bit is set. This instruction operates on the lower 32 I/O
   * registers – addresses 0-31.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SBIS insn) {
    this.programCounter = this.programCounter + 1;
    Byte io = this.data.read(insn.A + 32);
    Bit iob = io.get(insn.b);
    AbstractAvr fork = null;
    if (iob == UNKNOWN) {
      fork = clone();
      iob = TRUE;
    }
    AvrInstruction following = decode(this.programCounter);
    int pc = this.programCounter + following.getWidth();
    if (iob == TRUE) {
      this.programCounter = pc;
    }
    //
    return fork;
  }

  /**
   * Subtracts an immediate value (0-63) from a register pair and places the
   * result in the register pair. This instruction operates on the upper four
   * register pairs, and is well suited for operations on the Pointer registers.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SBIW insn) {
    this.programCounter = this.programCounter + 1;
    Word rd = readWord(insn.Rd);
    // Perform operation
    Word r = rd.sub((byte) insn.K);
    // Update register file
    writeWord(insn.Rd, r);
    // Set Flags
    Bit rdh7 = rd.get(15);
    Bit r15 = r.get(15);
    //
    this.carryFlag = and(r15, not(rdh7));
    this.zeroFlag = r.isZero();
    this.negativeFlag = r15;
    this.overflowFlag = this.carryFlag;
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    //
    return null;
  }

  /**
   * Sets specified bits in register rd. Performs the logical ORI between the
   * contents of register rd and a constant mask K, and places the result in the
   * destination register rd.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SBR insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Byte rr = Byte.from((byte) insn.K);
    // Perform operation
    Byte r = rd.or(rr);
    // Update register file
    this.data.write(insn.Rd, r);
    // Set Flags
    Bit r7 = r.get(7);
    //
    this.zeroFlag = r.isZero();
    this.negativeFlag = r7;
    this.overflowFlag = FALSE;
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    //
    return null;
  }

  /**
   * This instruction tests a single bit in a register and skips the next
   * instruction if the bit is cleared.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SBRC insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Bit rdb = rd.get(insn.b);
    AbstractAvr fork = null;
    if (rdb == UNKNOWN) {
      fork = clone();
      rdb = FALSE;
    }
    AvrInstruction following = decode(this.programCounter);
    int pc = this.programCounter + following.getWidth();
    if (rdb == FALSE) {
      this.programCounter = pc;
    }
    //
    return fork;
  }

  /**
   * This instruction tests a single bit in a register and skips the next
   * instruction if the bit is set.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SBRS insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Bit rdb = rd.get(insn.b);
    AbstractAvr fork = null;
    if (rdb == UNKNOWN) {
      fork = clone();
      rdb = TRUE;
    }
    AvrInstruction following = decode(this.programCounter);
    int pc = this.programCounter + following.getWidth();
    if (rdb == TRUE) {
      this.programCounter = pc;
    }
    //
    return fork;
  }

  /**
   * Sets the Carry Flag (C) in SREG (Status register).
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SEC insn) {
    this.programCounter = this.programCounter + 1;
    this.carryFlag = TRUE;
    return null;
  }

  /**
   * Sets the Half Carry (H) in SREG (Status register).
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SEH insn) {
    this.programCounter = this.programCounter + 1;
    this.halfCarryFlag = TRUE;
    return null;
  }

  /**
   * Sets the Global Interrupt Flag (I) in SREG (Status register). The instruction
   * following SEI will be executed before any pending interrupts.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SEI insn) {
    this.programCounter = this.programCounter + 1;
    this.interruptFlag = TRUE;
    return null;
  }

  /**
   * Sets the Negative Flag (N) in SREG (Status register).
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SEN insn) {
    this.programCounter = this.programCounter + 1;
    this.negativeFlag = TRUE;
    return null;
  }

  /**
   * Loads $FF directly to register rd.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SER insn) {
    this.programCounter = this.programCounter + 1;
    this.data.write(insn.Rd, Byte.from((byte) 0xFF));
    //
    return null;
  }

  /**
   * Sets the Signed Flag (S) in SREG (Status register).
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SES insn) {
    this.programCounter = this.programCounter + 1;
    this.signFlag = TRUE;
    return null;
  }

  /**
   * Sets the T Flag in SREG (Status register).
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SET insn) {
    this.programCounter = this.programCounter + 1;
    this.bitcopyFlag = TRUE;
    return null;
  }

  /**
   * Sets the Overflow Flag (V) in SREG (Status register).
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SEV insn) {
    this.programCounter = this.programCounter + 1;
    this.overflowFlag = TRUE;
    return null;
  }

  /**
   * Sets the Zero Flag (Z) in SREG (Status register).
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SEZ insn) {
    this.programCounter = this.programCounter + 1;
    this.zeroFlag = TRUE;
    return null;
  }

  /**
   * Stores one byte indirect from a register to data space. For parts with SrAM,
   * the data space consists of the register File, I/O memory, and internal SrAM
   * (and external SrAM if applicable). For parts without SrAM, the data space
   * consists of the register File only. The EEPrOM has a separate address space.
   * The data location is pointed to by the X (16 bits) Pointer register in the
   * register File.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ST_X insn) {
    this.programCounter = this.programCounter + 1;
    // Load X register
    Word x = readWord(AVR.R26_XL_ADDRESS);
    // Sanity check address
    checkAddressKnown(x);
    // Perform operation
    Byte rd = this.data.read(insn.Rd);
    this.data.write(x.toInt(), rd);
    return null;
  }

  /**
   * Same as ST_X with post-increment.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ST_X_INC insn) {
    this.programCounter = this.programCounter + 1;
    // Load X register
    Word x = readWord(AVR.R26_XL_ADDRESS);
    // Sanity check address
    checkAddressKnown(x);
    // Perform operation
    Byte rd = this.data.read(insn.Rd);
    this.data.write(x.toInt(), rd);
    // Post increment
    writeWord(AVR.R26_XL_ADDRESS, x.inc());
    return null;
  }

  /**
   * Same as ST_X with pre-deccrement.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ST_X_DEC insn) {
    this.programCounter = this.programCounter + 1;
    // Load X register
    Word x = readWord(AVR.R26_XL_ADDRESS);
    // Sanity check address
    checkAddressKnown(x);
    // Perform operation
    x = x.dec();
    writeWord(AVR.R26_XL_ADDRESS, x);
    // Perform operation
    Byte rd = this.data.read(insn.Rd);
    this.data.write(x.toInt(), rd);
    return null;
  }

  /**
   * Stores one byte indirect with or without displacement from a register to data
   * space. For parts with SrAM, the data space consists of the register File, I/O
   * memory, and internal SrAM (and external SrAM if applicable). For parts
   * without SrAM, the data space consists of the register File only. The EEPrOM
   * has a separate address space. The data location is pointed to by the Y (16
   * bits) Pointer register in the register File.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ST_Y insn) {
    this.programCounter = this.programCounter + 1;
    // Load Y register
    Word y = readWord(AVR.R28_YL_ADDRESS);
    // Sanity check address
    checkAddressKnown(y);
    // Perform operation
    Byte rd = this.data.read(insn.Rd);
    this.data.write(y.toInt(), rd);
    return null;
  }

  /**
   * Same as ST_Y with post-increment.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ST_Y_INC insn) {
    this.programCounter = this.programCounter + 1;
    // Load Y register
    Word y = readWord(AVR.R28_YL_ADDRESS);
    // Sanity check address
    checkAddressKnown(y);
    // Perform operation
    Byte rd = this.data.read(insn.Rd);
    this.data.write(y.toInt(), rd);
    // Post increment
    writeWord(AVR.R28_YL_ADDRESS, y.inc());
    return null;
  }

  /**
   * Same as ST_Y with pre-decrement.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ST_Y_DEC insn) {
    this.programCounter = this.programCounter + 1;
    // Load Y register
    Word y = readWord(AVR.R28_YL_ADDRESS);
    // Sanity check address
    checkAddressKnown(y);
    // Pre decrement
    y = y.dec();
    writeWord(AVR.R28_YL_ADDRESS, y);
    // Perform operation
    Byte rd = this.data.read(insn.Rd);
    this.data.write(y.toInt(), rd);
    return null;
  }

  /**
   * Same as <code>STD_Y</code> but with displacement.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.STD_Y_Q insn) {
    this.programCounter = this.programCounter + 1;
    // Load Y register
    Word y = readWord(AVR.R28_YL_ADDRESS);
    // Sanity check address
    checkAddressKnown(y);
    // Perform operation
    Byte rd = this.data.read(insn.Rd);
    this.data.write(y.toInt() + insn.q, rd);
    return null;
  }

  /**
   * Stores one byte indirect with or without displacement from a register to data
   * space. For parts with SrAM, the data space consists of the register File, I/O
   * memory, and internal SrAM (and external SrAM if applicable). For parts
   * without SrAM, the data space consists of the register File only. The EEPrOM
   * has a separate address space. The data location is pointed to by the Z (16
   * bits) Pointer register in the register File.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ST_Z insn) {
    this.programCounter = this.programCounter + 1;
    // Load Z register
    Word z = readWord(AVR.R30_ZL_ADDRESS);
    // Sanity check address
    checkAddressKnown(z);
    // Perform operation
    Byte rd = this.data.read(insn.Rd);
    this.data.write(z.toInt(), rd);
    return null;
  }

  /**
   * Same as ST_Z with post-increment.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ST_Z_INC insn) {
    this.programCounter = this.programCounter + 1;
    // Load Z register
    Word z = readWord(AVR.R30_ZL_ADDRESS);
    // Sanity check address
    checkAddressKnown(z);
    // Perform operation
    Byte rd = this.data.read(insn.Rd);
    this.data.write(z.toInt(), rd);
    // Post increment
    writeWord(AVR.R30_ZL_ADDRESS, z.inc());
    return null;
  }

  /**
   * Same as ST_Z with pre-decrement.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.ST_Z_DEC insn) {
    this.programCounter = this.programCounter + 1;
    // Load Z register
    Word z = readWord(AVR.R30_ZL_ADDRESS);
    // Sanity check address
    checkAddressKnown(z);
    // Pre decrement
    z = z.dec();
    writeWord(AVR.R30_ZL_ADDRESS, z);
    // Perform operation
    Byte rd = this.data.read(insn.Rd);
    this.data.write(z.toInt(), rd);
    return null;
  }

  /**
   * Same as <code>STD_Z</code> but with displacement.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.STD_Z_Q insn) {
    this.programCounter = this.programCounter + 1;
    // Load Z register
    Word z = readWord(AVR.R30_ZL_ADDRESS);
    // Sanity check address
    checkAddressKnown(z);
    // Perform operation
    Byte rd = this.data.read(insn.Rd);
    this.data.write(z.toInt() + insn.q, rd);
    return null;
  }

  /**
   * Stores one byte from a register to the data space. For parts with SrAM, the
   * data space consists of the register File, I/O memory, and internal SrAM (and
   * external SrAM if applicable). For parts without SrAM, the data space consists
   * of the register File only. In some parts the Flash memory has been mapped to
   * the data space and can be written using this command. The EEPrOM has a
   * separate address space. A 7-bit address must be supplied.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.STS_DATA_WIDE insn) {
    this.programCounter = (this.programCounter + 2);
    Byte rd = this.data.read(insn.Rd);
    this.data.write(insn.k, rd);
    return null;
  }

  /**
   * Subtracts two registers and places the result in the destination register rd.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SUB insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Byte rr = this.data.read(insn.Rr);
    // Perform operation
    Byte r = rd.sub(rr);
    // Update register file
    this.data.write(insn.Rd, r);
    // Set Flags
    Bit rd7 = rd.get(7);
    Bit rr7 = rr.get(7);
    Bit r7 = r.get(7);
    //
    this.carryFlag = or(and(not(rd7), rr7), and(rr7, r7), and(r7, not(rd7)));
    this.zeroFlag = and(r.isZero(), this.zeroFlag);
    this.negativeFlag = r7;
    this.overflowFlag = or(and(rd7, not(rr7), not(r7)), and(not(rd7), rr7, r7));
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    Bit rd3 = rd.get(3);
    Bit rr3 = rr.get(3);
    Bit r3 = r.get(3);
    //
    this.halfCarryFlag = or(and(not(rd3), rr3), and(rr3, r3), and(r3, not(rd3)));
    //
    return null;
  }

  /**
   * Subtracts a register and a constant, and places the result in the destination
   * register rd. This instruction is working on register r16 to r31 and is very
   * well suited for operations on the X, Y, and Z-pointers.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SUBI insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Byte k = Byte.from((byte) insn.K);
    // Perform operation
    Byte r = rd.sub(k);
    // Update register file
    this.data.write(insn.Rd, r);
    // Set Flags
    Bit rd7 = rd.get(7);
    Bit k7 = k.get(7);
    Bit r7 = r.get(7);
    //
    this.carryFlag = or(and(not(rd7), k7), and(k7, r7), and(r7, not(rd7)));
    this.zeroFlag = r.isZero();
    this.negativeFlag = r7;
    this.overflowFlag = or(and(rd7, not(k7), not(r7)), and(not(rd7), k7, r7));
    this.signFlag = xor(this.negativeFlag, this.overflowFlag);
    Bit rd3 = rd.get(3);
    Bit k3 = k.get(3);
    Bit r3 = r.get(3);
    //
    this.halfCarryFlag = or(and(not(rd3), k3), and(k3, r3), and(r3, not(rd3)));
    //
    return null;
  }

  /**
   * Swaps high and low nibbles in a register.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.SWAP insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    // Write back
    this.data.write(insn.Rd, rd.swap());
    //
    return null;
  }

  /**
   * Exchanges one byte indirect between register and data space. The data
   * location is pointed to by the Z (16 bits) Pointer register in the register
   * File. Memory access is limited to the current data segment of 64KB. To access
   * another data segment in devices with more than 64KB data space, the rAMPZ in
   * register in the I/O area has to be changed. The Z-pointer register is left
   * unchanged by the operation.
   *
   * @param insn Instruction being executed
   * @return Forked AVR state or <code>null</code> (if no fork).
   */
  private @Nullable AbstractAvr execute(AvrInstruction.XCH insn) {
    this.programCounter = this.programCounter + 1;
    Byte rd = this.data.read(insn.Rd);
    Word z = readWord(AVR.R30_ZL_ADDRESS);
    // Sanity check address
    checkAddressKnown(z);
    // Perform operation
    this.data.write(insn.Rd, this.data.read(z.toInt()));
    this.data.write(z.toInt(), rd);
    //
    return null;
  }

  /**
   * Set a specific bit in the status register (SREG). Since this register is
   * represented using individual bits, we actually have to do some work here.
   *
   * @param bit   Bit index (between <code>0</code> and <code>7</code> inclusive).
   * @param value Bit value to set.
   */
  private void setStatusBit(int bit, Bit value) {
    switch (bit) {
      case 0:
        this.carryFlag = value;
        break;
      case 1:
        this.zeroFlag = value;
        break;
      case 2:
        this.negativeFlag = value;
        break;
      case 3:
        this.overflowFlag = value;
        break;
      case 4:
        this.signFlag = value;
        break;
      case 5:
        this.halfCarryFlag = value;
        break;
      case 6:
        this.bitcopyFlag = value;
        break;
      case 7:
        this.interruptFlag = value;
        break;
      default:
        throw new IllegalArgumentException();
    }
  }

  /**
   * Get a specific bit from the status register (SREG). Since this register is
   * represented using individual bits, we actually have to do some work here.
   *
   * @param bit Bit index (between <code>0</code> and <code>7</code> inclusive).
   * @return Bit read from status register.
   */
  private Bit getStatusBit(int bit) {
    switch (bit) {
      case 0:
        return this.carryFlag;
      case 1:
        return this.zeroFlag;
      case 2:
        return this.negativeFlag;
      case 3:
        return this.overflowFlag;
      case 4:
        return this.signFlag;
      case 5:
        return this.halfCarryFlag;
      case 6:
        return this.bitcopyFlag;
      case 7:
        return this.interruptFlag;
      default:
        throw new IllegalArgumentException();
    }
  }

  /**
   * Push a byte onto the stack. This will first load the <code>SP</code> from
   * memory and then write the given byte to that location. For this to succeed,
   * the location must be <i>known</i>.
   *
   * @param bite Byte value to be pushed.
   */
  private void pushByte(Byte bite) {
    // Construct SP contents
    Word sp = readWord(AVR.SPL_ADDRESS);
    // Sanity check address
    checkAddressKnown(sp);
    // Write data
    this.data.write(sp.toInt(), bite);
    // Post-decrement stack pointer
    sp = sp.dec();
    writeWord(AVR.SPL_ADDRESS, sp);
  }

  /**
   * Pop a byte from the stack. This will first load the <code>SP</code> from
   * memory and then read the given byte from that location. For this to succeed,
   * the location must be <i>known</i>.
   *
   * @return Byte read from stack
   */
  private Byte popByte() {
    // Construct SP contents
    Word sp = readWord(AVR.SPL_ADDRESS);
    // Sanity check address
    checkAddressKnown(sp);
    // Pre-increment stack pointer
    sp = sp.inc();
    writeWord(AVR.SPL_ADDRESS, sp);
    // read data
    return this.data.read(sp.toInt());
  }

  /**
   * Push a 16bit word onto the stack. This will first load the <code>SP</code>
   * from memory and then write the given word over that and the following
   * location. For this to succeed, the location must be <i>known</i>.
   *
   * @param word Value to be pushed.
   */
  private void pushWord(Word word) {
    // Construct SP contents
    Word sp = readWord(AVR.SPL_ADDRESS);
    // Sanity check address
    checkAddressKnown(sp);
    // Write data
    sp = sp.dec();
    writeWord(sp.toInt(), word);
    // Post-decrement stack pointer
    sp = sp.dec();
    writeWord(AVR.SPL_ADDRESS, sp);
  }

  /**
   * Pop a 16bit word from the stack. This will first load the <code>SP</code>
   * from memory and then read from that and the following location. For this to
   * succeed, the location must be <i>known</i>.
   *
   * @return The word read from the stack.
   */
  private Word popWord() {
    // Construct SP contents
    Word sp = readWord(AVR.SPL_ADDRESS);
    // Sanity check address
    checkAddressKnown(sp);
    // Pre-increment stack pointer
    sp = sp.add((byte) 2);
    writeWord(AVR.SPL_ADDRESS, sp);
    // read data
    return readWord(sp.toInt() - 1);
  }

  /**
   * read a 16-bit word from a given address in this machine. Since the AVR is
   * little endian, the low byte is stored at the address and the high byte is
   * stored at the following location.
   *
   * @param address Address to read from.
   * @return Word read from given address
   */
  public Word readWord(int address) {
    Byte msb = this.data.read(address + 1);
    Byte lsb = this.data.read(address);
    return Word.from(msb, lsb);
  }

  /**
   * Write a 16-bit word to a given address in this machine. Since the AVR is
   * little endian, the low byte is stored at the address and the high byte is
   * stored at the following location.
   *
   * @param address Address where to write word.
   * @param word    Word to be written.
   */
  public void writeWord(int address, Word word) {
    this.data.write(address, word.getLow());
    this.data.write(address + 1, word.getHigh());
  }

  /**
   * Check a given 16bit address has a <i>known</i> value and, if not, report a
   * suitable error. This is necessary because some operations cannot proceed
   * without a known address.
   *
   * @param address The address to check.
   */
  private void checkAddressKnown(Word address) {
    if (address.isUnknown()) {
      String pcHex = Integer.toHexString(this.programCounter);
      String unknownMessage = "Unknown indirect address (PC=0x"; //$NON-NLS-1$
      throw new IllegalArgumentException(unknownMessage + pcHex + RBRACE);
    }
  }

  /**
   * Logical AND of one or more bits. Since each bit may be unknown, the outcome
   * may be unknown. However, if one bit is <code>FALSE</code> then the result
   * must be <code>FALSE</code>. Likewise, if all bits are <code>TRUE</code> then
   * the result is <code>TRUE</code>.
   *
   * @param bits Array of one or more bits.
   * @return Logical AND of bits.
   */
  private static Bit and(Bit... bits) {
    boolean oneUnknown = false;
    for (int i = 0; i != bits.length; ++i) {
      Bit ith = bits[i];
      if (ith == FALSE) {
        return FALSE;
      } else if (ith == UNKNOWN) {
        oneUnknown = true;
      }
    }
    if (oneUnknown) {
      return UNKNOWN;
    }
    return TRUE;
  }

  /**
   * Logical OR of one or more bits. Since each bit may be unknown, the outcome
   * may be unknown. However, if one bit is <code>TRUE</code> then the result must
   * be <code>TRUE</code>. Likewise, if all bits are <code>FALSE</code> then the
   * result is <code>FALSE</code>.
   *
   * @param bits Array of one or more bits
   * @return Logical OR of bits.
   */
  private static Bit or(Bit... bits) {
    boolean oneUnknown = false;
    for (int i = 0; i != bits.length; ++i) {
      Bit ith = bits[i];
      if (ith == TRUE) {
        return TRUE;
      } else if (ith == UNKNOWN) {
        oneUnknown = true;
      }
    }
    if (oneUnknown) {
      return UNKNOWN;
    }
    return FALSE;
  }

  /**
   * Logical XOR of a single bit. Since the argument bits may be unknown, the
   * result may be unknown. However, if both bits have concrete values then the
   * result will be concrete (though that is the only circumstance).
   *
   * @param lhs Left bit.
   * @param rhs right bit.
   * @return XOR of bits.
   */
  private static Bit xor(Bit lhs, Bit rhs) {
    if (lhs == UNKNOWN || rhs == UNKNOWN) {
      return UNKNOWN;
    }
    if (lhs == rhs) {
      return FALSE;
    }
    return TRUE;
  }

  /**
   * Logical NOT of a single bit. Since the bit may be unknown, the result may be
   * unknown. But, if the bit has a known value then we can determine a concrete
   * result.
   *
   * @param b Bit to be logically negated.
   * @return False if True (and vice versa), otherwise unknown.
   */
  private static Bit not(Bit b) {
    if (b == UNKNOWN) {
      return UNKNOWN;
    }
    if (b == FALSE) {
      return TRUE;
    }
    return FALSE;
  }

  /**
   * Left angle brace.
   */
  private static final String LANGLE = "<"; //$NON-NLS-1$
  /**
   * Semicolon.
   */
  private static final String SEMICOLON = ";"; //$NON-NLS-1$
  /**
   * Colon.
   */
  private static final String COLON = ":"; //$NON-NLS-1$
  /**
   * Vertical bar.
   */
  private static final String BAR = "|"; //$NON-NLS-1$
  /**
   * Right-angle brace.
   */
  private static final String RANGLE = ">"; //$NON-NLS-1$
  /**
   * Right brace.
   */
  private static final String RBRACE = ")"; //$NON-NLS-1$
  /**
   * Format specifier.
   */
  private static final String FMT_32 = "%04X"; //$NON-NLS-1$
  /**
   * Debug beginning.
   */
  private static final String DBG_BEG = "PC=0x"; //$NON-NLS-1$
  /**
   * Debug end.
   */
  private static final String DBG_MID = ", SREG="; //$NON-NLS-1$
}
