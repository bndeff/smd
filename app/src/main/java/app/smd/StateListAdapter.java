package app.smd;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class StateListAdapter extends RecyclerView.Adapter<StateListAdapter.ViewHolder> {

    private final StateMachine sm;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final LedGridView ledPreview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ledPreview = (LedGridView) itemView.findViewById(R.id.ledPreview);
            ledPreview.setEditable(false);
        }

        @SuppressLint("ResourceAsColor")
        public void bindFrame(String pattern, boolean isSelected) {
            ledPreview.setHexPattern(pattern, false);
            itemView.setBackgroundColor(isSelected ? 0x400000ff : 0x00000000);
        }

    }

    public StateListAdapter(StateMachine stateMachine) {
        sm = stateMachine;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_state_list, parent, false);
        return new StateListAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindFrame(sm.getThumbnail(position), sm.getCurrentState() == position);
        holder.itemView.setOnClickListener(v -> {
            sm.gotoState(position);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return sm.getStateCount();
    }

}