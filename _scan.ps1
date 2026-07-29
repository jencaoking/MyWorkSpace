$p = 'J:/PROJECT/Android-Project/MyWorkSpace/MyWork/app/src/main/java/com/jencao/mywork/data/remote/model/ApiModels.kt'
$bytes = [System.IO.File]::ReadAllBytes($p)
$text = [System.Text.Encoding]::UTF8.GetString($bytes)
$lines = $text -split "`n"
for ($i = 0; $i -lt $lines.Length; $i++) {
    $l = $lines[$i].TrimEnd("`r")
    if ($l -match '[^\x09\x0A\x0D\x20-\x7E]') {
        Write-Host ("LINE " + ($i + 1) + ": " + $l)
    }
}
Write-Host "DONE scanning"
