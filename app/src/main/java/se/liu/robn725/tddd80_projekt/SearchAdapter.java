package se.liu.robn725.tddd80_projekt;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

/**
 * Adapter to display the search results.
 * Displays an ArrayList of the object type User.
 */
public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
    private View view;
    private Context context;
    private SearchOnClickListener listener;
    private ArrayList<User> users;

    public SearchAdapter(Context context, SearchOnClickListener listener, ArrayList<User> users) {
        this.context = context;
        this.listener = listener;
        this.users = users;
    }

    interface SearchOnClickListener {
        void onClickedUser(String username);

    }

    @NonNull
    @Override
    public SearchAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        view = layoutInflater.inflate(R.layout.user_search, parent, false);
        ViewHolder viewHolder = new SearchAdapter.ViewHolder(view, listener);

        return viewHolder;
    }


    @Override
    public void onBindViewHolder(@NonNull SearchAdapter.ViewHolder holder, int position) {

        holder.username.setText(users.get(position).getUsername());
        // Insert internet uri into imageView.
        Glide.with(context).load(users.get(position).getImageUrl()).into(holder.searchImage);
        holder.search_card_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClickedUser(users.get(position).getUsername());
            }
        });
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }
    
    @Override
    public int getItemCount() {
        return users.size();
    }

    /**
     * Initiate all of the views
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView username;
        ImageView searchImage;
        View search_card_view;
        public ViewHolder(@NonNull View itemView, SearchAdapter.SearchOnClickListener listener) {
            super(itemView);
            search_card_view = itemView.findViewById(R.id.search_card_view);
            searchImage = itemView.findViewById(R.id.user_image_search);
            username = itemView.findViewById(R.id.user_search_name);
        }
    }
}
