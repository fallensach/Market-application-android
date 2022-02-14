package se.liu.robn725.tddd80_projekt;

import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
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
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Integer.parseInt;

/**
 * Handles the details of a post.
 *
 */
public class PostDetailFragment extends Fragment {
    private Button likeButton;
    private Button unlikeButton;
    private Button commentButton;
    private TextView price;
    private ImageView image;
    private TextView likes;
    private TextView description;
    private TextView title;
    private UserViewModel userViewModel;
    private RequestQueue queue;
    private MiscDataViewModel miscDataViewModel;
    private TextView location;
    private boolean isInLikes;
    private int postLikes;
    private EditText commentText;
    private RecyclerView recyclerView;
    private CommentAdapter commentAdapter;
    private ArrayList<Comment> comments = new ArrayList<>();

    public static PostDetailFragment newInstance() {
        return new PostDetailFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_post_detail, container, false);
        likeButton = view.findViewById(R.id.like_button);
        unlikeButton = view.findViewById(R.id.unlike_button);
        commentButton = view.findViewById(R.id.comment_button);
        price = view.findViewById(R.id.detail_price);
        image = view.findViewById(R.id.detail_image);
        title = view.findViewById(R.id.detail_title);
        description = view.findViewById(R.id.detail_description);
        likes = view.findViewById(R.id.detail_likes);
        queue = Volley.newRequestQueue(getContext());
        unlikeButton.setVisibility(View.GONE);
        location = view.findViewById(R.id.detail_location);
        recyclerView = view.findViewById(R.id.comment_list_view);
        userViewModel = new ViewModelProvider(getActivity()).get(UserViewModel.class);
        miscDataViewModel = new ViewModelProvider(getActivity()).get(MiscDataViewModel.class);
        commentText = view.findViewById(R.id.detail_comment_text);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        int postId = miscDataViewModel.getPostId().getValue();

        try {
            // Get the post data.
            getPost(postId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Hide like and unlike button if own post
        if (miscDataViewModel.isProfile()) {
            likeButton.setVisibility(View.GONE);
            unlikeButton.setVisibility(View.GONE);
        } else {
            isInLikes(postId);
        }

        likeButton.setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
               try {
                   // Add a like
                   likePost(postId);
                   likeButton.setVisibility(View.GONE);
                   unlikeButton.setVisibility(View.VISIBLE);
                   postLikes += 1;
                   likes.setText(Integer.toString(postLikes));
               } catch (JSONException e) {
                   e.printStackTrace();
               }
           }
        });

        unlikeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Unlike
                    unlikePost(postId);
                    likeButton.setVisibility(View.VISIBLE);
                    unlikeButton.setVisibility(View.GONE);
                    postLikes -= 1;
                    likes.setText(Integer.toString(postLikes));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

        commentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commentRequest(postId, commentText.getText().toString());
                // Reset EditText
                commentText.setText("");
            }
        });

        getCommentRequest(postId);
        return view;

    }

    /**
     * Sends a request to post a comment to the server.
     * @param postId Id of the post to comment on
     * @param comment The comment text
     */
    public void commentRequest(int postId, String comment) {
        String commentUrl = "https://market-app-swe.herokuapp.com/comment";
        HashMap<String, String> postCommentRequestMap = new HashMap<>();
        postCommentRequestMap.put("message", comment);
        postCommentRequestMap.put("post_id", Integer.toString(postId));
        JSONObject postCommentRequest = new JSONObject(postCommentRequestMap);
        JsonObjectRequest commentRequest = new JsonObjectRequest(Request.Method.POST, commentUrl, postCommentRequest, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                getCommentRequest(postId);
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
        queue.add(commentRequest);
    }

    /**
     * Request for comments on a post given an id.
     * @param postId Id of the post
     */
    public void getCommentRequest(int postId) {
        String getCommentUrl = "https://market-app-swe.herokuapp.com/get_comments/" + postId;
        JsonArrayRequest commentRequest = new JsonArrayRequest(Request.Method.GET, getCommentUrl, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                // Reset comments before adding new ones.
                comments.clear();
                // Add the comments from the response.
                for (int index = 0; index < response.length(); index++) {
                    try {
                        JSONObject commentData = (JSONObject) response.get(index);
                        String commentName = commentData.get("user").toString();
                        String commentText = commentData.get("message").toString();
                        String commentDate = commentData.get("time").toString();
                        Comment comment = new Comment(commentName, commentText, commentDate);
                        comments.add(comment);
                        // Check if all comments are added.
                        if (index == response.length() - 1) {
                            commentAdapter = new CommentAdapter(getContext(), comments);
                            recyclerView.setAdapter(commentAdapter);
                            commentAdapter.notifyDataSetChanged();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error);
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
        queue.add(commentRequest);
    }

    /**
     * Sends a request to like a post to the server.
     * @param postId Id of the post to like.
     * @throws JSONException Throws error if response is invalid.
     */
    public void likePost(int postId) throws JSONException {
        String likeUrl = "https://market-app-swe.herokuapp.com/like";
        HashMap<String, String> postIdRequestMap = new HashMap<>();
        postIdRequestMap.put("post_id", Integer.toString(postId));
        JSONObject postIdRequest = new JSONObject(postIdRequestMap);

        JsonObjectRequest likeRequest = new JsonObjectRequest(Request.Method.POST, likeUrl, postIdRequest, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
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
        queue.add(likeRequest);
    }

    /**
     * Sends a request to unlike a post to the server.
     * @param postId Id of post to unlike.
     * @throws JSONException Throws error if response is invalid.
     */
    public void unlikePost(int postId) throws JSONException {
        String likeUrl = "https://market-app-swe.herokuapp.com/unlike/"+postId;
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
     * Sends a request to get all of the post's details.
     * @param postId Id of the post to get details from.
     * @throws JSONException Throws error if response is invalid.
     */
    public void getPost(int postId) throws JSONException {
        String url = "https://market-app-swe.herokuapp.com/get_post/" + postId;
        JsonObjectRequest getPostRequest = new JsonObjectRequest(Request.Method.GET , url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.i("Success", response.toString());
                try {
                    String title = response.get("title").toString();
                    String category = response.get("category").toString();
                    String description = response.get("description").toString();
                    int id = parseInt(response.get("id").toString());
                    int likes = parseInt(response.get("likes").toString());
                    String location = response.get("location").toString();
                    int price = parseInt(response.get("price").toString());
                    String time = response.get("time").toString();
                    String ownerId =response.get("owner_id").toString();
                    Uri picture = Uri.parse(response.get("picture").toString());

                    // Updates all of the textViews with correct data.
                    updatePostDetails(title, description, price, location, picture, likes);

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
        queue.add(getPostRequest);
    }

    /**
     * Updates the textViews with new data.
     * @param title Title of the post.
     * @param description Description of the post.
     * @param price Price of the post.
     * @param location Location of the post.
     * @param picture Image uri of the post.
     * @param likes Likes of the post.
     */
    public void updatePostDetails(String title,
                                  String description, int price, String location, Uri picture, int likes) {
        this.title.setText(title);
        this.postLikes = likes;
        this.location.setText(location);
        this.description.setText(description);
        this.price.setText(Integer.toString(price) + " Kr");
        Glide.with(getContext()).load(picture).into(image);
        this.likes.setText(Integer.toString(likes));
    }

    /**
     * Sends a request to see if the user already liked a post.
     * @param postId Id of the post to check for likes.
     */
    public void isInLikes(int postId) {
        String username = userViewModel.getUsername();
        String url = "https://market-app-swe.herokuapp.com/" + username + "/liked/" + postId;
        JsonObjectRequest getPostRequest = new JsonObjectRequest(Request.Method.GET , url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    isInLikes = response.getBoolean("message");
                    // If in likes then hide the like button and display the unlike button, vice versa.
                    if (isInLikes) {
                        likeButton.setVisibility(View.GONE);
                        unlikeButton.setVisibility(View.VISIBLE);
                    } else {
                        likeButton.setVisibility(View.VISIBLE);
                        unlikeButton.setVisibility(View.GONE);
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

}