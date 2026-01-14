package ro.cabinet.backend.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthUsersProperties {
  private List<UserConfig> users = new ArrayList<>();

  public List<UserConfig> getUsers() {
    return users;
  }

  public void setUsers(List<UserConfig> users) {
    this.users = users;
  }

  public static class UserConfig {
    private String username;
    private String password;

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}
