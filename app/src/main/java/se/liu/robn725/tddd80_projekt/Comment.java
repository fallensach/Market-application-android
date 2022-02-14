package se.liu.robn725.tddd80_projekt;

/**
 * Structure for a object type comment.
 * Has commentName, comment and commentDate.
 */
public class Comment {
    private String commentName;
    private String comment;
    private String commentDate;

    public Comment(String commentName, String comment, String commentDate) {
        this.commentName = commentName;
        this.comment = comment;
        this.commentDate = commentDate;
    }

    public String getCommentName() {
        return commentName;
    }


    public String getComment() {
        return comment;
    }


    public String getCommentDate() {
        return commentDate;
    }

}
