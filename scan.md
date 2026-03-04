Scan my codespace for exposed credentials and data leaks. Please:

1. **Search for exposed secrets** including:
   - API keys (AWS, GitHub, Stripe, OpenAI, etc.)
   - Private keys (.pem, .key files, SSH keys)
   - Database credentials and connection strings
   - OAuth tokens and bearer tokens
   - Slack/Discord/Telegram webhooks
   - Environment variables with sensitive values

2. **Check common locations**:
   - .env and .env.* files
   - config files (config.json, settings.py, etc.)
   - Source code comments containing secrets
   - Git history (if .git exists)
   - Docker files and .dockerignore files
   - .npmrc, .netrc, and other credential files
   - Package.json, requirements.txt, and dependency files
   - Logs and temporary files

3. **Look for patterns of leaked data**:
   - Hardcoded passwords or credentials in code
   - Base64-encoded secrets
   - AWS access keys (AKIA prefix)
   - Private GitHub tokens (ghp_ prefix)
   - Stripe keys (sk_live_, pk_live_ prefixes)
   - MongoDB connection strings
   - JWT tokens

4. **Generate a report** showing:
   - All files with potential leaks (with line numbers)
   - Severity level for each finding (high/medium/low)
   - Type of credential/data exposed
   - Recommended remediation steps

5. **Provide remediation guidance** for:
   - How to safely remove exposed credentials
   - How to rotate compromised keys
   - Best practices for secret management (.env, vault, etc.)
   - How to prevent future leaks

Please scan the entire project directory recursively and be thorough. Ignore generated files such as the target, node_modules.