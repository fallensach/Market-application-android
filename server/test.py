import json
import os
import tempfile
import pytest

from server import app, init_db


@pytest.fixture
def client():
    db_fd, name = tempfile.mkstemp()
    app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///' + str(name)
    app.config['TESTING'] = True
    with app.test_client() as client:
        with app.app_context():
            init_db()
        yield client
    os.close(db_fd)
    os.unlink(name)

def test_login(client):
    client.post("/register", json=dict(username="test", password="123456", first_name="hej", last_name="hej", email="@", phone="12345"))
    rv = client.post("/login", json=dict(username="test", password="123456"))
    rv_wrong = client.post("/login", json=dict(username="test", password="12345"))
    assert rv.status_code == 200
    assert rv_wrong.status_code == 401

def test_register(client):
    # successful register:
    rv1 = client.post("/register", json=dict(username="test", password="123456", first_name="Billy", last_name="Bolly", email="1@kden.com", phone="123456"))
    assert 200 == rv1.status_code
    # too short password:
    rv2 = client.post("/register", json=dict(username="west", password="1234", first_name="Milly", last_name="Molly", email="2@kden.com", phone="123456"))
    assert 400 == rv2.status_code
    # no @ in email
    rv3 = client.post("/register", json=dict(username="best", password="123456", first_name="Silly", last_name="Solly", email="fksd", phone="123456"))
    assert 400 == rv3.status_code
    # no first name
    rv4 = client.post("/register", json=dict(username="west", password="123456", first_name="", last_name="Colly", email="4@kden.com", phone="123456"))
    assert 400 == rv4.status_code
    # no last name
    rv5 = client.post("/register", json=dict(username="west", password="123456", first_name="Willy", last_name="", email="5@kden.com", phone="123456"))
    assert 400 == rv5.status_code


def test_get_user(client):
    client.post("/register", json=dict(username="test", password="123456", first_name="hej", last_name="hej", email="@", phone="12345"))
    rv = client.get("/get_user/test")
    assert rv.status_code == 200

def test_follow_user(client):
    client.post("/register", json=dict(username="test", password="123456", first_name="hej", last_name="hej", email="@", phone="12345"))
    client.post("/register", json=dict(username="test2", password="123456", first_name="hej", last_name="hej", email="@2",
                                       phone="12345"))

    login = client.post("/login", json=dict(username="test", password="123456"))
    token = json.loads(login.data.decode("utf-8"))["token"]
    header = {"Authorization": "Bearer " + token}
    # Follow user
    rv = client.post("/follow_user", json={"user": "test2"}, headers=header)
    # Follow yourself
    rv2 = client.post("/follow_user", json={"user": "test"}, headers=header)
    # Follow same user again
    rv3 = client.post("/follow_user", json={"user": "test2"}, headers=header)
    print(rv.data)
    assert rv.status_code == 200
    assert rv2.status_code == 400
    assert rv3.status_code == 400

