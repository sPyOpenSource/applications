# Resources

## Primary Resources (High Quality)

1. **RFC 9110 - HTTP Semantics** (https://www.rfc-editor.org/rfc/rfc9110.html)
   - The authoritative specification for HTTP semantics
   - Use: precise definitions of methods, headers, status codes

2. **MDN Web Docs - HTTP Guide** (https://developer.mozilla.org/en-US/docs/Web/HTTP)
   - Type: Documentation/Guide
   - Trust: Very High
   - Use: Conceptual explanations, practical examples
   - Notes: Excellent, accessible, keeps current

3. **Beej's Guide to Network Programming** (https://beej.us/guide/bgnet/)
   - Type: Tutorial/Guide
   - Trust: Very High
   - Use: TCP socket fundamentals
   - Notes: Clear explanation of sockets in C, concepts transfer well

4. **HTTP/1.1: The Definitive Guide** (O'Reilly) - reference conceptually
   - Classic deep dive

## Codebase Reference
- src/test/net/WebServer.java - Server setup and connection acceptance
- src/test/net/TCPWebWorker.java - HTTP protocol handling
- src/test/net/TCPWorker.java - Simple TCP echo

## Supplementary

1. **Let's Build a Web Server** (https://ruslanspivak.com/lsbaws-part1/)
   - Type: Tutorial
   - Trust: High
   - Use: Step-by-step server construction, complements codebase
