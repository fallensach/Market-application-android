package se.liu.robn725.tddd80_projekt;


/**
 * Structure for a object type Post.
 * Has id (post id), ownerId, title, description, category, price, time, location, picture and likes.
 */

public class Post {
    private int id;
    private String ownerId;
    private String title;
    private String description;
    private String category;
    private int price;
    private String time;
    private String location;
    private String picture;
    private int likes;


    public Post(int id, String ownerId, String title, String description, String category, int price, String time, String location, String picture, int likes) {
        this.id = id;
        this.ownerId = ownerId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = price;
        this.time = time;
        this.location = location;
        this.picture = picture;
        this.likes = likes;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getLocation() {
        return location;
    }


    public String getPicture() {
        return picture;
    }

    public int getLikes() {
        return likes;
    }

}
