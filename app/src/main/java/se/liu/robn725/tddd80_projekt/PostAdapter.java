package se.liu.robn725.tddd80_projekt;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * Adapter that handles the recyclerview for a list of posts.
 * Takes a context, arrayList of object post and a listener.
 */
public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    private ArrayList<Post> posts;
    private Context context;
    private OnClickListener listener;
    private String fragmentName;
    private View view;
    private boolean isHomeFeedActive;

    public PostAdapter(Context context, ArrayList<Post> posts, OnClickListener listener, String fragmentName) {
        this.posts = posts;
        this.context = context;
        this.listener = listener;
        this.fragmentName = fragmentName;
    }

    interface OnClickListener {
        void onClick(int postId);
        void onDeleteClick(int position, int postId);
    }

    @NonNull
    @Override
    public PostAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        // Since multiple fragments use this adapter we have to divide 
        // it into different layouts depending on which one is used.
        if (fragmentName.equals("selfPostsFragment")) {
            view = layoutInflater.inflate(R.layout.self_post, parent, false);
            isHomeFeedActive = false;
        } else if (fragmentName.equals("homeFragment")) {
            view = layoutInflater.inflate(R.layout.home_feed, parent, false);
            isHomeFeedActive = true;
        } else if (fragmentName.equals("searchedPostFragment")) {
            view = layoutInflater.inflate(R.layout.searched_post, parent, false);
            isHomeFeedActive = true;
        }

        ViewHolder viewHolder = new ViewHolder(view, listener);

        return viewHolder;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onBindViewHolder(@NonNull PostAdapter.ViewHolder holder, int position) {
        String pris = "Pris: " + posts.get(position).getPrice() + "kr";
        holder.postTitle.setText(posts.get(position).getTitle());
        holder.postDescription.setText(posts.get(position).getDescription());
        holder.postPrice.setText(pris);
        holder.likes.setText(Integer.toString(posts.get(position).getLikes()));
        holder.location.setText(posts.get(position).getLocation());
        holder.username.setText(posts.get(position).getOwnerId());
        int postId = posts.get(position).getId();
        Uri postPictureAsUri = Uri.parse(posts.get(position).getPicture());
        Glide.with(context).load(postPictureAsUri).into(holder.postImage);
        
        // Check if homeFeed.
        if (!isHomeFeedActive) {
            // Hide the delete post button.
            holder.deletePostButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onDeleteClick(position, postId);
                }
            });
        }
            
            // Add on click listener to the home feed posts.
            holder.postLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onClick(postId);
                }
            });

    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    /**
     * Initiate all of the views
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView postTitle;
        TextView postDescription;
        TextView postPrice;
        Button deletePostButton;
        View postLayout;
        OnClickListener listener;
        TextView likes;
        ImageView postImage;
        TextView location;
        TextView username;
        public ViewHolder(@NonNull View itemView, OnClickListener listener) {
            super(itemView);
            this.listener = listener;
            location = itemView.findViewById(R.id.post_location);
            likes = itemView.findViewById(R.id.post_likes);
            username = itemView.findViewById(R.id.post_username);
            postTitle = itemView.findViewById(R.id.post_title);
            postDescription = itemView.findViewById(R.id.post_description);
            postPrice = itemView.findViewById(R.id.post_price);
            postLayout = itemView.findViewById(R.id.post_layout);
            if (!isHomeFeedActive) {
                deletePostButton = itemView.findViewById(R.id.delete_post_button);
            }
            postImage = itemView.findViewById(R.id.post_picture);
        }
    }

}
