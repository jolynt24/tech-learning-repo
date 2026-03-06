Explore the quarkus-wellness-api project and give me a thorough walkthrough of the codebase. Please:

1. **Explain the overall architecture**:
   - What is this application and what does it do? (covers auth and daily wellness entry tracking with meals)
   - What tech stack is used and why those choices make sense together?
   - How is the project structured (packages, layers, responsibilities)? Include `auth/`, `entries/`, `config/`, `dto/`, `enums/`, `util/`, and `exceptions/`

2. **Walk through the API endpoints**:
   - List all available routes with their HTTP method, path, auth requirements, and purpose — include both auth (`/api/auth/**`) and entries (`/api/entries/**`) endpoints
   - Explain the request and response shapes for each endpoint
   - Show example curl commands I can use to test each one locally

3. **Explain the authentication flow**:
   - How does user registration work end to end?
   - How does login work and what tokens are issued?
   - How are access tokens and refresh tokens different, and how does refresh work?
   - How are protected routes secured using JWT and roles?

4. **Break down the key classes**:
   - What does each class in `auth/`, `entries/`, `config/`, `dto/`, `enums/`, `util/`, and `exceptions/` do?
   - Cover `DailyEntry`, `EntryResource`, `EntryService`, `Meal`, and `MealType` specifically
   - Explain any Quarkus-specific annotations used (e.g. `@UserDefinition`, `@PanacheEntity`, `@ApplicationScoped`)
   - How does the error handling system work from exception to HTTP response?

5. **Explain the data model**:
   - What does the `User` entity look like and how is it persisted?
   - How are passwords stored and verified?
   - What role does the `roles` field play in authorization?
   - What do `DailyEntry` and `Meal` look like — their fields, relationships, and how they map to the database?
   - How does `MealType` (enum) factor into a meal record?

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
