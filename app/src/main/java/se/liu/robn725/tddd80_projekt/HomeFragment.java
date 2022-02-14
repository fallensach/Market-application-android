package se.liu.robn725.tddd80_projekt;

import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static java.lang.Integer.parseInt;

/**
 * HomeFragment has the user feed which can navigate to posts from other users
 */
public class HomeFragment extends Fragment implements PostAdapter.OnClickListener {
    private RecyclerView recyclerView;
    private ArrayList<Post> posts = new ArrayList<>();
    private TextView searchText;
    private UserViewModel userViewModel;
    private MiscDataViewModel miscDataViewModel;
    private View view;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_home, container, false);
        recyclerView = view.findViewById(R.id.home_post_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        searchText = view.findViewById(R.id.search_box);
        userViewModel = new ViewModelProvider(getActivity()).get(UserViewModel.class);
        miscDataViewModel = new ViewModelProvider(getActivity()).get(MiscDataViewModel.class);
        searchText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToSearchResultFragment();
            }
        });

        getFollowingPosts();
        return view;
    }


    @Override
    public void onClick(int postId) {
        miscDataViewModel.getPostId();
        miscDataViewModel.setPostId(postId);
        switchToPostDetail();
    }

    @Override
    public void onDeleteClick(int position, int postId) {

    }
    
    public void switchToSearchResultFragment() {
        NavController navController = Navigation.findNavController(getView());
        navController.navigate(R.id.action_homeFragment_to_searchUserFragment);
    }

    /**
     * Puts all of the posts from the users you follow into posts.
     */
    public void getFollowingPosts() {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        String postsUrl = "https://market-app-swe.herokuapp.com/get_posts_from_following/" + userViewModel.getUsername();
        JsonObjectRequest followingPostsRequest = new JsonObjectRequest(Request.Method.GET, postsUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                // Clear all posts before adding new ones or else it duplicates
                posts.clear();
                try {
                    JSONArray jsonPosts = response.getJSONArray("posts");
                    for (int index = 0; index < jsonPosts.length(); index++) {
                        // Adds all of the data from our response object into a new post.
                        JSONObject jsonObject = jsonPosts.getJSONObject(index);
                        String title = jsonObject.get("title").toString();
                        String category = jsonObject.get("category").toString();
                        String description = jsonObject.get("description").toString();
                        int id = parseInt(jsonObject.get("id").toString());
                        int likes = parseInt(jsonObject.get("likes").toString());
                        String location = jsonObject.get("location").toString();
                        int price = parseInt(jsonObject.get("price").toString());
                        String time = jsonObject.get("time").toString();
                        String ownerId =jsonObject.get("owner_id").toString();
                        String picture = jsonObject.get("picture").toString();
                        Post post = new Post(id, ownerId, title, description, category, price, time, location, picture, likes);
                        posts.add(post);
                        // When all posts are added set the adapter
                        if (index == jsonPosts.length()-1) {
                            PostAdapter postAdapter = new PostAdapter(getContext(), posts, HomeFragment.this, "homeFragment");
                            recyclerView.setAdapter(postAdapter);
                        }
                    }
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
        queue.add(followingPostsRequest);
    }

    public void switchToPostDetail() {
        NavController navController = Navigation.findNavController(getView());
        navController.navigate(R.id.action_homeFragment_to_postDetailFragment);
    }

}