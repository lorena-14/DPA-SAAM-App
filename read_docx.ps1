Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead('C:/Users/Melissa_/StudioProjects/DPA-SAAM/Documento Anteproyecto (version 7) .docx')
$entry = $zip.GetEntry('word/document.xml')
$stream = $entry.Open()
$reader = New-Object System.IO.StreamReader($stream)
$xml = $reader.ReadToEnd()
$reader.Close()
$stream.Close()
$zip.Dispose()
$xml -replace '<[^>]+>', ' ' -replace '\s+', ' '