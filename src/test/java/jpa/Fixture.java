package jpa;

public class Fixture {

    public static User JAVAJIGI() {
        return new User(
                "javajigi",
                "password",
                "name",
                "javajigi@slipp.net"

        );
    }

    public static User 땡칠() {
        return new User(
                "0chil",
                "password",
                "땡칠",
                "0@chll.it"

        );
    }
}
