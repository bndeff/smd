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
import android.widget.Toast;

public class PatternActivity extends AppCompatActivity {

    private PersistedProjectList pl;
    private StateMachine sm;
    private LedGridView led;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pattern);

        pl = new PersistedProjectList(this);
        sm = pl.getMachine();
        led = (LedGridView) findViewById(R.id.ledPattern);
        led.setHexPattern(sm.getPattern(), false);
        led.setOnChangeListener(() -> {
            sm.setPattern(led.getHexPattern());
        });
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

    private void unimplemented() {
        Toast.makeText(this, "Not yet implemented", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.miUndoPattern) {
            led.undo();
            applyChanges();
        }
        else if(id == R.id.miRedoPattern) {
            led.redo();
            applyChanges();
        }
        else if(id == R.id.miLockTransfers) {
            unimplemented();
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