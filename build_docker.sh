#!/bin/bash
NAMESPACE="${1:-codebase_b500_app}"
docker build -t "$NAMESPACE" .