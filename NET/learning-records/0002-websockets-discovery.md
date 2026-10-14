# 0002-websockets-discovery.md

## Context
User asked to learn about WebSockets. Investigated the codebase for WebSocket-related code.

## Discovery
- Found `Sec-WebSocket-Key:` in TCPWebWorker.parse() - it's detected but not processed
- No actual WebSocket handshake response (no 101 Switching Protocols)
- No frame parsing/handling
- The compiled class in dist/ also has the key string, confirming it's just a placeholder

## Insight
The server is designed as a simple HTTP server only. The WebSocket header detection suggests awareness of WebSocket upgrade requests, but full implementation would require:
1. Computing Sec-WebSocket-Accept (SHA-1 of key + magic GUID, then base64)
2. Sending 101 response with proper headers
3. Switching to WebSocket frame protocol (different from HTTP line-based reading)

This is a natural next learning topic given the detected placeholder. The lesson should clarify what's there vs what's needed.

## Zone of Proximal Development Implication
After learning HTTP basics (lessons 0001-0006), WebSockets are a logical next step. But implementing full WebSocket protocol is more complex. Start with conceptual understanding of the handshake - that's accessible and builds on HTTP knowledge.

## Actionable Takeaway
Created Lesson 0007 (WebSockets Introduction) and reference doc covering handshake mechanics. The lesson builds on HTTP knowledge (status codes, headers, request/response flow).
