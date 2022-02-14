package se.liu.robn725.tddd80_projekt;

import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles the searched user profile.
 * Has temporary data from MiscViewModel to get and display the searched user data.
 */
public class SearchedUserProfile extends Fragment {
    private MiscDataViewModel miscDataViewModel;
    private TextView username;
    private TextView fullname;
    private TextView email;
    private TextView phone;
    private ImageView profilePicture;
    private TextView followerCount;
    private TextView followingCount;
    private Button followButton;
    private Button unfollowButton;
    private RequestQueue queue;
    private UserViewModel userViewModel;
    private int followers;
    private boolean isInFollowers;
    public SearchedUserProfile() {
        // Required empty public constructor
    }

    public static SearchedUserProfile newInstance() {
        return new SearchedUserProfile();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_searched_user_profile, container, false);
        queue = Volley.newRequestQueue(getContext());
        username = view.findViewById(R.id.profile_username_text);
        fullname = view.findViewById(R.id.profile_fullname_text);
        email = view.findViewById(R.id.profile_email_text);
        phone = view.findViewById(R.id.profile_phone_text);
        profilePicture = view.findViewById(R.id.profile_change_image_button);
        followerCount = view.findViewById(R.id.profile_followers);
        followingCount = view.findViewById(R.id.profile_following);
        userViewModel = new ViewModelProvider(getActivity()).get(UserViewModel.class);
        miscDataViewModel = new ViewModelProvider(getActivity()).get(MiscDataViewModel.class);
        followButton = view.findViewById(R.id.follow_button);
        unfollowButton = view.findViewById(R.id.unfollow_button);
        setProfileData();
        isInFollowers(miscDataViewModel.getUsername());
        followButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    followUser(miscDataViewModel.getUsername());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        unfollowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    unfollowUser(miscDataViewModel.getUsername());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        return view;
    }

    /**
     * Sets all of the textViews with correct data from the ViewModel.
     */
    private void setProfileData() {
        String username = miscDataViewModel.getUsername();
        String firstName = miscDataViewModel.getFirstName();
        String lastName = miscDataViewModel.getLastName();
        String email = miscDataViewModel.getEmail();
        String phone = miscDataViewModel.getPhone();
        String followers = miscDataViewModel.getSearchedUserData().getValue().get("follower_count");
        String following = miscDataViewModel.getSearchedUserData().getValue().get("following_count");
        this.followers = Integer.parseInt(followers);
        String firstAndLastName = firstName + " " + lastName;
        this.username.setText(username);
        fullname.setText(firstAndLastName);
        this.email.setText(email);
        this.phone.setText(phone);
        this.followerCount.setText(followers);
        this.followingCount.setText(following);
        setProfilePicture();
    }

    /**
     * Sets the profile picture of the user.
     */
    public void setProfilePicture() {
        if (miscDataViewModel.getProfilePicture() != null) {
            // Set internet Uri to image bitmap
            Glide.with(getContext()).load(miscDataViewModel.getProfilePicture()).into(profilePicture);
        } else {
            profilePicture.setImageResource(R.drawable.ic_baseline_person_24);
        }
    }

    /**
     * Sends a request to follow a user.
     * @param username Username of the user to follow.
     * @throws JSONException Throws error if response is invalid.
     */
    public void followUserRequest(String username) throws JSONException {
        String followUrl = "https://market-app-swe.herokuapp.com/follow_user";
        HashMap<String, String> followRequestMap = new HashMap<>();
        followRequestMap.put("user", username);
        JSONObject followRequestData = new JSONObject(followRequestMap);

        JsonObjectRequest followRequest = new JsonObjectRequest(Request.Method.POST, followUrl, followRequestData, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i("Success", response.toString());
                try {
                    Toast text = Toast.makeText(getContext(), response.get("message").toString(), Toast.LENGTH_SHORT);
                    text.show();

                } catch (JSONException e) {
                    try {
                        Toast text = Toast.makeText(getContext(), response.get("message").toString(), Toast.LENGTH_SHORT);
                        text.show();
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                    }

                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }){
            // Send header with token
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                params.put("Authorization", "Bearer " + userViewModel.getToken().getValue());
                return params;
            }
        };
        queue.add(followRequest);
    }

    /**
     * Sends a request to unfollow a user.
     * @param username Username to unfollow.
     * @throws JSONException Throws error if response is invalid.
     */
    public void unfollowUserRequest(String username) throws JSONException {
        String likeUrl = "https://market-app-swe.herokuapp.com/unfollow_user/"+username;
        HashMap<String, String> deletePostToken = userViewModel.getTokenHeader();
        JSONObject deletePostParams = new JSONObject(deletePostToken);

        JsonObjectRequest unlikeRequest = new JsonObjectRequest(Request.Method.DELETE, likeUrl, deletePostParams, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i("Success", response.toString());
                try {
                    Toast text = Toast.makeText(getContext(), response.get("message").toString(), Toast.LENGTH_SHORT);
                    text.show();

                } catch (JSONException e) {
                    try {
                        Toast text = Toast.makeText(getContext(), response.get("message").toString(), Toast.LENGTH_SHORT);
                        text.show();
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                    }

                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error);
            }
        }){
            // Send header with token
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                params.put("Authorization", "Bearer " + userViewModel.getToken().getValue());
                return params;
            }
        };
        queue.add(unlikeRequest);

    }

    /**
     * Check if user is already following userToCheck
     * @param userToCheck Username to check if user is following.
     */
    public void isInFollowers(String userToCheck) {
        String username = userViewModel.getUsername();
        String url = "https://market-app-swe.herokuapp.com/" + username + "/follows/" + userToCheck;
        JsonObjectRequest getPostRequest = new JsonObjectRequest(Request.Method.GET , url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    isInFollowers = response.getBoolean("message");
                    if (isInFollowers) {
                        followButton.setVisibility(View.GONE);
                        unfollowButton.setVisibility(View.VISIBLE);
                    } else {
                        followButton.setVisibility(View.VISIBLE);
                        unfollowButton.setVisibility(View.GONE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(getPostRequest);

    }

    /**
     * Send request to unfollow.
     * Visibly show the unfollow count in realtime when unfollowing.
     * @param username Username to follow
     * @throws JSONException
     */
    public void unfollowUser(String username) throws JSONException {
        unfollowUserRequest(username);
        followButton.setVisibility(View.VISIBLE);
        unfollowButton.setVisibility(View.GONE);
        String followerNumber = Integer.toString(followers - 1);
        followerCount.setText(followerNumber);
    }
    
    /**
     * Send request to follow.
     * Visibly show the follow count in realtime when following.
     * @param username Username to follow
     * @throws JSONException
     */
    public void followUser(String username) throws JSONException {
        followUserRequest(username);
        followButton.setVisibility(View.GONE);
        unfollowButton.setVisibility(View.VISIBLE);
        String followerNumber = Integer.toString(followers + 1);
        followerCount.setText(followerNumber);
    }

}