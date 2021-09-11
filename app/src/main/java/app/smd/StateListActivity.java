package app.smd;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.Locale;

public class StateListActivity extends AppCompatActivity {

    private PersistedProjectList pl;
    private StateMachine sm;
    private RecyclerView rv;
    private StateListAdapter sla;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_state_list);

        pl = new PersistedProjectList(this);
        sm = pl.getMachine();

        rv = findViewById(R.id.listStates);
        int spans = 4;
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            spans = 7;
        }
        RecyclerView.LayoutManager lm = new GridLayoutManager(this, spans);
        rv.setLayoutManager(lm);

        sla = new StateListAdapter(sm);
        rv.setAdapter(sla);
        SimpleItemAnimator rva = (SimpleItemAnimator) rv.getItemAnimator();
        if(rva != null) rva.setSupportsChangeAnimations(false);

        pl.setOnChangeListener(this::updateFrame);
        updateFrame();
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
        sla = new StateListAdapter(sm);
        rv.setAdapter(sla);
        updateFrame();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_state_list, menu);

        Toolbar toolbar = findViewById(R.id.tbFrameActions);
        Menu toolbarMenu = toolbar.getMenu();
        inflater.inflate(R.menu.toolbar_state_list, toolbarMenu);
        int menuSize = toolbarMenu.size();
        for(int i = 0; i < menuSize; ++i) {
            toolbarMenu.getItem(i).setOnMenuItemClickListener(this::onOptionsItemSelected);
        }
        return true;
    }

    private void updateFrame() {
        if(sm.isErrorState()) {
            sm.gotoState(sm.getStateCount()-1);
        }
        updateTitle();
    }

    private void updateTitle() {
        this.setTitle(sm.getName());
        String frameTitle = String.format(Locale.US, "Frame #%d", sm.getCurrentState() + 1);
        ((Toolbar) findViewById(R.id.tbFrameActions)).setTitle(frameTitle);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.miPlayFrame) {
            Intent intent = new Intent(this, SimulationActivity.class);
            intent.putExtra("mode", 1);
            this.startActivity(intent);
        }
        else if(id == R.id.miDebugFrame) {
            Intent intent = new Intent(this, SimulationActivity.class);
            intent.putExtra("mode", 2);
            this.startActivity(intent);
        }
        else if(id == R.id.miProjectSettings) {
            Intent intent = new Intent(this, ProjectSettingsActivity.class);
            this.startActivity(intent);
        }
        else if(id == R.id.miEditFrame) {
            Intent intent = new Intent(this, PatternActivity.class);
            this.startActivity(intent);
        }
        else if(id == R.id.miAddFrame) {
            int oldIndex = sm.getCurrentState();
            sm.addState();
            pl.persistState();
            if(!sm.isErrorState()) sla.notifyItemInserted(sm.getCurrentState());
            if(oldIndex >= 0) sla.notifyItemChanged(oldIndex);
        }
        else if(id == R.id.miCloneFrame) {
            int oldIndex = sm.getCurrentState();
            sm.cloneState();
            pl.persistState();
            if(!sm.isErrorState()) sla.notifyItemInserted(sm.getCurrentState());
            if(oldIndex >= 0) sla.notifyItemChanged(oldIndex);
        }
        else if(id == R.id.miMoveFrameForward) {
            int oldIndex = sm.getCurrentState();
            sm.moveStateUp();
            pl.persistState();
            if(oldIndex >= 0 && !sm.isErrorState()) {
                sla.notifyItemMoved(oldIndex, sm.getCurrentState());
            }
        }
        else if(id == R.id.miMoveFrameBackward) {
            int oldIndex = sm.getCurrentState();
            sm.moveStateDown();
            pl.persistState();
            if(oldIndex >= 0 && !sm.isErrorState()) {
                sla.notifyItemMoved(oldIndex, sm.getCurrentState());
            }
        }
        else if(id == R.id.miCutFrame) {
            int oldIndex = sm.getCurrentState();
            sm.cutState();
            pl.persistState();
            if(oldIndex >= 0) {
                sla.notifyItemRemoved(oldIndex);
                if(!sm.isErrorState()) sla.notifyItemChanged(sm.getCurrentState());
            }
        }
        else if(id == R.id.miCopyFrame) {
            sm.copyState();
            pl.persistState();
        }
        else if(id == R.id.miPasteFrame) {
            int oldIndex = sm.getCurrentState();
            sm.pasteState();
            pl.persistState();
            if(!sm.isErrorState()) {
                if(oldIndex >= 0) sla.notifyItemChanged(oldIndex);
                sla.notifyItemInserted(sm.getCurrentState());
            }
        }
        else if(id == R.id.miDeleteFrame) {
            int oldIndex = sm.getCurrentState();
            sm.removeState();
            pl.persistState();
            if(oldIndex >= 0) {
                sla.notifyItemRemoved(oldIndex);
                if(!sm.isErrorState()) sla.notifyItemChanged(sm.getCurrentState());
            }
        }
        return true;
    }
}