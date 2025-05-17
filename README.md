
# 🎬 Fabflix - Project 3

## Deployment Info
- **URL(Customers)**: https://18.119.142.99:8443/main.html
 - **Credentials**:
   - email: `a2@email.com`
   - Password: `a2`
    
- **URL(Employees)**: https://18.119.142.99:8443/fabflix/_dashboard/dashboard.html
 - **Credentials**:
   - email: `classta@email.edu`
   - Password: `classta`
     
- **Tomcat Manager**: https://18.119.142.99:8443/manager/html
 - **Credentials**:
   - Username: `admin`
   - Password: `mypassword`

## Team Members
- **Brian Seo** — XML Parsing, recaptcha Implementation,HTTPs, and Prepared Statements for SQL injection Prevention, AWS Deployment
- **Lucas Kim** — Frontend Features, UI/UX, Encryption, Dashboard and Stored Procedures

## Demo Video
https://youtu.be/DaNoGcySF1w

---

## Security Enhancements

### Task 1: reCAPTCHA
- Integrated Google reCAPTCHA on the login page.
- Backend verifies reCAPTCHA token with Google's verification API.
- Keys registered for both `localhost` and AWS IP.

### Task 2: HTTPS
- Configured Tomcat SSL on port `8443` using a self-signed certificate.
- All HTTP traffic redirected to HTTPS via `web.xml` security constraints.
- HTTPS enforced on all sensitive endpoints (login, payment, employee dashboard).

### Task 3: PreparedStatement
- All servlets use `PreparedStatement` to prevent SQL injection.
- No user inputs are concatenated directly into SQL strings.
- Example:
  ```java
  String query = "SELECT * FROM movies WHERE title LIKE ?";
  PreparedStatement ps = conn.prepareStatement(query);
  ps.setString(1, "%" + title + "%");
  ```

---

## Task 4: Password Encryption
- Implemented encrypted password storage using **Jasypt's StrongPasswordEncryptor**.
- `customers` and `employees` passwords are encrypted and checked via:
  ```java
  encryptor.checkPassword(inputPassword, encryptedPasswordFromDB);
  ```
- One-time encryption migration run via `UpdateSecurePassword.java`.

- (Taken off due to internal error)

---

## Task 5: Dashboard (Employee Portal)

### Employee Login
- reCAPTCHA + encrypted password verification.
- Redirects to secure employee dashboard upon login.

### View Metadata
- Displays all table names with their columns and data types.

### Add Star
- Inputs:
  - Name (required)
  - Birth Year (optional)
- Generates a unique star ID (e.g., `nm1234567`) using the current max ID.

### Add Movie via Stored Procedure
- Adds new movies and associates one **star** and one **genre**.
- Prevents duplicates (same title + year + director).
- Inserts into:
  - `movies`
  - `genres`
  - `stars`
  - `stars_in_movies`
  - `genres_in_movies`
- Stored procedure defined in `stored-procedure.sql`:
  - Handles ID generation and conditional insertion.

---

## Task 6: XML Parsing and Data Insertion

### Parsed Files
- `mains243.xml`:
  - Populates:
    - `movies`
    - `genres`
    - `genres_in_movies`
- `casts124.xml`:
  - Populates:
    - `stars_in_movies`
  - Cross-references:
    - `<fid>` from `mains243.xml`
    - `<a>` (actor name) to existing stars

### Parsing & Insertion
- Used **Java DOM Parser**.
- Inserted using `PreparedStatement`.
- Set encoding to `ISO-8859-1`.

### Error Handling
- Skipped malformed entries, logged warnings.
- Treated invalid or missing values as `NULL`.
- Deduplicated entries (movies, genres, stars_in_movies).

### Performance Optimizations
- Used batch inserts:
  ```java
  pstmt.addBatch();
  pstmt.executeBatch();
  ```


