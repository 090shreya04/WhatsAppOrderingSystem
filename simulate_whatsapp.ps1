param (
    [Parameter(Mandatory=$true, Position=0)]
    [string]$Message,
    [string]$FromPhone = "919876543210",
    [string]$BusinessPhone = "911234567890"
)

$Payload = @{
    entry = @(
        @{
            changes = @(
                @{
                    value = @{
                        metadata = @{
                            display_phone_number = $BusinessPhone
                        }
                        messages = @(
                            @{
                                from = $FromPhone
                                id = "wamid.$(New-Guid)"
                                type = "text"
                                text = @{
                                    body = $Message
                                }
                            }
                        )
                    }
                }
            )
        }
    )
} | ConvertTo-Json -Depth 10

Invoke-RestMethod -Uri "http://localhost:8081/api/v1/webhook/whatsapp" `
                  -Method Post `
                  -ContentType "application/json" `
                  -Body $Payload | Out-Null

Write-Host "✅ Simulated WhatsApp message sent: `"$Message`" (from $FromPhone)" -ForegroundColor Green
