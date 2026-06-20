# Troubleshooting & Deployment Notes
# banking-auth-api — Problems Encountered & Fixes

---

## Issue 1 — Spring Initializr rejects Spring Boot 2.7.x

**Symptom**
```
HTTP 400 Bad Request from start.spring.io:
"Invalid Spring Boot version '2.7.18', Spring Boot compatibility range is >=3.5.0"
```

**Root Cause**
Spring Initializr dropped support for Spring Boot 2.7.x (EOL). The web UI and API both
reject version numbers below 3.5.0 as of 2025.

**Fix**
Do not use Spring Initializr. Create the project structure manually:
- Write `pom.xml` by hand with `spring-boot-starter-parent` 2.7.18
  (still available on Maven Central — only the *generator* rejects it)
- Create the directory tree manually

**Note**
Spring Boot 2.7.x requires Java 8 or 11. Spring Boot 3.x requires Java 17+.
If only JDK 8 is available, stay on 2.7.x.

---

## Issue 2 — Maven fails: "JAVA_HOME environment variable is not defined correctly"

**Symptom**
```
The JAVA_HOME environment variable is not defined correctly,
this environment variable is needed to run this program.
```
Happens even when `$env:JAVA_HOME` is correctly set in PowerShell.

**Root Cause**
`C:\Fussion_Essence\Tool\apache-maven-3.9.11\bin\mvn.cmd` contained a hardcoded line
that overwrites any environment JAVA_HOME before Maven checks it:
```batch
set "JAVA_HOME=C:\Env\Java\jdk-17.0.11"
```
That path does not exist on this machine, so Maven immediately errors out.

**Fix**
Edit `C:\Fussion_Essence\Tool\apache-maven-3.9.11\bin\mvn.cmd` and comment out
the hardcoded override:
```batch
@REM set "JAVA_HOME=C:\Env\Java\jdk-17.0.11"
```
Maven will then fall back to the shell environment's `JAVA_HOME`
(`C:\Fussion_Essence\Tool\jdk1.8.0_202`).

**Verify**
```powershell
mvn --version
# Expected output includes: Java version: 1.8.0_202
```

---

## Issue 3 — Compiler error: source/target level 11 but only JDK 8 installed

**Symptom**
```
[ERROR] Failed to execute goal ... compiler:3.10.1:compile
Source option 11 is no longer supported. Use 7 or later.
```

**Root Cause**
`pom.xml` had `<java.version>11</java.version>` but the only JDK on this machine
is 1.8.0_202 (Java 8).

**Fix**
In `banking-auth-api/pom.xml`, change:
```xml
<java.version>11</java.version>
```
to:
```xml
<java.version>8</java.version>
```
Spring Boot 2.7.18 is fully compatible with Java 8.

---

## Issue 4 — Build artifacts (target/) accidentally committed to git

**Symptom**
`git log --stat` shows `.class` files, surefire XML reports, and compiled
`.properties` files inside `target/` in the initial commit. Repository size
bloats unnecessarily.

**Root Cause**
No `.gitignore` existed at the time of the first `git add .`, so Maven's entire
`target/` output directory was staged and committed.

**Fix — Step 1: Create .gitignore**
```gitignore
# Maven build output
target/
*.jar
*.war

# IDE files
.idea/
*.iml
.classpath
.project
.settings/

# Runtime data (H2 file DB, logs)
data/
logs/
*.log
```

**Fix — Step 2: Remove target/ from git tracking (files stay on disk)**
```bash
git rm -r --cached banking-auth-api/target/
```

**Fix — Step 3: Commit the cleanup**
```bash
git add banking-auth-api/.gitignore
git commit -m "chore: add .gitignore and remove target/ build artifacts from tracking"
```

**Future Prevention**
Always create `.gitignore` before the first `git add`.

---

## Issue 5 — Git checkout: 20+ "Deletion of directory failed. Try again? (y/n)" prompts

**Symptom**
When running `git checkout main` (switching away from a branch with many directories),
Git repeatedly asks:
```
Deletion of directory 'banking-auth-api/src/main/java/com/suraj/banking/auth/config' failed.
Should I try again? (y/n)
```
This repeats for every subdirectory — can be 20+ prompts.

**Root Cause**
Windows holds open file handles on directories when VS Code Explorer or another
process has them open. Git cannot delete the directory shell during checkout.

**Fix**
Answer `n` to each prompt. Git skips the directory deletion but still completes
the checkout — files are updated correctly even if empty directory shells remain.

Alternatively, close VS Code's Explorer file tree before switching branches,
or use:
```bash
git checkout -f main
```
Note: `-f` (force) discards any uncommitted changes — use only when the working
tree is clean.

**Note**
This is Windows-only behaviour. Does not occur on Linux or macOS.

---

## Issue 6 — /actuator/health returns 404 Not Found

**Symptom**
```
GET http://localhost:8080/actuator/health
→ Whitelabel Error Page — 404 Not Found
```

**Root Cause**
`spring-boot-starter-actuator` was never added to `pom.xml`.
The `/actuator/**` endpoints simply do not exist.

**Fix — pom.xml**
Add inside `<dependencies>`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Fix — application.properties**
```properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

**Fix — SecurityConfig.java**
Ensure the actuator paths are in the `permitAll()` list:
```java
.antMatchers("/actuator/health", "/actuator/info").permitAll()
```

---

## Issue 7 — /h2-console returns 401 Unauthorized

**Symptom**
```
GET http://localhost:8080/h2-console
→ HTTP ERROR 401
```

**Root Cause A**
`SecurityConfig` did not include `/h2-console/**` in the `permitAll()` list.
Spring Security blocked every request with a 401 before the page could load.

**Root Cause B**
The H2 web console renders its UI inside HTML `<frame>` elements.
Spring Security sets the `X-Frame-Options: DENY` response header by default,
which causes modern browsers to refuse to display the frame even after
authentication passes.

**Fix — SecurityConfig.java**

1. Permit the path:
```java
.antMatchers("/h2-console/**").permitAll()
```

2. Disable the frame-options header (dev only):
```java
.headers().frameOptions().disable()
.and()
```

**Why this is safe in dev**
The H2 console is enabled only in the dev profile via:
```properties
# application.properties (dev only)
spring.h2.console.enabled=true
```
In `application-prod.properties` this line is absent, so the console is not
reachable in production. The `frameOptions().disable()` therefore has no
production impact.

---

## Issue 8 — git push rejected: "Updates were rejected (fetch first)"

**Symptom**
```
! [rejected]  main -> main (fetch first)
error: failed to push some refs to 'https://github.com/...'
hint: Updates were rejected because the remote contains work that you do not have locally.
```

**Root Cause**
The remote branch had a commit (GitHub-created merge commit, README edit, etc.)
that was not in the local history. Local and remote diverged.

**Fix**
```bash
git pull origin main --rebase
git push origin main
```
`--rebase` replays local commits on top of the remote tip instead of creating
a merge commit, keeping the history linear.

---

## Quick Reference — New Machine Setup Checklist

Before running `mvn spring-boot:run` for the first time on a new machine:

- [ ] `java -version` → shows 1.8.x or 11.x
- [ ] `mvn --version` → shows the correct Java version (not a missing-path error)
- [ ] Check `mvn.cmd` for a hardcoded `JAVA_HOME` override line (see Issue 2)
- [ ] `.gitignore` exists in project root before any `git add`
- [ ] `spring-boot-starter-actuator` in `pom.xml` if health endpoint is needed
- [ ] `spring.h2.console.enabled=true` in `application.properties` for local H2 console access
- [ ] `/h2-console/**` permitted in `SecurityConfig` and `frameOptions().disable()` set
- [ ] `management.endpoints.web.exposure.include=health,info` in `application.properties`

---

## Environment (at time of writing)

| Tool | Version / Path |
|---|---|
| JDK | 1.8.0_202 — `C:\Fussion_Essence\Tool\jdk1.8.0_202` |
| Maven | 3.9.11 — `C:\Fussion_Essence\Tool\apache-maven-3.9.11` |
| Spring Boot | 2.7.18 |
| OS | Windows 10 |
| Git remote | https://github.com/suraj-suryn/Resume.git |
