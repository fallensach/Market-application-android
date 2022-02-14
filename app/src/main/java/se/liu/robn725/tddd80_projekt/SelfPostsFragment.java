package se.liu.robn725.tddd80_projekt;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Fragment for the user's own posts.
 * Contains a recylerView with a list of posts
 */
public class SelfPostsFragment extends Fragment implements PostAdapter.OnClickListener {
    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private ArrayList<Post> posts;
    private UserViewModel userViewModel;
    private TextView noPostText;
    private MiscDataViewModel miscDataViewModel;
    private boolean isSearch;

    public SelfPostsFragment() {
        // Required empty public constructor
    }

    public static SelfPostsFragment newInstance() {
        return new SelfPostsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_self_posts, container, false);
        // Get the shared ViewModel
        userViewModel = new ViewModelProvider(getActivity()).get(UserViewModel.class);
        miscDataViewModel = new ViewModelProvider(getActivity()).get(MiscDataViewModel.class);
        noPostText = view.findViewById(R.id.no_posts);
        recyclerView = view.findViewById(R.id.self_post_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (!miscDataViewModel.isProfile()) {
            isSearch = !miscDataViewModel.getUsername().equals(userViewModel.getUsername());
        }

        if (isSearch) {
            posts = new ArrayList<>();
            // Add the new posts
            posts.addAll(miscDataViewModel.getUserPosts().getValue());
            updateEmptyPostText();
            postAdapter = new PostAdapter(getContext(), posts, this, "searchedPostFragment");
            recyclerView.setAdapter(postAdapter);
        } else {
            // Request the posts of user
            postsRequest();
            // Observer if new posts are created by the user
            final Observer<ArrayList<Post>> postsObserver = new Observer<ArrayList<Post>>() {
                @Override
                public void onChanged(ArrayList<Post> arrayWithPosts) {
                    // Reset posts before adding new
                    posts = new ArrayList<>();
                    // Add the new posts
                    posts.addAll(arrayWithPosts);
                    // If posts is empty then show no posts text
                    updateEmptyPostText();
                    // Send posts to adapter with context
                    postAdapter = new PostAdapter(getContext(), posts, SelfPostsFragment.this, "selfPostsFragment");
                    recyclerView.setAdapter(postAdapter);
                }
            };
            // Start observing the user posts
            userViewModel.getUserPosts().observe(getActivity(), postsObserver);
        }


        return view;
    }

    /**
     * Sends a post request to the server to get all of the posts
     * from the active user.
     */
    public void postsRequest() {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        String username = userViewModel.getUserData().getValue().get("username");
        String url = "https://market-app-swe.herokuapp.com/get_posts/" + username;
        
        JsonArrayRequest getPostsRequest= new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                userViewModel.getUserPosts();
                try {
                    // Convert JSONArray response to ArrayList<Post>
                    userViewModel.setUserPosts(response);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error);
            }
        });
        queue.add(getPostsRequest);
    }

    /**
     * Sends the postId to the miscDataViewModel so it can update the detail fragment.
     * @param postId Id of the post.
     */
    @Override
    public void onClick(int postId) {
        miscDataViewModel.getPostId();
        miscDataViewModel.setPostId(postId);
        switchToPostDetailFragment();
    }

    /**
     * Deletes a post from the recyclerView and database
     * @param position Position of the adapter to remove.
     * @param postId Id of post to remove.
     */
    @Override
    public void onDeleteClick(int position, int postId) {
        removePost(postId);
        removePostFromAdapter(position);
    }

    /**
     * Removes a post by sending a delete request to the server
     * with the correct postId
     * @param postId Id of the post requested to delete
     */
    public void removePost(int postId) {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        HashMap<String, String> deletePostToken = userViewModel.getTokenHeader();
        JSONObject deletePostParams = new JSONObject(deletePostToken);
        String url = "https://market-app-swe.herokuapp.com/delete_post/" + postId;
        JsonObjectRequest deletePostRequest = new JsonObjectRequest(Request.Method.DELETE, url, deletePostParams, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }) {
            // Send header with token
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                params.put("Authorization", "Bearer " + userViewModel.getToken().getValue());
                return params;
            }
        };
        queue.add(deletePostRequest);
    }

    /**
     * Removes the clicked item from the adapter's recyclerView
     * @param position index of the adapter position
     */
    public void removePostFromAdapter(int position) {
        posts.remove(position);
        postAdapter.notifyItemRemoved(position);
        postAdapter.notifyItemRangeChanged(position, posts.size());
        updateEmptyPostText();
    }

    /**
     * Updates the no post text.
     * Should be called whenever the postAdapter is changed.
     */
    public void updateEmptyPostText() {
        if (!miscDataViewModel.isProfile()) {
            if (miscDataViewModel.getUserPosts().getValue().isEmpty()) {
                noPostText.setVisibility(View.VISIBLE);
            } else {
            noPostText.setVisibility(View.INVISIBLE);
        }

    } else {
            if (posts.size() == 0) {
                noPostText.setVisibility(View.VISIBLE);
            } else {
                noPostText.setVisibility(View.INVISIBLE);
            }
        }

    }


    public void switchToPostDetailFragment() {
        NavController navController = Navigation.findNavController(requireView());
        if (isSearch) {
            navController.navigate(R.id.action_searchedUserProfile_to_postDetailFragment);
        } else {
            navController.navigate(R.id.action_profileFragment_to_postDetailFragment);
        }
    }

}