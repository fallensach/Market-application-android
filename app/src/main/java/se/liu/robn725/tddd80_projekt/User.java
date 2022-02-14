package se.liu.robn725.tddd80_projekt;

/**
 * Creates a user object.
 */
public class User {
    private String username;
    private String imageUrl;

    public User(String username, String imageUrl) {
        this.username = username;
        this.imageUrl = imageUrl;
    }

    public String getUsername() {
        return username;
    }
    
    public String getImageUrl() {
        if (imageUrl.equals("null")) {
            return "https://i.stack.imgur.com/dr5qp.jpg";
        }
        return imageUrl;
    }
}
