package app.smd;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

public class StateMachine {

    private static class State {
        String pattern;
        int[] transfer;
        int speed;

        State() {
            pattern = "0000000000000000";
            transfer = new int[NUM_TX];
            transfer[TX_LEFT] = OP_INHERIT;
            transfer[TX_RIGHT] = OP_INHERIT;
            transfer[TX_UP] = OP_INHERIT;
            transfer[TX_DOWN] = OP_INHERIT;
            transfer[TX_CLICK] = OP_INHERIT;
            transfer[TX_AUTO] = OP_INHERIT;
            speed = 1;
        }

        State(State s) {
            pattern = s.pattern;
            transfer = s.transfer.clone();
            speed = s.speed;
        }

        private BigInteger readTx(BigInteger td, int tx) {
            int op = td.byteValue() & 0x7f;
            if(op >= 0x80 - NUM_OP) op -= 0x80;
            transfer[tx] = op;
            return td.shiftRight(7);
        }

        State(String transferData, String pattern) {
            this.pattern = pattern;
            transfer = new int[NUM_TX];
            BigInteger td = new BigInteger(transferData, 16);
            speed = td.byteValue() & 0x3;
            td = td.shiftRight(2);
            for(int tx=NUM_TX-1; tx>=0; --tx) {
                td = readTx(td, tx);
            }
        }

        String getTransferData() {
            BigInteger td = BigInteger.ZERO;
            for(int tx=0; tx<NUM_TX; ++tx) {
                int op = transfer[tx];
                if(op < 0) op += 0x80;
                td = td.shiftLeft(7).or(BigInteger.valueOf(op & 0x7f));
            }
            td = td.shiftLeft(2).or(BigInteger.valueOf(speed & 0x3));
            return String.format("%011x", td);
        }
    }

    private static class ProgramData {
        int numStates;
        String header;
        String transferData;
        String patternData;
        boolean valid;

        ProgramData(int numStates, String header, String transferData, String patternData) {
            this.numStates = numStates;
            this.header = header;
            this.transferData = transferData;
            this.patternData = patternData;
            valid = true;
        }

        ProgramData(String program) {
            if(program.length() < 22) { invalidate(); return; }
            if(!program.startsWith(magic)) {invalidate(); return; }
            numStates = new BigInteger(program.substring(8, 10), 16).byteValue();
            if(numStates < 1 || numStates > stateCap) { invalidate(); return; }
            int tLen = (numStates * 11 + 1) & ~1;
            int pLen = numStates * 16;
            if(program.length() != 22 + tLen + pLen) { invalidate(); return; }
            header = program.substring(10, 22);
            transferData = program.substring(22, 22 + tLen);
            patternData = program.substring(22 + tLen, 22 + tLen + pLen);
            valid = true;
        }

        private void invalidate() {
            numStates = 0;
            header = transferData = patternData = "";
            valid = false;
        }

        String toProgramString() {
            String nsData = String.format("%02x", numStates);
            int tLen = numStates * 11;
            int pLen = numStates * 16;
            if(header.length() != 12) return "error";
            if(transferData.length() != tLen) return "error";
            if(patternData.length() != pLen) return "error";
            String tPad = (numStates % 2 != 0) ? "0" : "";
            return magic + nsData + header + transferData + tPad + patternData;
        }

    }

    private final ArrayList<State> states;
    private final int[] defaultTransfer;
    private int globalSpeed;
    private State clipboard;
    private int currentState;
    private int playbackSpeed;
    private boolean playbackPaused;
    private String name;
    private OnChangeListener onChangeListener;

    private static final int stateCap = 75;
    private static final String magic = "73743031";
    private static final int[] timerMultipliers = {1, 2, 4, 0};
    private static final int[] timerList = {2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 0};

    public static final int TX_LEFT = 0;
    public static final int TX_RIGHT = 1;
    public static final int TX_UP = 2;
    public static final int TX_DOWN = 3;
    public static final int TX_CLICK = 4;
    public static final int TX_AUTO = 5;
    public static final int NUM_TX = 6;

    public static final int OP_INHERIT = -1;
    public static final int OP_NEXT = -2;
    public static final int OP_PREV = -3;
    public static final int OP_PAUSE = -4;
    public static final int OP_FASTER = -5;
    public static final int OP_SLOWER = -6;
    public static final int OP_NONE = -7;
    public static final int OP_ERROR = -8;  // also reusing for error state
    public static final int NUM_OP = 8;

    public StateMachine() {
        states = new ArrayList<>();
        defaultTransfer = new int[NUM_TX];
        onChangeListener = null;
        resetParams(false);
    }

    public StateMachine(StateMachine sm, boolean keepState) {
        states = new ArrayList<>();
        for(State s : sm.states) {
            states.add(new State(s));
        }
        defaultTransfer = sm.defaultTransfer.clone();
        globalSpeed = sm.globalSpeed;
        if(keepState) {
            clipboard = sm.clipboard;
            currentState = sm.currentState;
            playbackSpeed = sm.playbackSpeed;
            playbackPaused = sm.playbackPaused;
            name = sm.name;
        } else {
            clipboard = null;
            name = "";
            resetPlayback();
        }
        onChangeListener = null;
    }

    private void loadProgramDataInternal(ProgramData pd) {
        if(!pd.valid) { resetParams(true); return; }
        int n = pd.numStates;
        if(n < 1 || n > stateCap) { resetParams(true); return; }
        if(pd.header.length() != 12) { resetParams(true); return; }
        if(pd.transferData.length() < 11 * n) { resetParams(true); return; }
        if(pd.patternData.length() < 16 * n) { resetParams(true); return; }
        BigInteger hd = new BigInteger(pd.header, 16);
        globalSpeed = hd.byteValue() & 0x3f;
        if(globalSpeed >= timerList.length) { resetParams(true); return; }
        playbackSpeed = globalSpeed;
        hd = hd.shiftRight(6);
        for(int tx=NUM_TX-1; tx>=0; --tx) {
            int op = hd.byteValue() & 0x7f;
            if(op >= 0x80 - NUM_OP) op -= 0x80;
            defaultTransfer[tx] = op;
            hd = hd.shiftRight(7);
        }
        states.clear();
        for(int i=0; i<n; ++i) {
            State s = new State(pd.transferData.substring(11 * i, 11 * (i+1)),
                    pd.patternData.substring(16 * i, 16 * (i+1)));
            states.add(s);
        }
    }

    public StateMachine(ProgramData pd) {
        this();
        loadProgramDataInternal(pd);
    }

    private void loadProgramInternal(String program) {
        loadProgramDataInternal(new ProgramData(program));
    }

    private void loadRepresentationInternal(String repr) {
        Scanner s = new Scanner(repr);
        s.useDelimiter(",");
        loadProgramInternal(s.next());
        String clipboardTransferData = s.next();
        String clipboardPattern = s.next();
        if(!clipboardTransferData.isEmpty()) {
            clipboard = new State(clipboardTransferData, clipboardPattern);
        }
        currentState = s.nextInt();
        playbackSpeed = s.nextInt();
        playbackPaused = s.nextInt() != 0;
        s.skip(",");
        s.useDelimiter("\\A");  // read the rest of repr
        name = s.next();
    }

    public StateMachine(String programOrRepr) {
        this();
        if(programOrRepr.contains(",")) {
            loadRepresentationInternal(programOrRepr);
        } else {
            loadProgramInternal(programOrRepr);
        }
    }

    private ProgramData getProgramData() {
        BigInteger td = BigInteger.ZERO;
        for(int tx=0; tx<NUM_TX; ++tx) {
            int op = defaultTransfer[tx];
            if(op < 0) op += 0x80;
            td = td.shiftLeft(7).or(BigInteger.valueOf(op & 0x7f));
        }
        td = td.shiftLeft(6).or(BigInteger.valueOf(globalSpeed & 0x3f));
        String header = String.format("%012x", td);
        StringBuilder transferData = new StringBuilder();
        StringBuilder patternData = new StringBuilder();
        int n = states.size();
        for(int i=0; i<n; ++i) {
            State s = states.get(i);
            transferData.append(s.getTransferData());
            patternData.append(s.pattern);
        }
        return new ProgramData(n, header, transferData.toString(), patternData.toString());
    }

    public String getProgram() {
        return getProgramData().toProgramString();
    }

    public String getRepresentation() {
        String clipboardTransferData = "";
        String clipboardPattern = "";
        if(clipboard != null) {
            clipboardTransferData = clipboard.getTransferData();
            clipboardPattern = clipboard.pattern;
        }
        return String.format(Locale.US,"%s,%s,%s,%d,%d,%d,%s",
                getProgram(),
                clipboardTransferData,
                clipboardPattern,
                currentState,
                playbackSpeed,
                playbackPaused ? 1 : 0,
                name);
    }

    public void setOnChangeListener(OnChangeListener listener) {
        onChangeListener = listener;
    }

    private void fireOnChange() {
        if(onChangeListener != null) onChangeListener.onChange();
    }

    public void loadProgram(String program) {
        resetParams(false);
        loadProgramInternal(program);
        fireOnChange();
    }

    public void loadRepresentation(String repr) {
        resetParams(false);
        loadRepresentationInternal(repr);
        fireOnChange();
    }

    private void resetParams(boolean error) {
        states.clear();
        states.add(new State());
        defaultTransfer[TX_LEFT] = OP_PREV;
        defaultTransfer[TX_RIGHT] = OP_NEXT;
        defaultTransfer[TX_UP] = OP_FASTER;
        defaultTransfer[TX_DOWN] = OP_SLOWER;
        defaultTransfer[TX_CLICK] = OP_PAUSE;
        defaultTransfer[TX_AUTO] = OP_NEXT;
        globalSpeed = 5;
        clipboard = null;
        name = "";
        resetPlayback();
        if(error) currentState = OP_ERROR;
    }

    public boolean isErrorState() {
        return currentState < 0 || currentState >= states.size();
    }

    public void gotoState(int state) {
        if(state < 0 || state >= states.size()) {
            currentState = OP_ERROR;
        } else {
            currentState = state;
        }
        fireOnChange();
    }

    public int getCurrentState() {
        return currentState;
    }

    public int getStateCount() {
        return states.size();
    }

    public String getName() {
        return name;
    }

    public void setName(String newName) {
        name = newName;
        fireOnChange();
    }

    public String getPattern() {
        if(isErrorState()) return "81bda1bda1a13c81";
        return states.get(currentState).pattern;
    }

    public String getThumbnail(int index) {
        if(index < 0 || index >= states.size()) {
            return "0000000000000000";
        } else {
            return states.get(index).pattern;
        }
    }

    public int getRawTransfer(int tx) {
        if(isErrorState()) return OP_ERROR;
        if(tx < 0 || tx >= NUM_TX) return OP_ERROR;
        return states.get(currentState).transfer[tx];
    }

    public int getDefaultTransfer(int tx) {
        if(tx < 0 || tx >= NUM_TX) return OP_ERROR;
        return defaultTransfer[tx];
    }

    public int getTransfer(int tx) {
        int op = getRawTransfer(tx);
        if(op != OP_INHERIT) return op;
        return defaultTransfer[tx];
    }

    public int getSpeed() {
        if(isErrorState()) return 3;
        return states.get(currentState).speed;
    }

    public int getGlobalSpeed() {
        return globalSpeed;
    }

    public int getMaxSpeed() {
        return timerMultipliers.length - 1;
    }

    public int getMaxGlobalSpeed() {
        return timerList.length - 1;
    }

    public int getDelay() {
        if(isErrorState()) return 0;
        if(playbackSpeed < 0 || playbackSpeed >= timerList.length) return 0;
        int speed = states.get(currentState).speed;
        if(speed < 0 || speed >= timerMultipliers.length) return 0;
        return timerList[playbackSpeed] * timerMultipliers[speed];
    }

    public boolean isPaused() {
        return playbackPaused;
    }

    public void resetPlayback() {
        currentState = 0;
        playbackSpeed = globalSpeed;
        playbackPaused = false;
        fireOnChange();
    }

    public void processOp(int op) {
        if(isErrorState()) return;
        if(op >= 0 && op < states.size()) {
            currentState = op;
            fireOnChange();
            return;
        }
        switch (op) {
            case OP_NEXT:
                gotoNextState(true);
                break;
            case OP_PREV:
                gotoPrevState(true);
                break;
            case OP_PAUSE:
                playbackPaused = !playbackPaused;
                fireOnChange();
                break;
            case OP_FASTER:
                if(playbackSpeed > 0) {
                    --playbackSpeed;
                    fireOnChange();
                }
                break;
            case OP_SLOWER:
                if(playbackSpeed < timerList.length-1) {
                    ++playbackSpeed;
                    fireOnChange();
                }
                break;
            default:
                currentState = OP_ERROR;
                fireOnChange();
        }
    }

    public boolean isFirstState() {
        return currentState == 0;
    }

    public boolean isLastState() {
        return currentState == states.size() - 1;
    }

    public int getNextState(boolean rollOver) {
        if(isErrorState()) return currentState;
        if(isLastState()) {
            if(!rollOver) return currentState;
            return 0;
        } else {
            return currentState + 1;
        }
    }

    public void gotoNextState(boolean rollOver) {
        int ns = getNextState(rollOver);
        if(ns != currentState) {
            currentState = ns;
            fireOnChange();
        }
    }

    public int getPrevState(boolean rollOver) {
        if(isErrorState()) return currentState;
        if(isFirstState()) {
            if(!rollOver) return currentState;
            return states.size() -1;
        } else {
            return currentState - 1;
        }
    }

    public void gotoPrevState(boolean rollOver){
        int ps = getPrevState(rollOver);
        if(ps != currentState) {
            currentState = ps;
            fireOnChange();
        }
    }

    private void shiftStateTransfers(State s, int threshold, int shift) {
        for(int t=0; t<NUM_TX; ++t){
            int op = s.transfer[t];
            if(op >= threshold) s.transfer[t] = op + shift;
        }
    }

    private void shiftTransfers(int threshold, int shift) {
        int n = states.size();
        for(int i=0; i<n; ++i) {
            shiftStateTransfers(states.get(i), threshold, shift);
        }
        if(clipboard != null) shiftStateTransfers(clipboard, threshold, shift);
    }

    private void swapStateTransfers(State s, int index) {
        for(int t=0; t<NUM_TX; ++t) {
            int op = s.transfer[t];
            if(op == index) s.transfer[t] = index+1;
            else if(op == index+1) s.transfer[t] = index;
        }
    }

    private void swapTransfers(int index) {
        int n = states.size();
        for(int i=0; i<n; ++i) {
            swapStateTransfers(states.get(i), index);
        }
        if(clipboard != null) swapStateTransfers(clipboard, index);
    }

    private void addStateInternal(State s) {
        if(isErrorState()) return;
        if(states.size() >= stateCap) {
            currentState = OP_ERROR;
            return;
        }
        states.add(currentState + 1, s);
        shiftTransfers(currentState + 1, 1);
        ++currentState;
    }

    public void addState() {
        addStateInternal(new State());
        fireOnChange();
    }

    public void cloneState() {
        addStateInternal(new State(states.get(currentState)));
        fireOnChange();
    }

    public void removeState() {
        if(isErrorState()) return;
        if(states.size() == 1) {
            states.set(0, new State());
            fireOnChange();
            return;
        }
        states.remove(currentState);
        shiftTransfers(currentState, -1);
        if(currentState == states.size()) --currentState;
        fireOnChange();
    }

    public void copyState() {
        if(isErrorState()) return;
        clipboard = new State(states.get(currentState));
        fireOnChange();
    }

    public void cutState() {
        if(isErrorState()) return;
        copyState();
        removeState();
        fireOnChange();
    }

    public void pasteState() {
        if(isErrorState()) return;
        addStateInternal(new State(clipboard));
        fireOnChange();
    }

    public boolean isClipboardValid() {
        return clipboard != null;
    }

    public void clearClipboard() {
        clipboard = null;
        fireOnChange();
    }

    private void swapStates(int index) {
        if(index < 0 || index >= states.size()-1) return;
        State s = states.get(index);
        states.set(index, states.get(index+1));
        states.set(index+1, s);
        swapTransfers(index);
    }

    public void moveStateUp() {
        if(isErrorState() || isFirstState()) return;
        currentState -= 1;
        swapStates(currentState);
        fireOnChange();
    }

    public void moveStateDown() {
        if(isErrorState() || isLastState()) return;
        swapStates(currentState);
        currentState += 1;
        fireOnChange();
    }

    public void setPattern(String value) {
        if(isErrorState()) return;
        states.get(currentState).pattern = value;
        fireOnChange();
    }

    public void setRawTransfer(int tx, int value) {
        if(isErrorState()) return;
        if(tx < 0 || tx >= NUM_TX) return;
        states.get(currentState).transfer[tx] = value;
        fireOnChange();
    }

    public void setDefaultTransfer(int tx, int value) {
        if(tx < 0 || tx >= NUM_TX) return;
        defaultTransfer[tx] = value;
        fireOnChange();
    }

    public void setSpeed(int value) {
        if(isErrorState()) return;
        if(value < 0 || value >= timerMultipliers.length) return;
        states.get(currentState).speed = value;
        fireOnChange();
    }

    public void setGlobalSpeed(int value) {
        if(value < 0 || value >= timerList.length) return;
        globalSpeed = playbackSpeed = value;
        fireOnChange();
    }

}
