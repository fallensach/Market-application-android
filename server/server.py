from datetime import datetime, timedelta
from flask import Flask, jsonify, request
from flask_sqlalchemy import SQLAlchemy
from flask_bcrypt import Bcrypt
import os
from flask_jwt_extended import JWTManager, jwt_required, create_access_token, \
    get_jwt_identity, set_access_cookies, get_jwt
from sqlalchemy import func

app = Flask(__name__)
if 'NAMESPACE' in os.environ and os.environ['NAMESPACE'] == 'heroku':
    db_uri = os.environ['DATABASE_URL']
    debug_flag = False

# when running locally: use sqlite
else:
    db_path = os.path.join(os.path.dirname(__file__), 'test.db')
    db_uri = 'sqlite:///{}'.format(db_path)
    debug_flag = True

ACCESS_EXPIRES = timedelta(weeks=1)
app.config["JWT_SECRET_KEY"] = "hej"
app.config["SQLALCHEMY_DATABASE_URI"] = db_uri
app.config["JWT_TOKEN_LOCATION"] = ["headers", "cookies", "json", "query_string"]
app.config["JWT_ACCESS_TOKEN_EXPIRES"] = ACCESS_EXPIRES

db = SQLAlchemy(app)
bcrypt = Bcrypt(app)
jwt = JWTManager(app)

class User(db.Model):
    username = db.Column(db.String(60), primary_key=True)
    password = db.Column(db.String(200), unique=False)
    first_name = db.Column(db.String(60), unique=False)
    last_name = db.Column(db.String(60), unique=False)
    email = db.Column(db.String(100), unique=True)
    phone = db.Column(db.String(20), unique=False)
    profile_picture_url = db.Column(db.String(500), unique=True, nullable=True)
    followers = db.relationship("Followers", backref="user_followers")
    following = db.relationship("Following", backref="user_following")
    posts = db.relationship("Post", backref="owner")
    reviews = db.relationship("Reviews", backref="user_review")
    location = db.Column(db.String(120))


class Reviews(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user = db.Column(db.String(60), db.ForeignKey("user.username"))
    reviewer = db.Column(db.String(60))
    rating = db.Column(db.Integer)
    comment = db.Column(db.String(255))

    def serialize(self):
        return {"user": self.user,
                "reviewer": self.reviewer,
                "rating": self.rating,
                "comment": self.comment
                }


class Followers(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user = db.Column(db.String(60), db.ForeignKey("user.username"), unique=False)
    follower_name = db.Column(db.String(60), unique=False)


class Following(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    user = db.Column(db.String(60), db.ForeignKey("user.username"), unique=False)
    followed_user = db.Column(db.String(60), unique=False)


class TokenBlacklist(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    jti = db.Column(db.String(36), nullable=False)
    token_in_db_date = db.Column(db.DateTime, nullable=False)


class Post(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    owner_id = db.Column(db.String(60), db.ForeignKey("user.username"))
    picture = db.Column(db.String(200), unique=False, nullable=True)
    category = db.Column(db.String(60), unique=False)
    title = db.Column(db.String(100), unique=False)
    description = db.Column(db.String(500), unique=False)
    price = db.Column(db.Integer, unique=False)
    shared_location = db.Column(db.String(60), nullable=True)
    shared_time = db.Column(db.DateTime(timezone=True), server_default=func.now())
    comments = db.relationship("Comment", backref="post_comments")
    likes = db.relationship("Likes", backref="post_likes")

    def serialize(self):
        return {"id": self.id,
		        "owner_id": self.owner_id,
                "picture": self.picture,
                "category": self.category,
                "title": self.title,
                "description": self.description,
                "price": self.price,
                "location": self.shared_location,
                "time": self.shared_time,
                "likes": len(self.likes)
        }
    def empty_serialize(self):
        return {"post:": []}

class Likes(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    post_id = db.Column(db.Integer, db.ForeignKey("post.id"))
    user = db.Column(db.String(60), nullable=True)


class Comment(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    post_id = db.Column(db.Integer, db.ForeignKey("post.id"))
    user = db.Column(db.String(60), unique=False, nullable=True)
    message = db.Column(db.String(255), unique=False, nullable=True)
    time = db.Column(db.DateTime(timezone=True), server_default=func.now())

    def serialize(self):
        return {"post_id": self.post_id,
                "user": self.user,
                "message": self.message,
                "time": self.time}


@app.route("/")
def index():
    return "<div style='text-align:center'>Server Online</div>"

# Register a new user
@app.route("/register", methods=["POST"])
def register():
    data = request.get_json()
    username = data["username"]
    password = data["password"]
    first_name = data["first_name"]
    last_name = data["last_name"]
    email = data["email"]
    phone = data["phone"]
    u = User.query.filter_by(username=username).first()
    if u is not None:
        return jsonify({"Message:":"Användarnamn upptaget"}), 400
    if "@" not in email:
        return jsonify(message="email not valid"), 400
    if len(password) < 5:
        return jsonify(message="Lösenordet måste bestå av minst 6 bokstäver"), 400
    if len(first_name) == 0 or len(last_name) == 0:
        return jsonify(message="Förnamn och efternam får inte vara tomt"), 400

    hash_password = bcrypt.generate_password_hash(password).decode("utf-8")
    u = User(username=username, password=hash_password, first_name=first_name, last_name=last_name, email=email, phone=phone)
    db.session.add(u)
    db.session.commit()
    return jsonify(message="Användare skapad"), 200

# Login the user
@app.route("/login", methods=["POST"])
def login():
    data = request.get_json()
    username = data["username"]
    password = data["password"]
    u = User.query.filter_by(username=username).first()
    if u is not None:
        if bcrypt.check_password_hash(u.password, password):
            token = create_access_token(identity=u.username)
            response = jsonify(access_token=token)
            set_access_cookies(response, token)
            return jsonify({"token": token}), 200

    return jsonify(message="username or password is wrong"), 401

# Get user information by id
@app.route("/get_user/<user_id>")
def get_user(user_id):
    user = User.query.filter_by(username=user_id).first()
    follower_count = len(user.followers)
    following_count = len(user.following)

    if user != None:
        return jsonify(username=user.username, first_name=user.first_name, last_name=user.last_name, email=user.email, phone=user.phone, follower_count=follower_count, following_count=following_count, profile_picture_url=user.profile_picture_url), 200
    return jsonify(message="user not found"), 404

# Revoke token
@jwt.token_in_blocklist_loader
def check_if_token_revoked(jwt_header, jwt_payload):
    jti = jwt_payload["jti"]
    token = db.session.query(TokenBlacklist.id).filter_by(jti=jti).scalar()
    return token is not None

# Follow a user
@app.route("/follow_user", methods=["POST"])
@jwt_required()
def follow_user():
    user_to_follow = request.get_json()["user"]
    current_identity = get_jwt_identity()
    user_follow = User.query.filter_by(username=user_to_follow).first()

    if (current_identity and user_follow) != None:
        user = User.query.filter_by(username=current_identity).first()
        following = user.following

        # Check if already following
        for follow in following:
            if follow.followed_user == user_to_follow:
                return jsonify(message="Följer redan"), 400

        if user_follow.username == current_identity:
            return jsonify(message="Följ inte dig själv"), 400

        user = User.query.filter_by(username=current_identity).first()
        following = Following(user=user.username, followed_user=user_to_follow)
        follower = Followers(user=user_to_follow, follower_name=user.username)
        db.session.add(following)
        db.session.add(follower)
        db.session.commit()
        return jsonify(message="Följde användare"), 200

    return jsonify(message= "Användare ej hittad"), 404


# Unfollow a user
@app.route("/unfollow_user/<user>", methods=["DELETE"])
@jwt_required()
def unfollow_user(user):
    username = User.query.filter_by(username=get_jwt_identity()).first()
    following = Following.query.filter_by(user=get_jwt_identity(), followed_user=user).delete()
    followers = Followers.query.filter_by(user=user, follower_name=get_jwt_identity()).delete()
    db.session.commit()
    return jsonify(message="Avföljde"), 200


# Check if a user is following another
@app.route("/<user>/follows/<user_2>")
def check_following(user, user_2):
    user = User.query.filter_by(username=user).first()
    following = user.following

    for follow in following:
        if follow.followed_user == user_2:
            return jsonify(message=True), 200

    return jsonify(message=False), 200


# Get the user's followers
@app.route("/followers/<user>", methods=["GET"])
def get_followers(user):
    follow_dict = {}
    followers = Followers.query.filter_by(user=user).all()
    username = User.query.filter_by(username=user).first()

    if username != None:
        follower_list = []

        if not followers:
            follow_dict[user] = []

        for follower in followers:
            follower_list.append(follower.follower_name)
            follow_dict[follower.user] = follower_list

        return jsonify(follow_dict), 200
    return jsonify(Message="User not found"), 404

# Get everyone that the user is following
@app.route("/following/<user>", methods=["GET"])
def get_following(user):
    following_dict = {}
    following = Following.query.filter_by(user=user).all()
    username = User.query.filter_by(username=user).first()

    if username != None:
        following_list = []

        if not following:
            following_dict[user] = []

        else:
            for followed in following:
                following_list.append(followed.followed_user)
                following_dict[followed.user] = following_list

        return jsonify(following_dict), 200
    return jsonify(Message="User not found"), 404


# Create a post
@app.route("/create_post", methods=["POST"])
@jwt_required()
def create_post():
    identity = get_jwt_identity()
    user = User.query.filter_by(username=identity).first()
    data = request.get_json()
    picture = data["picture"]
    category = data["category"]
    title = data["title"]
    description = data["description"]
    price = data["price"]
    location = data["location"]

    if user != None:
        if price == "" or price == None:
            return jsonify(message="Pris får inte vara tomt"), 400
        if category == None:
            return jsonify(message="Kategori måste väljas"), 400
        if title == None or title == "":
            return jsonify(message="Titeln kan inte vara tom"), 400
        if description == None or description == "":
            return jsonify(message="Fyll i en beskrivning"), 400
            
        post = Post(owner_id=user.username, picture=picture, category=category, title=title, description=description, price=price, shared_location=location)
        db.session.add(post)
        db.session.commit()

        return jsonify(message="Annons skapad"), 200
    return jsonify(message="user not found"), 404


# Get a post by id
@app.route("/get_post/<post_id>")
def get_post(post_id):
    post = Post.query.filter_by(id=post_id).first()

    if post != None:
        likes_list = []

        for like in post.likes:
            likes_list.append(like)

        likes = len(likes_list)

        return jsonify(id=post_id, owner_id=post.owner_id, picture=post.picture,
                       category=post.category, price=post.price,
                       location=post.shared_location, time=post.shared_time, likes=likes, title=post.title, description=post.description), 200

    return jsonify(message="cannot find post id"), 404


# Search for user
@app.route("/search_user/<username_input>")
def search_username(username_input):
    user = User.query.all()
    users_list = []
    for username_string in user:
        if username_input in username_string.username:
            users_list.append(username_string.username)
    return jsonify(users=users_list), 200


# Get all posts of a user
@app.route("/get_posts/<user>")
def get_posts(user):
    username = User.query.filter_by(username=user).first()
    posts = Post.query.filter_by(owner_id=user).all()

    if username != None:
        posts_list = []
        for post in posts:
            if posts == []:
                posts_list.append(post.empty_serialize())
            posts_list.append(post.serialize())

        return jsonify(posts_list), 200
    return jsonify(message="user does not exist"), 404


# Get all posts from the people that user is following
@app.route("/get_posts_from_following/<user>")
def get_posts_following(user):
    def get_following_user(user):
        following_dict = {}
        following = Following.query.filter_by(user=user).all()
        username = User.query.filter_by(username=user).first()

        if username != None:
            following_list = []

            if not following:
                following_dict[user] = []

            else:
                for followed in following:
                    following_list.append(followed.followed_user)
                    following_dict[followed.user] = following_list

            following_dict["following"] = following_dict.pop(user)
            return following_dict
        return jsonify(Message="User not found")

    following = get_following_user(user)

    def get_posts(following_dict):
        posts_list = []
        if following_dict["following"] != []:
            for user in following_dict["following"]:
                posts = Post.query.filter_by(owner_id=user).all()

                for post in posts:
                    if post == []:
                        posts_list.append(post.empty_serialize())
                    else:
                        posts_list.append(post.serialize())
                        
        return posts_list
    posts_list = get_posts(following)

    return jsonify(posts=posts_list), 200


# Add image to the post
@app.route("/upload_post_photo", methods=["POST"])
@jwt_required()
def upload_post_photo():
    post_id = request.get_json["post_id"]
    new_image_url = request.get_json["post_image"]
    post = Post.query.filter_by(id=post_id).first()

    if post != None:
        post.picture = new_image_url
        db.session.commit()
        return jsonify(message="Annonsbild upplagd"), 200

    return jsonify(message="Annons id kunde inte hittas"), 404


# Delete a post
@app.route("/delete_post/<post_id>", methods=["DELETE"])
@jwt_required()
def delete_post(post_id):
    user = get_jwt_identity()
    post = Post.query.filter_by(id=post_id).first()
    comments = Comment.query.filter_by(post_id=post_id)
    if post != None:
        if post.owner_id == user:
            comments.delete()
            Post.query.filter_by(id=post_id).delete()
            db.session.commit()
        else:
            return jsonify(message="unauthorized delete of post"), 401

        return jsonify(message="deleted post"), 200
    return jsonify(message="post not found"), 404


# Add a profile picture url
@app.route("/upload_image", methods=["POST"])
@jwt_required()
def upload_image():
    image = request.get_json()["photo_url"]
    username = get_jwt_identity()
    user = User.query.filter_by(username=username).first()
    if user != None:
        user.profile_picture_url = image
        db.session.commit()
        return jsonify(message="image uploaded"), 200
    return jsonify(message="user not found"), 404


# Post a comment on a post
@app.route("/comment", methods=["POST"])
@jwt_required()
def comment():
    current_user = get_jwt_identity()
    user = User.query.filter_by(username=current_user).first()
    message = request.get_json()["message"]
    post_id = request.get_json()["post_id"]
    post = Post.query.filter_by(id=post_id).first()

    if post != None and user is not None:
        if len(message) < 1:
            return jsonify(message="Meddelande kan inte vara tomt"), 400

        comment = Comment(post_id=post_id, user=user.username, message=message)
        db.session.add(comment)
        db.session.commit()
        return jsonify(message="commented"), 200

    return jsonify(message="post not found"), 404

# Get comments from a post
@app.route("/get_comments/<post_id>")
def get_comments(post_id):
    post = Post.query.filter_by(id=post_id).first()
    if post != None:
        comment_list = []
        comments = post.comments

        for comment in comments:
            comment_list.append(comment.serialize())

        return jsonify(comment_list), 200

    return jsonify(error="post not found"), 404


# Like a post
@app.route("/like", methods=["POST"])
@jwt_required()
def like():
    current_user = get_jwt_identity()
    user = User.query.filter_by(username=current_user).first()
    post_id = request.get_json()["post_id"]
    post = Post.query.filter_by(id=post_id).first()
    likes = Likes.query.filter_by(post_id=post_id, user=current_user).first()

    if post != None and user is not None:
        if likes != None:
            return jsonify(message="you already liked this post"), 400

        like = Likes(post_id=post_id, user=user.username)
        db.session.add(like)
        db.session.commit()
        return jsonify(message="liked"), 200

    return jsonify(message="post not found"), 404

# See if user liked a post
@app.route("/<user>/liked/<post_id>")
def liked(user, post_id):
    post = Post.query.filter_by(id=post_id).first()
    user = User.query.filter_by(username=user).first()
    for like in post.likes:
        if like.user == user.username:
            return jsonify(message=True), 200

    return jsonify(message=False), 200


# Get all the likes of a post
@app.route("/get_likes/<post_id>")
def get_likes(post_id):
    post = Post.query.filter_by(id=post_id).first()
    if post != None:
        likes_list = []

        for like in post.likes:
            likes_list.append(like)

        return jsonify(likes=len(likes_list)), 200
    return jsonify(error="post not found"), 404


# Unlikes a post
@app.route("/unlike/<post_id>", methods=["DELETE"])
@jwt_required()
def unlike(post_id):
    user = get_jwt_identity()
    post = Post.query.filter_by(id=post_id).first()

    if post != None:
        likes = post.likes

        for like in likes:
            if user == like.user:
                Likes.query.filter_by(user=user).delete()

        db.session.commit()
        return jsonify(message="unliked post"), 200

    return jsonify(message="post not found"), 404


# Logout user
@app.route("/logout", methods=["DELETE"])
@jwt_required()
def revoke_token():
    jti = get_jwt()["jti"]
    time_now = datetime.now()
    db.session.add(TokenBlacklist(jti=jti, token_in_db_date=time_now))
    db.session.commit()
    return jsonify(message="token revoked"), 200


def init_db():
    db.drop_all()
    db.create_all()


if __name__ == "__main__":
    app.debug = True
    app.run()
