# Local Setup Guide

## Environment Variables

This project uses environment variables to manage sensitive credentials securely.

### Setup Steps

1. **Copy the template file:**
   ```bash
   cp .env.example .env.local
   ```

2. **Edit `.env.local` with your actual credentials:**
   ```bash
   # Database
   MYSQL_PASSWORD=your-actual-mysql-password

   # Groq API
   GROQ_API_KEY=your-actual-groq-api-key

   # Twilio (if using WhatsApp features)
   TWILIO_ACCOUNT_SID=your-sid
   TWILIO_AUTH_TOKEN=your-token
   ```

3. **Load environment variables before running the app:**

   **Option A: Manual export (Linux/Mac):**
   ```bash
   export $(cat .env.local | xargs)
   mvn spring-boot:run
   ```

   **Option B: Manual export (Windows/PowerShell):**
   ```powershell
   Get-Content .env.local | ForEach-Object {
       $key, $value = $_ -split '='
       [System.Environment]::SetEnvironmentVariable($key, $value)
   }
   mvn spring-boot:run
   ```

   **Option C: Using IDE (IntelliJ IDEA):**
   - Go to Run → Edit Configurations
   - Select your Spring Boot configuration
   - In "Environment variables", add:
     ```
     MYSQL_PASSWORD=your-password;GROQ_API_KEY=your-key
     ```

### Important Notes

- ✋ **DO NOT commit `.env.local`** — it's in `.gitignore` for a reason
- ✅ `.env.example` is committed and safe to share
- 🔒 Never hardcode secrets in `application.properties`
- 📝 Update `.env.example` when you add new environment variables

### Production Deployment

For production, set environment variables directly on your server/container:
- Docker: Use `docker run -e MYSQL_PASSWORD=xxx ...`
- Kubernetes: Use secrets and environment variable mappings
- Cloud platforms: Use their secrets management services
