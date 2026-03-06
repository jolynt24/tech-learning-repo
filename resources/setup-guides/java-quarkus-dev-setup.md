# Java / Quarkus Dev Setup — macOS

## Prerequisites: Homebrew

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

---

## 1. Java (JDK 25 LTS)

```bash
brew install --cask openjdk@17
```

Verify:
```bash
java -version
```

Set `JAVA_HOME` in `~/.zshrc`:
```bash
export JAVA_HOME=$(/opt/homebrew/opt/openjdk@17/bin)
export PATH=$JAVA_HOME:$PATH
```

---

## 2. Maven

```bash
brew install maven
```

Verify:
```bash
mvn -v
```

---

## 3. Quarkus CLI

```bash
brew install quarkusio/tap/quarkus
```

Verify:
```bash
quarkus --version
```

Create a new project:
```bash
quarkus create app org.example:my-app --extension=rest,hibernate-orm,jdbc-postgresql
cd my-app
quarkus dev
```

---

## 4. Docker Desktop

```bash
brew install --cask docker
```

Open Docker Desktop from Applications to complete setup, then verify:
```bash
docker --version
docker compose version
```

---

## 5. IntelliJ IDEA

```bash
brew install --cask intellij-idea
```

Recommended plugins:
- **Quarkus** (search in Plugins marketplace)
- **Lombok** (if using Lombok)
- **Database Navigator** or use built-in DataGrip features

Set Project SDK: `File > Project Structure > SDK > Add SDK > temurin-25`

---

## 6. GitHub CLI

```bash
brew install gh
```

Authenticate:
```bash
gh auth login
```

Follow prompts: GitHub.com > HTTPS > Login with browser.

Configure git identity:
```bash
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
git config --global init.defaultBranch main
```

---

## 7. Optional but Recommended

| Tool | Install | Purpose |
|---|---|---|
| jq | `brew install jq` | Parse JSON in terminal |
| httpie | `brew install httpie` | API testing from CLI |
| pgcli | `brew install pgcli` | Better PostgreSQL CLI |

---

## Verify Full Stack

```bash
java -version       # openjdk 25
mvn -v              # Apache Maven 3.x
quarkus --version   # 3.x
docker --version    # Docker 27.x
gh --version        # gh 2.x
```
