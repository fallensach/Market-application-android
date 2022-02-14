package se.liu.robn725.tddd80_projekt;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.Integer.parseInt;

/**
 * UserViewModel stores information about the user.
 * Token used for authentication header
 * UserData which contains basic information: username, email, first name, last name, phone, followers and following.
 * UserPosts which contains an ArrayList of the object type Post. All of the users posts.
 */
public class UserViewModel extends ViewModel {
    private MutableLiveData<String> tokenLiveData;
    private MutableLiveData<HashMap<String, String>> userLiveData;
    private MutableLiveData<ArrayList<Post>> userPosts;
    
    
    public LiveData<String> getToken() {
        if (tokenLiveData == null) {
            tokenLiveData = new MutableLiveData<>();
        }
        return tokenLiveData;
    }

    public HashMap<String, String> getTokenHeader() {
        HashMap<String, String> tokenHeader = new HashMap<>();
        String token = tokenLiveData.getValue();
        tokenHeader.put("Authorization", "Bearer " + token);
        return tokenHeader;
    }

    public void setPhotoUrl(String url) {
        HashMap<String, String> dataMap = getUserData().getValue();
        dataMap.put("profile_picture_url", url);
        userLiveData.setValue(dataMap);
    }

    public LiveData<HashMap<String, String>> getUserData() {
        if (userLiveData == null) {
            userLiveData = new MutableLiveData<>();
        }
        return userLiveData;
    }

    public MutableLiveData<ArrayList<Post>> getUserPosts() {
        if (userPosts == null) {
            userPosts = new MutableLiveData<>();
        }
        return userPosts;
    }

    /**
     * Takes a JSONArray with valid types and converts it into a ArrayList filled with Post objects
     * @param userPostsAsJSONArray JSONArray with types: id, owner_id, title, description, category, price, time, location, picture
     * @throws JSONException throw errors if response is invalid.
     */
    public void setUserPosts(JSONArray userPostsAsJSONArray) throws JSONException {
        ArrayList<Post> posts = new ArrayList<>();
        for (int i = 0; i < userPostsAsJSONArray.length(); i++) {
            JSONObject postObject = (JSONObject) userPostsAsJSONArray.get(i);
            int postId = parseInt(postObject.get("id").toString());
            String ownerId = postObject.get("owner_id").toString();
            String title = postObject.get("title").toString();
            String description = postObject.get("description").toString();
            String category = postObject.get("category").toString();
            int price = parseInt(postObject.get("price").toString());
            String time = postObject.get("time").toString();
            String location = postObject.get("location").toString();
            String pictureUrl = postObject.get("picture").toString();
            int likes = parseInt(postObject.get("likes").toString());
            
            Post post = new Post(postId, ownerId, title
                    , description, category, price, time
                    , location, pictureUrl, likes);
            posts.add(post);
        }
        userPosts.setValue(posts);
    }

    public void setToken(String token) {
        tokenLiveData.setValue(token);
    }

    /**
    * Sets user data from the JSONObject userObject into liveModel.
    * @param userObject JSONObject with types: username, first_name, last_name, email, phone,
    * follower_count, following_count, profile_picture_url.
    */
    public void setUserData(JSONObject userObject) throws JSONException {
        HashMap<String, String> userDataMap = new HashMap<>();
        userDataMap.put("username", userObject.get("username").toString());
        userDataMap.put("first_name", userObject.get("first_name").toString());
        userDataMap.put("last_name", userObject.get("last_name").toString());
        userDataMap.put("email", userObject.get("email").toString());
        userDataMap.put("phone", userObject.get("phone").toString());
        userDataMap.put("follower_count", userObject.get("follower_count").toString());
        userDataMap.put("following_count", userObject.get("following_count").toString());
        if (!userObject.isNull("profile_picture_url")) {
            userDataMap.put("profile_picture_url", userObject.get("profile_picture_url").toString());
        }

        userLiveData.setValue(userDataMap);
    }

    public String getUsername() {
        return userLiveData.getValue().get("username");
    }


    public Uri getProfilePicture() {
        if (userLiveData.getValue().get("profile_picture_url") != null) {
            return Uri.parse(userLiveData.getValue().get("profile_picture_url"));
        }
        return null;
    }

    public void destroyToken() {
        tokenLiveData.setValue(null);
    }
}
