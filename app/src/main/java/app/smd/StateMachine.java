package app.smd;

import java.math.BigInteger;
import java.util.ArrayList;

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
    public static final int OP_ERROR = -8;
    public static final int NUM_OP = 8;

    public StateMachine() {
        states = new ArrayList<>();
        defaultTransfer = new int[NUM_TX];
        resetParams(false);
    }

    public StateMachine(StateMachine sm) {
        states = new ArrayList<>();
        for(State s : sm.states) {
            states.add(new State(s));
        }
        defaultTransfer = sm.defaultTransfer.clone();
        globalSpeed = sm.globalSpeed;
        clipboard = null;
        resetPlayback();
    }

    public StateMachine(ProgramData pd) {
        this();
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

    public StateMachine(String program) {
        this(new ProgramData(program));
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

    public String getProgramString() {
        return getProgramData().toProgramString();
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
        resetPlayback();
        if(error) currentState = -1;
    }

    public boolean isErrorState() {
        return currentState < 0 || currentState >= states.size();
    }

    public void gotoState(int state) {
        if(state < 0 || state >= states.size()) {
            currentState = -1;
        } else {
            currentState = state;
        }
    }

    public int getCurrentState() {
        return currentState;
    }

    public int getStateCount() {
        return states.size();
    }

    public String getPattern() {
        if(isErrorState()) return "81bda1bda1a13c81";
        return states.get(currentState).pattern;
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
    }

    public void processOp(int op) {
        if(isErrorState()) return;
        if(op >= 0 && op < states.size()) {
            currentState = op;
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
                break;
            case OP_FASTER:
                if(playbackSpeed > 0) --playbackSpeed;
                break;
            case OP_SLOWER:
                if(playbackSpeed < timerList.length-1) ++playbackSpeed;
                break;
            default:
                currentState = -1;  // error
        }
    }

    public boolean isFirstState() {
        return currentState == 0;
    }

    public boolean isLastState() {
        return currentState == states.size() - 1;
    }

    public void gotoNextState(boolean rollOver) {
        if(isErrorState()) return;
        if(isLastState()) {
            if(rollOver) currentState = 0;
        } else {
            currentState = currentState + 1;
        }
    }

    public void gotoPrevState(boolean rollOver){
        if(isErrorState()) return;
        if(isFirstState()) {
            if(rollOver) currentState = states.size() -1;
        } else {
            currentState = currentState - 1;
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
            currentState = -1;  // error
            return;
        }
        states.add(currentState + 1, s);
        shiftTransfers(currentState + 1, 1);
        ++currentState;
    }

    public void addState() {
        addStateInternal(new State());
    }

    public void cloneState() {
        addStateInternal(new State(states.get(currentState)));
    }

    public void removeState() {
        if(isErrorState()) return;
        if(states.size() == 1) {
            states.set(0, new State());
            return;
        }
        states.remove(currentState);
        shiftTransfers(currentState, -1);
        if(currentState == states.size()) --currentState;
    }

    public void copyState() {
        if(isErrorState()) return;
        clipboard = new State(states.get(currentState));
    }

    public void cutState() {
        if(isErrorState()) return;
        copyState();
        removeState();
    }

    public void pasteState() {
        if(isErrorState()) return;
        addStateInternal(new State(clipboard));
    }

    public boolean isClipboardValid() {
        return clipboard != null;
    }

    public void clearClipboard() {
        clipboard = null;
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
    }

    public void moveStateDown() {
        if(isErrorState() || isLastState()) return;
        swapStates(currentState);
        currentState += 1;
    }

    public void setPattern(String value) {
        if(isErrorState()) return;
        states.get(currentState).pattern = value;
    }

    public void setRawTransfer(int tx, int value) {
        if(isErrorState()) return;
        if(tx < 0 || tx >= NUM_TX) return;
        states.get(currentState).transfer[tx] = value;
    }

    public void setDefaultTransfer(int tx, int value) {
        if(tx < 0 || tx >= NUM_TX) return;
        defaultTransfer[tx] = value;
    }

    public void setSpeed(int value) {
        if(isErrorState()) return;
        if(value < 0 || value >= timerMultipliers.length) return;
        states.get(currentState).speed = value;
    }

    public void setGlobalSpeed(int value) {
        if(value < 0 || value >= timerList.length) return;
        globalSpeed = playbackSpeed = value;
    }

}
