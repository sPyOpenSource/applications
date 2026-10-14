# 0001-initial-exploration.md

## Context
Started teaching journey about HTTP servers in this JX OS codebase. Explored the test/net source files to understand the implementation.

## Discovery
- Found a minimal HTTP server implementation: WebServer.java (accept loop, threading/domain modes) + TCPWebWorker.java (parsing, responses)
- Uses java.net.Socket/ServerSocket for TCP transport
- Supports both thread-per-connection and domain-per-connection concurrency models
- Has basic request parsing (GET/POST, extracts filename, handles default index)
- Has simple response construction with Content-Length, Content-Type, basic status codes
- Has optional filesystem serving via JX FS interface
- Also saw simpler TCPWorker for comparison

## Insight
The implementation is intentionally minimal - great for learning fundamentals. It exposes the core pieces (sockets, parsing, response building) clearly without framework abstractions. The dual concurrency model (threads vs domains) is interesting - domains are JX's isolation mechanism.

## Zone of Proximal Development Implication
Starting from fundamentals is appropriate. The user needs to understand:
1. Sockets/TCP basics (foundation)
2. HTTP request structure (what's being parsed)
3. HTTP response structure (what's being sent)
4. Concurrency models (how requests are handled)
5. Filesystem serving (extension)

All of these are accessible from the codebase.

## Actionable Takeaway
Created a structured learning path with 6 foundational lessons and reference materials. Each lesson ties directly to the actual code files, keeping learning grounded in concrete implementation.
