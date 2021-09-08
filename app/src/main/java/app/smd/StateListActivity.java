package app.smd;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

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

        int tileWidth = (int) Math.ceil(getResources().getDimension(R.dimen.thumbnail_size) +
                2 * getResources().getDimension(R.dimen.thumbnail_padding));
        rv = (RecyclerView) findViewById(R.id.listStates);
        RecyclerView.LayoutManager lm = new TiledLayoutManager(this, tileWidth);
        rv.setLayoutManager(lm);

        sla = new StateListAdapter(sm);
        rv.setAdapter(sla);

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.tbFrameActions);
        Menu toolbarMenu = toolbar.getMenu();
        inflater.inflate(R.menu.toolbar_state_list, toolbarMenu);
        int menuSize = toolbarMenu.size();
        for(int i = 0; i < menuSize; ++i) {
            toolbarMenu.getItem(i).setOnMenuItemClickListener(this::onOptionsItemSelected);
        }
        return true;
    }

    private void applyChanges() {
        pl.persistState();
        sla.notifyDataSetChanged();
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
            sm.addState();
            applyChanges();
        }
        else if(id == R.id.miCloneFrame) {
            sm.cloneState();
            applyChanges();
        }
        else if(id == R.id.miMoveFrameForward) {
            sm.moveStateUp();
            applyChanges();
        }
        else if(id == R.id.miMoveFrameBackward) {
            sm.moveStateDown();
            applyChanges();
        }
        else if(id == R.id.miCutFrame) {
            sm.cutState();
            applyChanges();
        }
        else if(id == R.id.miCopyFrame) {
            sm.copyState();
            applyChanges();
        }
        else if(id == R.id.miPasteFrame) {
            sm.pasteState();
            applyChanges();
        }
        else if(id == R.id.miDeleteFrame) {
            sm.removeState();
            applyChanges();
        }
        return true;
    }
}