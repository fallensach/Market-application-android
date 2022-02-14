package se.liu.robn725.tddd80_projekt;

import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;
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
import java.util.HashMap;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SearchUserFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SearchUserFragment extends Fragment implements SearchAdapter.SearchOnClickListener {
    private EditText searchText;
    private ImageButton searchButton;
    private ArrayList<User> users = new ArrayList<>();
    private SearchAdapter searchAdapter;
    private RecyclerView searchedUsersView;
    private TextView noSearches;
    private MiscDataViewModel miscDataViewModel;
    private UserViewModel userViewModel;

    public SearchUserFragment() {
        // Required empty public constructor
    }

    public static SearchUserFragment newInstance() {
        return new SearchUserFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search_user, container, false);
        searchText = view.findViewById(R.id.search_box);
        searchButton = view.findViewById(R.id.search_post_or_user);
        searchedUsersView = view.findViewById(R.id.users_list);
        searchedUsersView.setLayoutManager(new LinearLayoutManager(getContext()));
        noSearches = view.findViewById(R.id.no_searches);
        noSearches.setVisibility(View.GONE);
        searchText.requestFocus();
        searchText.isFocusableInTouchMode();
        searchText.setFocusable(true);
        miscDataViewModel = new ViewModelProvider(getActivity()).get(MiscDataViewModel.class);
        userViewModel = new ViewModelProvider(getActivity()).get(UserViewModel.class);
        
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchInput = searchText.getText().toString();
                searchForUser(searchInput);
            }
        });

        return view;
    }

    /**
     * Sends a search request for a username string
     * Returns a JSONObject containing all names that were matched.
     * @param input A string that will match to several usernames
     */
    public void searchForUser(String input) {
        String requestUrl = "https://market-app-swe.herokuapp.com/search_user/" + input;
        RequestQueue queue = Volley.newRequestQueue(getContext());
        JsonObjectRequest searchRequest = new JsonObjectRequest(Request.Method.GET, requestUrl,null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                // Clear the users array
                users.clear();
                String username = userViewModel.getUsername();
                try {
                    JSONArray usernameArray = response.getJSONArray("users");
                    // If there are no search results display 0 match found
                    if (usernameArray.length() == 0) {
                        noSearches.setVisibility(View.VISIBLE);
                        searchAdapter = new SearchAdapter(getContext(), SearchUserFragment.this, users);
                        searchedUsersView.setAdapter(searchAdapter);
                    } else {
                        for (int index = 0; index < usernameArray.length(); index++) {
                            if (!usernameArray.getString(index).equals(username)) {
                                addUser(usernameArray.getString(index));
                            }
                            noSearches.setVisibility(View.GONE);
                        }
                    }

                } catch (JSONException e) {
                    System.out.println("ERROR");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(searchRequest);
    }

    /**
     * Adds a user from database to local ArrayList.
     * @param username Username of user to add.
     */
    public void addUser(String username) {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        String dataUrl = "https://market-app-swe.herokuapp.com/get_user/" + username;
        JsonObjectRequest userDataRequest = new JsonObjectRequest(Request.Method.GET, dataUrl,null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    // Create new User object
                    User user = new User(response.get("username").toString(), response.get("profile_picture_url").toString());
                    users.add(user);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                searchAdapter = new SearchAdapter(getContext(), SearchUserFragment.this, users);
                searchedUsersView.setAdapter(searchAdapter);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String response = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                JSONObject data = new JSONObject();
                try {
                    data = new JSONObject(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String message = data.optString("message");
                Toast text = Toast.makeText(getContext(), message, Toast.LENGTH_SHORT);
                text.show();
            }
        });
        queue.add(userDataRequest);
    }
    

    @Override
    public void onClickedUser(String username) {
        getSearchedUserData(username);
    }

    /**
     * Get all of a searched user's data into the miscDataViewModel.
     * @param username Username of user to add into miscDataViewModel
     */
    public void getSearchedUserData(String username) {
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        miscDataViewModel.getSearchedUserData();
        String dataUrl = "https://market-app-swe.herokuapp.com/get_user/" + username;
        JsonObjectRequest userDataRequest = new JsonObjectRequest(Request.Method.GET, dataUrl,null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    // Convert JSONObject into ArrayList of User objects
                    miscDataViewModel.setUserData(response);
                    // Send post request to get posts of user
                    postsRequest(username);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String response = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                JSONObject data = new JSONObject();
                try {
                    data = new JSONObject(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String message = data.optString("message");
                Toast text = Toast.makeText(getContext(), message, Toast.LENGTH_SHORT);
                text.show();
            }
        });
        requestQueue.add(userDataRequest);
    }

    /**
     * Sends a post request to the server to get all of the posts
     * from the active user.
     * @param username Username of the user to get posts from.
     */
    public void postsRequest(String username) {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        String url = "https://market-app-swe.herokuapp.com/get_posts/" + username;

        JsonArrayRequest getPostsRequest= new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                miscDataViewModel.getUserPosts();
                try {
                    // Convert JSONArray response to ArrayList<Post>
                    miscDataViewModel.setUserPosts(response);
                    switchToProfile();

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

    public void switchToProfile() {
        NavController navController = Navigation.findNavController(getView());
        navController.navigate(R.id.action_searchUserFragment_to_searchedUserProfile);
    }
}