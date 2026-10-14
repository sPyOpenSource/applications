# Mission: Learning HTTP Servers

## Purpose
The goal of this learning journey is to understand how HTTP servers work fundamentally, using the concrete implementation in this JX operating system codebase as a reference.

## Learning Objectives
By the end of this mission, I want to be able to:
- Explain the HTTP request/response cycle at a low level
- Understand how TCP sockets are used to build an HTTP server
- Parse HTTP requests (methods, headers, URIs)
- Construct valid HTTP responses with proper headers
- Compare different concurrency models (thread-per-connection vs domain-per-connection)
- Understand how static file serving works

## Real-World Context
Understanding HTTP servers from first principles builds foundational knowledge for web development, networking, and systems programming. The simple implementation here strips away abstractions to reveal the core mechanics.

## Success Criteria
- Can trace through the request flow in WebServer and TCPWebWorker
- Can explain each part of an HTTP request and response
- Can modify a simple HTTP server behavior
