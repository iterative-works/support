#!/usr/bin/env bash
# Script to publish iw-support modules to e-BS Nexus

set -e

# Check if credentials are set
if [ -z "$EBS_NEXUS_USERNAME" ] || [ -z "$EBS_NEXUS_PASSWORD" ]; then
    echo "Error: EBS_NEXUS_USERNAME and EBS_NEXUS_PASSWORD must be set"
    echo "Usage: export EBS_NEXUS_USERNAME=your-username"
    echo "       export EBS_NEXUS_PASSWORD=your-password"
    exit 1
fi

# Export Mill-specific variables
export MILL_SONATYPE_USERNAME=$EBS_NEXUS_USERNAME
export MILL_SONATYPE_PASSWORD=$EBS_NEXUS_PASSWORD

# Parse command line arguments
MODULE=""
LOCAL_ONLY=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --local)
            LOCAL_ONLY=true
            shift
            ;;
        --module)
            MODULE="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--local] [--module MODULE_NAME]"
            echo "  --local: Publish to local Maven repository only"
            echo "  --module: Publish specific module (e.g., core.jvm)"
            exit 1
            ;;
    esac
done

# Build command
if [ "$LOCAL_ONLY" = true ]; then
    if [ -n "$MODULE" ]; then
        CMD="./mill $MODULE.publishLocal"
    else
        CMD="./mill __.publishLocal"
    fi
    echo "Publishing to local Maven repository..."
else
    if [ -n "$MODULE" ]; then
        CMD="./mill $MODULE.publish"
    else
        CMD="./mill __.publish"
    fi
    echo "Publishing to e-BS Nexus..."
    echo "Repository URLs are configured in IWPublishModule"
    echo "GPG signing is disabled by default"
fi

# Execute the command
echo "Running: $CMD"
eval $CMD

echo "Publishing completed successfully!"