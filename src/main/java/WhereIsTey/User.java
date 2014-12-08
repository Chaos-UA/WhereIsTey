package WhereIsTey;


public class User {
    private final String name;

    public User(String name) {
        this.name = name;
        if (name == null || name.isEmpty()) {
            throw new RuntimeException("User name is empty");
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (!name.toLowerCase().equals(user.name.toLowerCase())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }
}
