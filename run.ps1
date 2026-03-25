# Load environment variables from .env.local and run the Spring Boot application

# Function to load .env.local file
function Load-EnvFile {
    param(
        [string]$EnvFilePath = ".env.local"
    )

    if (!(Test-Path $EnvFilePath)) {
        Write-Error "File not found: $EnvFilePath"
        return
    }

    Write-Host "Loading environment variables from $EnvFilePath..." -ForegroundColor Green

    Get-Content $EnvFilePath | ForEach-Object {
        $line = $_.Trim()

        # Skip empty lines and comments
        if ($line -and -not $line.StartsWith("#")) {
            $key, $value = $line -split '=', 2

            if ($key -and $value) {
                [System.Environment]::SetEnvironmentVariable($key.Trim(), $value.Trim())
                Write-Host "  ✓ $($key.Trim())" -ForegroundColor Cyan
            }
        }
    }

    Write-Host "Environment variables loaded successfully!" -ForegroundColor Green
}

# Load the environment variables
Load-EnvFile

# Run Maven Spring Boot
Write-Host "`nStarting Spring Boot application..." -ForegroundColor Yellow
mvn spring-boot:run -f grocery-billing/pom.xml
