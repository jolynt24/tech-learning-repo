Explore the quarkus-wellness-api project and give me a thorough walkthrough of the codebase. Please:

1. **Explain the overall architecture**:
   - What is this application and what does it do?
   - What tech stack is used and why those choices make sense together?
   - How is the project structured (packages, layers, responsibilities)?

2. **Walk through the API endpoints**:
   - List all available routes with their HTTP method, path, auth requirements, and purpose
   - Explain the request and response shapes for each endpoint
   - Show example curl commands I can use to test each one locally

3. **Explain the authentication flow**:
   - How does user registration work end to end?
   - How does login work and what tokens are issued?
   - How are access tokens and refresh tokens different, and how does refresh work?
   - How are protected routes secured using JWT and roles?

4. **Break down the key classes**:
   - What does each class in `auth/`, `config/`, `dto/`, `util/`, and `exceptions/` do?
   - Explain any Quarkus-specific annotations used (e.g. `@UserDefinition`, `@PanacheEntity`, `@ApplicationScoped`)
   - How does the error handling system work from exception to HTTP response?

5. **Explain the data model**:
   - What does the `User` entity look like and how is it persisted?
   - How are passwords stored and verified?
   - What role does the `roles` field play in authorization?

6. **Show me how to run it locally**:
   - What are the prerequisites?
   - How do I start the dependencies (database, Redis)?
   - How do I start the app in dev mode?
   - What should I expect to see once it's running?

7. **Point out anything worth noting**:
   - Any interesting design decisions or Quarkus-specific patterns used
   - Any areas that could be improved or extended
   - Any configuration I should be aware of before running

Please read the actual source files before explaining — don't guess. Be specific and reference file paths and line numbers where helpful.
