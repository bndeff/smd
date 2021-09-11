package app.smd;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class PatternActivity extends AppCompatActivity {

    private PersistedProjectList pl;
    private StateMachine sm;
    private LedGridView led;
    private TransferButtons tb;
    private boolean showTransfers = false;
    private MenuItem speedMenu;
    private int currentSpeed;
    private int shownSpeed = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pattern);

        pl = new PersistedProjectList(this);
        sm = pl.getMachine();
        led = findViewById(R.id.ledPattern);
        loadFrame();
        led.setOnChangeListener(() -> sm.setPattern(led.getHexPattern()));

        tb = findViewById(R.id.tbControls);
        tb.setStateMachine(sm);
        tb.setupButton(1, TransferButtons.BT_TX, StateMachine.TX_UP, v -> updateTx(StateMachine.TX_UP));
        tb.setupButton(2, TransferButtons.BT_TX, StateMachine.TX_AUTO, v -> updateTx(StateMachine.TX_AUTO));
        tb.setupButton(3, TransferButtons.BT_TX, StateMachine.TX_LEFT, v -> updateTx(StateMachine.TX_LEFT));
        tb.setupButton(4, TransferButtons.BT_TX, StateMachine.TX_CLICK, v -> updateTx(StateMachine.TX_CLICK));
        tb.setupButton(5, TransferButtons.BT_TX, StateMachine.TX_RIGHT, v -> updateTx(StateMachine.TX_RIGHT));
        tb.setupButton(7, TransferButtons.BT_TX, StateMachine.TX_DOWN, v -> updateTx(StateMachine.TX_DOWN));
        tb.setDisplayMode(TransferButtons.FG_RAW, TransferButtons.BG_SHOW);
        tb.refresh();
        updateTitle();
        updateSpeed();

        if(savedInstanceState != null) {
            showTransfers = savedInstanceState.getBoolean("showTransfers", false);
        }
        tb.setVisibility(showTransfers ? View.VISIBLE : View.INVISIBLE);

        pl.setOnChangeListener(() -> {
            tb.refresh();
            updateTitle();
            updateSpeed();
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("showTransfers", showTransfers);
    }

    private String getSpeedTitle(int speed) {
        switch (speed) {
            case 0:
                return getString(R.string.speed_fast);
            case 1:
                return getString(R.string.speed_normal);
            case 2:
                return getString(R.string.speed_slow);
            case 3:
                return getString(R.string.speed_stopped);
            default:
                return getString(R.string.speed_unknown);
        }
    }

    private void updateSpeed() {
        if(speedMenu != null && shownSpeed != currentSpeed) {
            speedMenu.setTitle(getSpeedTitle(currentSpeed));
            shownSpeed = currentSpeed;
        }
    }

    private void loadFrame() {
        if(sm.isErrorState()) {
            sm.gotoState(sm.getStateCount()-1);
        }
        led.setHexPattern(sm.getPattern(), false);
        led.clearUndo();
        currentSpeed = sm.getSpeed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pl.persistState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pl.loadState();
        sm = pl.getMachine();
        tb.setStateMachine(sm);
        tb.refresh();
        updateTitle();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_pattern, menu);
        speedMenu = menu.findItem(R.id.miCycleSpeed);
        updateSpeed();

        Toolbar toolbar = findViewById(R.id.tbPatternActions);
        Menu toolbarMenu = toolbar.getMenu();
        inflater.inflate(R.menu.toolbar_pattern, toolbarMenu);
        int menuSize = toolbarMenu.size();
        for(int i = 0; i < menuSize; ++i) {
            toolbarMenu.getItem(i).setOnMenuItemClickListener(this::onOptionsItemSelected);
        }
        return true;
    }

    private void applyChanges() {
        pl.persistState();
    }

    private void updateTitle() {
        this.setTitle(sm.getName());
        String frameTitle = String.format(getString(R.string.frame_number_template), sm.getCurrentState() + 1);
        ((Toolbar) findViewById(R.id.tbPatternActions)).setTitle(frameTitle);
    }

    private void updateTx(int tx) {
        int rt = sm.getRawTransfer(tx) + 1;
        if(rt >= sm.getStateCount()) rt = -StateMachine.NUM_OP;
        sm.setRawTransfer(tx, rt);
        applyChanges();
        tb.refresh();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.miPreviousFrame) {
            sm.gotoPrevState(true);
            loadFrame();
            applyChanges();
        }
        else if(id == R.id.miNextFrame) {
            sm.gotoNextState(true);
            loadFrame();
            applyChanges();
        }
        else if(id == R.id.miUndoPattern) {
            led.undo();
            applyChanges();
        }
        else if(id == R.id.miRedoPattern) {
            led.redo();
            applyChanges();
        }
        else if(id == R.id.miTransfers) {
            showTransfers = !showTransfers;
            tb.setVisibility(showTransfers ? View.VISIBLE : View.INVISIBLE);
        }
        else if(id == R.id.miCycleSpeed) {
            if(currentSpeed == sm.getMaxSpeed()) {
                currentSpeed = 0;
            } else {
                currentSpeed += 1;
            }
            sm.setSpeed(currentSpeed);
            currentSpeed = sm.getSpeed();
            applyChanges();
        }
        else if(id == R.id.miShiftPatternLeft) {
            led.shiftLeft();
            applyChanges();
        }
        else if(id == R.id.miShiftPatternRight) {
            led.shiftRight();
            applyChanges();
        }
        else if(id == R.id.miShiftPatternUp) {
            led.shiftUp();
            applyChanges();
        }
        else if(id == R.id.miShiftPatternDown) {
            led.shiftDown();
            applyChanges();
        }
        else if(id == R.id.miRotatePatternLeft) {
            led.rotateLeft();
            applyChanges();
        }
        else if(id == R.id.miRotatePatternRight) {
            led.rotateRight();
            applyChanges();
        }
        else if(id == R.id.miFlipHorizontally) {
            led.flipHorizontally();
            applyChanges();
        }
        else if(id == R.id.miFlipVertically) {
            led.flipVertically();
            applyChanges();
        }
        else if(id == R.id.miInvertPattern) {
            led.invert();
            applyChanges();
        }
        else if(id == R.id.miClearPattern) {
            led.clear();
            applyChanges();
        }
        else if(id == R.id.miAddPatternFrame) {
            sm.addState();
            loadFrame();
            applyChanges();
        }
        else if(id == R.id.miClonePatternFrame) {
            sm.cloneState();
            loadFrame();
            applyChanges();
        }
        else if(id == R.id.miDeletePatternFrame) {
            sm.removeState();
            loadFrame();
            applyChanges();
        }
        return true;
    }
}