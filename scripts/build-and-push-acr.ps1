[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $Registry,

    [Parameter(Mandatory = $true)]
    [string] $Namespace,

    [string] $Tag = (Get-Date -Format "yyyyMMdd-HHmmss"),

    [string] $Platform = "linux/amd64",

    [switch] $PushLatest,

    [ValidateSet("backend", "crawler", "frontend")]
    [string[]] $Services = @("backend", "crawler", "frontend")
)

$ErrorActionPreference = "Stop"

$projectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $projectRoot

$registryName = $Registry.TrimEnd("/")
$namespaceName = $Namespace.Trim("/")
$imagePrefix = "$registryName/$namespaceName"

$images = @(
    @{
        Service = "backend"
        Name = "lottery-backend"
        Dockerfile = "lottery-backend/Dockerfile"
    },
    @{
        Service = "crawler"
        Name = "lottery-crawler"
        Dockerfile = "lottery-crawler/Dockerfile"
    },
    @{
        Service = "frontend"
        Name = "lottery-frontend"
        Dockerfile = "lottery-frontend/Dockerfile"
    }
)

$selectedServices = $Services | Select-Object -Unique
$selectedImages = $images | Where-Object { $selectedServices -contains $_.Service }

Write-Host "Building lottery images for $Platform with tag $Tag"
Write-Host "Selected services: $($selectedServices -join ', ')"

foreach ($image in $selectedImages) {
    $imageName = "$imagePrefix/$($image.Name)"
    $versionedTag = "${imageName}:${Tag}"

    Write-Host "Building $versionedTag"
    docker build --platform $Platform -f $image.Dockerfile -t $versionedTag .

    if ($PushLatest) {
        $latestTag = "${imageName}:latest"
        Write-Host "Tagging $latestTag"
        docker tag $versionedTag $latestTag
    }
}

foreach ($image in $selectedImages) {
    $imageName = "$imagePrefix/$($image.Name)"
    $versionedTag = "${imageName}:${Tag}"

    Write-Host "Pushing $versionedTag"
    docker push $versionedTag

    if ($PushLatest) {
        $latestTag = "${imageName}:latest"
        Write-Host "Pushing $latestTag"
        docker push $latestTag
    }
}

Write-Host ""
Write-Host "Add or update these values in .env.production:"
foreach ($image in $selectedImages) {
    $envName = "LOTTERY_$($image.Service.ToUpperInvariant())_IMAGE"
    Write-Host "$envName=$imagePrefix/$($image.Name):$Tag"
}
