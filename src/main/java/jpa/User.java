package jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "member")
public class User {

    public static final GuestUser GUEST_USER = new GuestUser();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String userId;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    private String name;
    private String email;
    @Embedded
    private TimeLog timeLog;

    protected User() {
    }

    public User(final String userId, final String password, final String name, final String email) {
        this(null, userId, password, name, email, new TimeLog());
    }

    public User(
            final Long id,
            final String userId,
            final String password,
            final String name,
            final String email,
            final TimeLog timeLog
    ) {
        this.id = id;
        this.userId = userId;
        this.password = password;
        this.name = name;
        this.email = email;
        this.timeLog = timeLog;
    }

    public void update(final User loginUser, final User target) {
        if (!matchUserId(loginUser.userId)) {
            throw new IllegalArgumentException();
        }

        if (!matchPassword(target.password)) {
            throw new IllegalArgumentException();
        }

        this.name = target.name;
        this.email = target.email;
    }

    private boolean matchUserId(final String userId) {
        return this.userId.equals(userId);
    }

    public boolean matchPassword(final String targetPassword) {
        return this.password.equals(targetPassword);
    }

    public boolean equalsNameAndEmail(final User target) {
        if (Objects.isNull(target)) {
            return false;
        }

        return name.equals(target.name) && email.equals(target.email);
    }

    public boolean isGuestUser() {
        return false;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setTimeLog(final TimeLog timeLog) {
        this.timeLog = timeLog;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public TimeLog getTimeLog() {
        return timeLog;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", password='" + password + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", timeLog=" + timeLog +
                '}';
    }

    private static class GuestUser extends User {
        @Override
        public boolean isGuestUser() {
            return true;
        }
    }
}
