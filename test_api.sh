#!/bin/bash
echo "Testing Ollama API..."
curl -X POST http://localhost:11434/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen3:4b",
    "messages": [{"role": "user", "content": "Say hello"}],
    "stream": false
  }' | python3 -m json.tool
