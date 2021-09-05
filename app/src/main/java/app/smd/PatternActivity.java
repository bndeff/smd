package app.smd;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class PatternActivity extends AppCompatActivity {

    private PersistedProjectList pl;
    private StateMachine sm;
    private LedGridView led;
    private TransferButtons tb;
    private boolean showTransfers = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pattern);

        pl = new PersistedProjectList(this);
        sm = pl.getMachine();
        led = (LedGridView) findViewById(R.id.ledPattern);
        loadFrame();
        led.setOnChangeListener(() -> {
            sm.setPattern(led.getHexPattern());
        });

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

        pl.setOnChangeListener(() -> {
            tb.refresh();
        });
    }

    private void loadFrame() {
        led.setHexPattern(sm.getPattern(), false);
        led.clearUndo();
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_pattern, menu);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tbPatternActions);
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

    private void updateTx(int tx) {
        int rt = sm.getRawTransfer(tx) + 1;
        if(rt >= sm.getStateCount()) rt = -StateMachine.NUM_OP;
        sm.setRawTransfer(tx, rt);
        applyChanges();
        tb.refresh();
    }

    private void unimplemented() {
        Toast.makeText(this, "Not yet implemented", Toast.LENGTH_SHORT).show();
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
        else if(id == R.id.miInvertPattern) {
            led.invert();
            applyChanges();
        }
        else if(id == R.id.miClearPattern) {
            led.clear();
            applyChanges();
        }
        return true;
    }
}