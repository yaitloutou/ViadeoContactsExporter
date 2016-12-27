# remove 161219073753_ (reversed date + _) form the file names using PowerShell
Get-childItem *.json | % {rename-item $_.name ($_.name -replace '\d*_','')}