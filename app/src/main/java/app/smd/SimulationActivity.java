package app.smd;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.Choreographer;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SimulationActivity extends AppCompatActivity {

    private StateMachine sm;
    private int initState;
    private boolean debugMode = false;
    private int viewMode = 0;
    private int delay = 0;
    private long lastTimer = 0;
    private long frameProgress = 0;
    private Choreographer cg;
    private int holdRefresh = 0;
    private boolean pendingRefresh = false;
    private LedGridView ledPattern;
    private TransferButtons tbControls;

    private boolean isPosAvailable(int pos) {
        if(debugMode) return true;
        return pos != 0 && pos != 2 && pos != 6 && pos != 8;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulation);
        ledPattern = findViewById(R.id.ledPattern);
        tbControls = findViewById(R.id.tbControls);

        int mode = getIntent().getIntExtra("mode", 0);
        debugMode = mode == 2;

        sm = new StateMachine((new PersistedProjectList(this)).getMachine(), mode != 0);
        initState = sm.getCurrentState();
        tbControls.setStateMachine(sm);
        tbControls.setupButton(1, TransferButtons.BT_TX, StateMachine.TX_UP, v -> processTx(StateMachine.TX_UP));
        tbControls.setupButton(3, TransferButtons.BT_TX, StateMachine.TX_LEFT, v -> processTx(StateMachine.TX_LEFT));
        tbControls.setupButton(4, TransferButtons.BT_TX, StateMachine.TX_CLICK, v -> processTx(StateMachine.TX_CLICK));
        tbControls.setupButton(5, TransferButtons.BT_TX, StateMachine.TX_RIGHT, v -> processTx(StateMachine.TX_RIGHT));
        tbControls.setupButton(7, TransferButtons.BT_TX, StateMachine.TX_DOWN, v -> processTx(StateMachine.TX_DOWN));
        if(debugMode) {
            tbControls.setupButton(0, TransferButtons.BT_RESET, initState, v -> sm.gotoState(initState));
            tbControls.setupButton(2, TransferButtons.BT_TX, StateMachine.TX_AUTO, v -> processTx(StateMachine.TX_AUTO));
            tbControls.setupButton(6, TransferButtons.BT_OP, StateMachine.OP_PREV, v -> sm.processOp(StateMachine.OP_PREV));
            tbControls.setupButton(8, TransferButtons.BT_OP, StateMachine.OP_NEXT, v -> sm.processOp(StateMachine.OP_NEXT));
        }
        refresh();

        sm.setOnChangeListener(this::stateMachineChanged);
        cg = Choreographer.getInstance();
        cg.postFrameCallback(this::frameCallback);
    }

    private void frameCallback(long frameTimeNanos) {
        if(delay != 0) {
            if(lastTimer != 0) {
                frameProgress += (frameTimeNanos - lastTimer) / delay;
                if (frameProgress > 1000000) {
                    int adv = (int) (frameProgress / 1000000);
                    frameProgress %= 1000000;
                    advanceFrames(adv);
                }
            }
            lastTimer = frameTimeNanos;
        } else {
            lastTimer = 0;
        }
        cg.postFrameCallback(this::frameCallback);
    }

    private void advanceFrames(int frames) {
        lockRefresh();
        for(int i=0; i<frames; ++i) {
            sm.processOp(sm.getTransfer(StateMachine.TX_AUTO));
        }
        unlockRefresh();
    }

    private void stateMachineChanged() {
        if(holdRefresh > 0) {
            pendingRefresh = true;
        } else {
            refresh();
        }
    }

    private void lockRefresh() {
        holdRefresh += 1;
    }

    private void unlockRefresh() {
        holdRefresh -= 1;
        if(holdRefresh < 0) holdRefresh = 0;
        if(holdRefresh == 0 && pendingRefresh) {
            pendingRefresh = false;
            refresh();
        }
    }

    private void refresh() {
        delay = sm.isPaused() ? 0 : sm.getDelay();
        ledPattern.setHexPattern(sm.getPattern(), false);
        tbControls.refresh();
    }

    private void processTx(int tx) {
        sm.processOp(sm.getTransfer(tx));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(!debugMode) return true;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_simulation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.miViewSim) {
            viewMode = (viewMode + 1) % 8;
            tbControls.setDisplayMode(viewMode % 4 + 1, (viewMode / 4) * 2);
            tbControls.refresh();
        }
        else if(id == R.id.miPauseSim) {
            sm.processOp(StateMachine.OP_PAUSE);
        }
        else if(id == R.id.miFasterSim) {
            sm.processOp(StateMachine.OP_FASTER);
        }
        else if(id == R.id.miSlowerSim) {
            sm.processOp(StateMachine.OP_SLOWER);
        }
       return true;
    }

}