package app.smd;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class StateListActivity extends AppCompatActivity {

    private PersistedProjectList pl;
    private StateMachine sm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_state_list);

        pl = new PersistedProjectList(this);
        sm = pl.getWrappedProjectList().getMachine();

        int tileWidth = (int) Math.ceil(getResources().getDimension(R.dimen.thumbnail_size) +
                2 * getResources().getDimension(R.dimen.thumbnail_padding));
        RecyclerView rv = (RecyclerView) findViewById(R.id.listStates);
        RecyclerView.LayoutManager lm = new TiledLayoutManager(this, tileWidth);
        rv.setLayoutManager(lm);

        StateListAdapter sla = new StateListAdapter(sm);
        rv.setAdapter(sla);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pl.persistState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_state_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return true;
    }
}