package app.smd;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

public class ProjectSettingsActivity extends AppCompatActivity {

    private PersistedProjectList pl;
    private StateMachine sm;
    private TransferButtons tb;
    private TextView txtTitle;
    private SeekBar sbSpeed;
    private int maxSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_settings);

        pl = new PersistedProjectList(this);
        sm = pl.getMachine();

        txtTitle = findViewById(R.id.txtTitle);
        txtTitle.setText(sm.getName());

        sbSpeed = findViewById(R.id.sbSpeed);
        maxSpeed = sm.getMaxGlobalSpeed();
        sbSpeed.setMax(maxSpeed);
        sbSpeed.setProgress(maxSpeed - sm.getGlobalSpeed());

        tb = findViewById(R.id.tbControls);
        tb.setStateMachine(sm);
        tb.setupButton(1, TransferButtons.BT_DEF, StateMachine.TX_UP, v -> updateTx(StateMachine.TX_UP));
        tb.setupButton(2, TransferButtons.BT_DEF, StateMachine.TX_AUTO, v -> updateTx(StateMachine.TX_AUTO));
        tb.setupButton(3, TransferButtons.BT_DEF, StateMachine.TX_LEFT, v -> updateTx(StateMachine.TX_LEFT));
        tb.setupButton(4, TransferButtons.BT_DEF, StateMachine.TX_CLICK, v -> updateTx(StateMachine.TX_CLICK));
        tb.setupButton(5, TransferButtons.BT_DEF, StateMachine.TX_RIGHT, v -> updateTx(StateMachine.TX_RIGHT));
        tb.setupButton(7, TransferButtons.BT_DEF, StateMachine.TX_DOWN, v -> updateTx(StateMachine.TX_DOWN));
        tb.setDisplayMode(TransferButtons.FG_RAW, TransferButtons.BG_FIXED);
        tb.refresh();

        this.setTitle(sm.getName());

        pl.setOnChangeListener(() -> tb.refresh());
    }

    @Override
    protected void onPause() {
        super.onPause();
        applyChanges();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pl.loadState();
        sm = pl.getMachine();
        tb.setStateMachine(sm);
        tb.refresh();
    }

    private void applyChanges() {
        sm.setName(txtTitle.getText().toString());
        sm.setGlobalSpeed(maxSpeed - sbSpeed.getProgress());
        pl.persistState();
    }

    private void updateTx(int tx) {
        int rt = sm.getDefaultTransfer(tx) + 1;
        if(rt == -1) rt = 0;
        if(rt >= sm.getStateCount()) rt = -StateMachine.NUM_OP;
        sm.setDefaultTransfer(tx, rt);
        applyChanges();
        tb.refresh();
    }

}