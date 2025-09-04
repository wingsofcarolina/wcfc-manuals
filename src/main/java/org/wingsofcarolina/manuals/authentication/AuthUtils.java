package org.wingsofcarolina.manuals.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import java.util.Date;
import java.util.HashMap;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.manuals.ManualsConfiguration;
import org.wingsofcarolina.manuals.model.User;

public class AuthUtils {

  private static final Logger LOG = LoggerFactory.getLogger(AuthUtils.class);

  private static AuthUtils instance = null;

  // For SecretKey generation
  private byte[] encoded = {
    -8,
    -36,
    93,
    58,
    -106,
    123,
    -77,
    -120,
    -119,
    80,
    -67,
    -58,
    -103,
    40,
    8,
    -81,
    4,
    -91,
    58,
    83,
    -67,
    69,
    22,
    71,
    74,
    -109,
    -125,
    67,
    -72,
    -39,
    -11,
    -63,
    42,
    1,
    5,
    3,
    -32,
    -97,
    -21,
    -67,
    -127,
    47,
    -46,
    -108,
    99,
    -69,
    36,
    120,
    -67,
    92,
    113,
    51,
    96,
    34,
    67,
    -12,
    -44,
    -31,
    -117,
    -37,
    92,
    -97,
    -100,
    67,
  };

  private SecretKey key;
  private JwtParser parser;
  private ObjectMapper mapper;

  public AuthUtils() {
    key = Keys.hmacShaKeyFor(encoded);
    parser = Jwts.parser().verifyWith(key).build();
    mapper = new ObjectMapper();
  }

  public static AuthUtils instance() {
    if (instance == null) {
      instance = new AuthUtils();
    }
    return instance;
  }

  // The following header hack is due to (a) Chrome demanding SameSite be set
  // and (b) NewCookie having no way to freaking do that. WTF people? So instead
  // of using the .cookie() call on the Response the cookie gets turned into a
  // String, and the SameSite setting gets added to the end, and the .header()
  // function is used instead. What a hack.
  public static String sameSite(NewCookie cookie) {
    return cookie.toString() + ";SameSite=none";
  }

  public SecretKey getKey() {
    return key;
  }

  public JwtParser getParser() {
    return parser;
  }

  public Jws<Claims> decodeCookie(Cookie cookie) {
    Jws<Claims> claims = null;
    String compactJws = cookie.getValue();
    if (compactJws != null && !compactJws.isEmpty()) {
      claims = parser.parseSignedClaims(compactJws);
    }
    return claims;
  }

  public String generateToken(User user) {
    // Now generate the Java Web Token
    // https://github.com/jwtk/jjwt
    // https://stormpath.com/blog/jwt-java-create-verify
    // https://scotch.io/tutorials/the-anatomy-of-a-json-web-token
    String compactJws = Jwts
      .builder()
      .issuedAt(new Date())
      .subject(user.getName())
      .claim("email", user.getEmail())
      .claim("admin", user.getAdmin())
      .claim("version", 1)
      .signWith(key)
      .compact();

    return compactJws;
  }

  public NewCookie generateCookie(User user) {
    int maxAge = 86400 * 30; // Seconds per day, times days to live
    NewCookie cookie = new NewCookie(
      "wcfc.manuals.token",
      generateToken(user),
      "/",
      null,
      "WCFC Manuals ID",
      maxAge,
      true,
      true
    );
    return cookie;
  }

  public NewCookie removeCookie() {
    return new NewCookie(
      "wcfc.manuals.token",
      null,
      "/",
      null,
      "WCFC Manuals ID",
      0,
      true,
      true
    );
  }

  public User getUserFromCookie(Cookie cookie) {
    User user = null;

    // If auth is not enabled, hard-code it to Dwight
    if (ManualsConfiguration.instance().getAuth() == false) {
      user = new User("Dwight Frye", "dfrye@planez.co");
    }

    if (cookie != null) {
      Jws<Claims> claims = decodeCookie(cookie);
      Claims body = claims.getBody();

      user = new User((String) body.getSubject(), (String) body.get("email"));

      HashMap<?, ?> mymap = mapper.convertValue(body, HashMap.class);

      if (mymap.containsKey("admin") || !ManualsConfiguration.instance().getAuth()) {
        user.setAdmin((Boolean) body.get("admin"));
      } else {
        user.setAdmin(false);
      }
    }
    return user;
  }
}
